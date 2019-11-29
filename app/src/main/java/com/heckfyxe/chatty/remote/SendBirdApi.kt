package com.heckfyxe.chatty.remote

import com.heckfyxe.chatty.room.RoomMessage
import com.heckfyxe.chatty.room.RoomUser
import com.heckfyxe.chatty.util.sendbird.toRoomMessage
import com.heckfyxe.chatty.util.sendbird.toRoomUser
import com.sendbird.android.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


private val CHANNELS_CLEAN_DELAY = TimeUnit.MINUTES.toMillis(2)

class UserIsNotConnectedException : Exception("User isn't connected!")

class SendBirdApi {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val channelsHashMap = ConcurrentHashMap<String, GroupChannel>()
    private val channelsLastUsageTime = ConcurrentHashMap<String, Long>()

    val isUserConnected: Boolean
        get() = SendBird.getCurrentUser() != null

    init {
        startInMemoryChannelsCaching()
    }

    private fun startInMemoryChannelsCaching() {
        scope.launch {
            for (tick in ticker(CHANNELS_CLEAN_DELAY)) {
                channelsLastUsageTime.forEach { (id: String, time: Long) ->
                    if (System.currentTimeMillis() - time >= CHANNELS_CLEAN_DELAY) {
                        channelsLastUsageTime.remove(id)
                        channelsHashMap.remove(id)
                    }
                }
            }
        }
    }

    private fun checkConnection() {
        if (!isUserConnected) throw UserIsNotConnectedException()
    }

    suspend fun connect(userId: String): RoomUser {
        val result = Channel<RoomUser>()
        val error = Channel<Exception>()
        SendBird.connect(userId) { user, e ->
            scope.launch {
                if (e != null) {
                    error.send(e)
                    return@launch
                }

                result.send(user.toRoomUser())
            }
        }
        return getResult(result, error)
    }

    suspend fun getChannels(): ReceiveChannel<List<GroupChannel>> {
        checkConnection()
        val currentUser = SendBird.getCurrentUser()
        connect(currentUser.userId)
        val result = Channel<List<GroupChannel>>()
        GroupChannel.createMyGroupChannelListQuery().next { channels, e ->
            scope.launch {
                if (e != null) {
                    result.close(e)
                    return@launch
                }
                if (channels.isEmpty()) {
                    result.close()
                    return@launch
                }
                result.send(channels)
            }
        }
        return result
    }

    private suspend fun getChannel(channelUrl: String): GroupChannel {
        if (channelsHashMap.containsKey(channelUrl)) {
            channelsLastUsageTime[channelUrl] = System.currentTimeMillis()
            return channelsHashMap[channelUrl]!!
        }

        checkConnection()
        val result = Channel<GroupChannel>()
        val error = Channel<Exception>()
        GroupChannel.getChannel(channelUrl) { channel, e ->
            scope.launch {
                if (e != null) {
                    error.send(e)
                    return@launch
                }

                channelsHashMap[channel.url] = channel
                channelsLastUsageTime[channel.url] = System.currentTimeMillis()
                result.send(channel)
            }
        }
        return getResult(result, error)
    }

    suspend fun getLastMessage(channelUrl: String): BaseMessage? =
        getChannel(channelUrl).lastMessage

    suspend fun createChannel(userId: String, interlocutorId: String): GroupChannel {
        connect(userId)
        val result = Channel<GroupChannel>(1)
        val error = Channel<Exception>(1)
        GroupChannel.createDistinctChannelIfNotExist(
            GroupChannelParams().addUserId(interlocutorId)
        ) { channel, _, e ->
            scope.launch {
                if (e != null) {
                    error.send(e)
                    return@launch
                }
                result.send(channel)
            }
        }
        return getResult(result, error)
    }

    suspend fun getPreviousMessagesByTime(
        channelId: String,
        time: Long,
        count: Int
    ): List<RoomMessage> {
        checkConnection()
        val result = Channel<List<RoomMessage>>()
        val errorChan = Channel<Exception>(1)
        getChannel(channelId).getPreviousMessagesByTimestamp(
            time,
            false, count,
            true, BaseChannel.MessageTypeFilter.ALL, ""
        ) { loadedMessages, error ->
            scope.launch {
                if (error != null) {
                    errorChan.send(error)
                    return@launch
                }

                val messages = loadedMessages.map {
                    it.toRoomMessage()
                }
                result.send(messages)
            }
        }
        return getResult(result, errorChan)
    }

    suspend fun getNextMessagesByTime(
        channelId: String,
        time: Long,
        count: Int
    ): List<RoomMessage> {
        checkConnection()
        val result = Channel<List<RoomMessage>>()
        val errorChan = Channel<Exception>(1)
        getChannel(channelId).getNextMessagesByTimestamp(
            time,
            false, count,
            true, BaseChannel.MessageTypeFilter.ALL, ""
        ) { loadedMessages, error ->
            scope.launch {
                if (error != null) {
                    errorChan.send(error)
                    return@launch
                }

                val messages = loadedMessages.map {
                    it.toRoomMessage()
                }
                result.send(messages)
            }
        }
        return getResult(result, errorChan)
    }

    suspend fun sendMessage(channelId: String, text: String): ReceiveChannel<RoomMessage> {
        checkConnection()
        val channel = getChannel(channelId)
        val result = Channel<RoomMessage>(1)
        val tempMessage = channel.sendUserMessage(text) { message, e ->
            updateMemoryCachedChannel(channel)
            scope.launch {
                if (e != null) {
                    result.close(e)
                    return@launch
                }

                result.send(message.toRoomMessage(true))
                result.close()
            }
        }
        updateMemoryCachedChannel(channel)
        result.send(tempMessage.toRoomMessage(out = true, sent = false))
        return result
    }

    suspend fun startTyping(channelId: String) = getChannel(channelId).startTyping()

    suspend fun endTyping(channelId: String) = getChannel(channelId).endTyping()

    private fun updateMemoryCachedChannel(channel: GroupChannel) {
        if (channelsHashMap.containsKey(channel.url)) {
            channelsLastUsageTime[channel.url] = System.currentTimeMillis()
            channelsHashMap[channel.url] = channel
        }
    }

    private suspend fun <T: Any> getResult(result: Channel<T>, error: Channel<Exception>): T =
        select {
            result.onReceive {
                result.close()
                error.close()
                it
            }
            error.onReceive {
                result.close()
                error.close()
                throw it
            }
        }


    suspend fun disconnect() {
        val channel = Channel<Boolean>(1)
        SendBird.disconnect {
            scope.launch {
                channel.send(true)
            }
        }
        channel.receive()
        channel.close()
    }
}