package com.heckfyxe.chatty.remote

import com.heckfyxe.chatty.room.RoomMessage
import com.heckfyxe.chatty.util.sendbird.toRoomMessage
import com.sendbird.android.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


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

    @OptIn(ObsoleteCoroutinesApi::class)
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

    private suspend fun connect(): User = suspendCancellableCoroutine {
        SendBird.connect(userId) { user, e ->
            if (e != null) {
                it.cancel(e)
            } else {
                it.resume(user)
            }
        }
    }

    suspend fun getCurrentUser(): User {
        checkConnection()
        return currentUser!!
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getChannels(): Flow<List<GroupChannel>> {
        checkConnection()
        return callbackFlow<List<GroupChannel>> {
            val query = GroupChannel.createMyGroupChannelListQuery()
            val handler = object : GroupChannelListQuery.GroupChannelListQueryResultHandler {
                override fun onResult(channels: List<GroupChannel>?, e: SendBirdException?) {
                    if (e != null) {
                        close(e)
                        return
                    }
                    if (channels.isNullOrEmpty()) {
                        close()
                        return
                    }
                    offer(channels)
                    query.next(this)
                }
            }
            query.next(handler)
            awaitClose()
        }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun sendMessage(channelId: String, text: String) = callbackFlow {
        checkConnection()
        val channel = getChannel(channelId)
        val tempMessage = channel.sendUserMessage(text.trim()) { message, e ->
            if (e != null) {
                close(e)
                return@sendUserMessage
            }

            offer(message.toRoomMessage())
            close()
        }
        offer(tempMessage.toRoomMessage())
        awaitClose()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun sendMessage(channelId: String, file: File) = callbackFlow {
        checkConnection()
        val channel = getChannel(channelId)
        val tempMessage = channel.sendFileMessage(FileMessageParams(file)) { message, e ->
            if (e != null) {
                close(e)
                return@sendFileMessage
            }

            offer(message.toRoomMessage())
            close()
        }
        offer(tempMessage.toRoomMessage().copy(file = file.path))
        awaitClose()
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

    suspend fun getUserById(userId: String): User? {
        checkConnection()
        return suspendCancellableCoroutine { cont ->
            val query = SendBird.createApplicationUserListQuery().apply {
                setUserIdsFilter(listOf(userId))
                setLimit(1)
            }
            query.next { userList, exception ->
                if (exception != null) {
                    cont.resumeWithException(exception)
                    return@next
                }
                if (!userList.isNullOrEmpty()) {
                    cont.resume(userList.first())
                } else {
                    cont.resume(null)
                }
            }
        }
    }

    suspend fun addChannelHandler(identifier: String, handler: SendBird.ChannelHandler) {
        checkConnection()
        SendBird.addChannelHandler(identifier, handler)
    }

    fun removeChannelHandler(identifier: String) {
        SendBird.removeChannelHandler(identifier)
    }

    suspend fun markMessagesAsRead(channelUrl: String) {
        checkConnection()
        getChannel(channelUrl).markAsRead()
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

    private suspend fun unregisterPushNotifications() {
        checkConnection()
        suspendCancellableCoroutine<Unit> {
            SendBird.unregisterPushTokenAllForCurrentUser { e ->
                if (e != null) {
                    it.cancel(e)
                    return@unregisterPushTokenAllForCurrentUser
                }
                it.resume(Unit)
            }
        }
    }

    private suspend fun disconnect(): Unit = suspendCancellableCoroutine {
        SendBird.disconnect {
            it.resume(Unit)
        }
    }

    suspend fun signOut() {
        unregisterPushNotifications()
        disconnect()
    }
}