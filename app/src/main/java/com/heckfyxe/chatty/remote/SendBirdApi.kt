package com.heckfyxe.chatty.remote

import com.heckfyxe.chatty.room.Message
import com.heckfyxe.chatty.room.User
import com.heckfyxe.chatty.util.sendbird.toMessage
import com.heckfyxe.chatty.util.sendbird.toRoomUser
import com.sendbird.android.BaseChannel
import com.sendbird.android.GroupChannel
import com.sendbird.android.GroupChannelParams
import com.sendbird.android.SendBird
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

class SendBirdApi {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val channelsHashMap = ConcurrentHashMap<String, GroupChannel>()
    private val channelsLastUsageTime = ConcurrentHashMap<String, Long>()

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

    suspend fun connect(userId: String): User {
        val result = Channel<User>()
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

    suspend fun loadChannel(channelId: String) {
        getChannel(channelId)
    }

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

    suspend fun getPreviousMessagesByTime(channelId: String, time: Long, count: Int): List<Message> {
        val result = Channel<List<Message>>()
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
                    it.toMessage()
                }
                result.send(messages)
            }
        }
        return getResult(result, errorChan)
    }

    suspend fun getNextMessagesByTime(channelId: String, time: Long, count: Int): List<Message> {
        val result = Channel<List<Message>>()
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
                    it.toMessage()
                }
                result.send(messages)
            }
        }
        return getResult(result, errorChan)
    }

    suspend fun sendMessage(channelId: String, text: String): ReceiveChannel<Message> {
        val channel = getChannel(channelId)
        val result = Channel<Message>(1)
        val tempMessage = channel.sendUserMessage(text) { message, e ->
            updateMemoryCachedChannel(channel)
            scope.launch {
                if (e != null) {
                    result.close(e)
                    return@launch
                }

                result.send(message.toMessage(true))
                result.close()
            }
        }
        updateMemoryCachedChannel(channel)
        result.send(tempMessage.toMessage(out = true, sent = false).apply {
            channel.lastMessage.createdAt + 1
        })
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