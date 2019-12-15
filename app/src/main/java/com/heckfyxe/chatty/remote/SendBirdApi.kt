package com.heckfyxe.chatty.remote

import com.heckfyxe.chatty.room.RoomMessage
import com.heckfyxe.chatty.util.sendbird.toRoomMessage
import com.sendbird.android.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume


private val CHANNELS_CLEAN_DELAY = TimeUnit.MINUTES.toMillis(2)

class SendBirdApi(private val userId: String) {
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private val channelsHashMap = ConcurrentHashMap<String, GroupChannel>()
    private val channelsLastUsageTime = ConcurrentHashMap<String, Long>()

    private val currentUser: User?
        get() = SendBird.getCurrentUser()
    private val isUserConnected: Boolean
        get() = currentUser != null

    private val mutex = Mutex()

    init {
        startInMemoryChannelsCaching()
    }

    @UseExperimental(ObsoleteCoroutinesApi::class)
    private fun startInMemoryChannelsCaching() = scope.launch {
        for (tick in ticker(CHANNELS_CLEAN_DELAY)) {
            channelsLastUsageTime.forEach { (id: String, time: Long) ->
                if (System.currentTimeMillis() - time >= CHANNELS_CLEAN_DELAY) {
                    channelsLastUsageTime.remove(id)
                    channelsHashMap.remove(id)
                }
            }
        }
    }

    private suspend fun checkConnection() = mutex.withLock {
        if (!isUserConnected) connect()
    }

    suspend fun connect(): User = suspendCancellableCoroutine {
        SendBird.connect(userId) { user, e ->
            if (e != null) {
                it.cancel(e)
            } else {
                it.resume(user)
            }
        }
    }

    suspend fun getChannels(): ReceiveChannel<List<GroupChannel>> {
        checkConnection()
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
        return suspendCancellableCoroutine {
            GroupChannel.getChannel(channelUrl) { channel, e ->
                if (e != null) {
                    it.cancel(e)
                } else {
                    channelsHashMap[channel.url] = channel
                    channelsLastUsageTime[channel.url] = System.currentTimeMillis()
                    it.resume(channel)
                }
            }
        }
    }

    suspend fun getLastMessage(channelUrl: String): BaseMessage? =
        getChannel(channelUrl).lastMessage

    suspend fun createChannel(interlocutorId: String): GroupChannel {
        checkConnection()
        return suspendCancellableCoroutine {
            GroupChannel.createDistinctChannelIfNotExist(
                GroupChannelParams().addUserId(interlocutorId)
            ) { channel, _, e ->
                if (e != null) {
                    it.cancel(e)
                    return@createDistinctChannelIfNotExist
                }
                it.resume(channel)
            }
        }
    }

    suspend fun getPreviousMessagesByTime(
        channelId: String,
        time: Long,
        count: Int
    ): List<RoomMessage> {
        checkConnection()
        val channel = getChannel(channelId)
        return suspendCancellableCoroutine {
            channel.getPreviousMessagesByTimestamp(
                time,
                false, count,
                true, BaseChannel.MessageTypeFilter.ALL, ""
            ) { loadedMessages, error ->
                if (error != null) {
                    it.cancel(error)
                    return@getPreviousMessagesByTimestamp
                }

                val messages = loadedMessages.map {
                    it.toRoomMessage()
                }
                it.resume(messages)
            }
        }
    }

    suspend fun getNextMessagesByTime(
        channelId: String,
        time: Long,
        count: Int
    ): List<RoomMessage> {
        checkConnection()
        val channel = getChannel(channelId)
        return suspendCancellableCoroutine {
            channel.getNextMessagesByTimestamp(
                time,
                false, count,
                true, BaseChannel.MessageTypeFilter.ALL, ""
            ) { loadedMessages, error ->
                if (error != null) {
                    it.cancel(error)
                    return@getNextMessagesByTimestamp
                }

                val messages = loadedMessages.map {
                    it.toRoomMessage()
                }
                it.resume(messages)
            }
        }
    }

    suspend fun sendMessage(channelId: String, text: String): ReceiveChannel<RoomMessage> {
        checkConnection()
        val channel = getChannel(channelId)
        val result = Channel<RoomMessage>(1)
        val tempMessage = channel.sendUserMessage(text.trim()) { message, e ->
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

    suspend fun updateNickname(nickname: String) {
        checkConnection()
        suspendCancellableCoroutine<Unit> {
            SendBird.updateCurrentUserInfo(nickname, currentUser!!.profileUrl) { e ->
                if (e != null) {
                    it.cancel(e)
                    return@updateCurrentUserInfo
                }
                it.resume(Unit)
            }
        }
    }

    suspend fun updateNicknameWithAvatarImage(nickname: String, avatar: File) {
        checkConnection()
        suspendCancellableCoroutine<Unit> {
            SendBird.updateCurrentUserInfoWithProfileImage(nickname, avatar) { e ->
                if (e != null) {
                    it.cancel(e)
                    return@updateCurrentUserInfoWithProfileImage
                }
                it.resume(Unit)
            }
        }
    }

    fun addChannelHandler(identifier: String, handler: SendBird.ChannelHandler) {
        SendBird.addChannelHandler(identifier, handler)
    }

    fun removeChannelHandler(identifier: String) {
        SendBird.removeChannelHandler(identifier)
    }

    suspend fun registerPushNotifications(token: String) {
        checkConnection()
        suspendCancellableCoroutine<Unit> {
            SendBird.registerPushTokenForCurrentUser(token) { _, e ->
                if (e != null) {
                    it.cancel(e)
                    return@registerPushTokenForCurrentUser
                }
                it.resume(Unit)
            }
        }
    }

    private fun updateMemoryCachedChannel(channel: GroupChannel) {
        if (channelsHashMap.containsKey(channel.url)) {
            channelsLastUsageTime[channel.url] = System.currentTimeMillis()
            channelsHashMap[channel.url] = channel
        }
    }

    suspend fun disconnect(): Unit = suspendCancellableCoroutine {
        SendBird.disconnect {
            it.resume(Unit)
        }
    }
}