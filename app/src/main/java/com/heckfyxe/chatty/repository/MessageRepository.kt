package com.heckfyxe.chatty.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.room.withTransaction
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.repository.source.MessagesDataSource
import com.heckfyxe.chatty.room.*
import com.heckfyxe.chatty.util.sendbird.channelFromDevice
import com.heckfyxe.chatty.util.sendbird.saveOnDevice
import com.heckfyxe.chatty.util.sendbird.toMessage
import com.sendbird.android.BaseChannel
import com.sendbird.android.GroupChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.KoinComponent
import org.koin.core.inject

class MessageRepository(val channelId: String) : KoinComponent {

    companion object {
        private const val PAGE_SIZE = 5
        private const val PREFETCH_SIZE = 2

        private val config = PagedList.Config.Builder()
            .setPageSize(PAGE_SIZE)
            .setPrefetchDistance(PREFETCH_SIZE)
            .setInitialLoadSizeHint(PAGE_SIZE)
            .build()
    }

    private val context: Context by inject()

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    private val database: AppDatabase by inject()
    private val dialogDao: DialogDao by inject()
    private val userDao: UserDao by inject()
    private val messageDao: MessageDao by inject()

    private val channelMutex = Mutex()
    val channel: Deferred<GroupChannel> = scope.async {
        channelFromDevice(channelId) as GroupChannel
    }

    private val currentUserId: String by inject(KOIN_USER_ID)
    val currentUser = loadCurrentUserAsync()

    private val dataSourceFactory = MessagesDataSource.Factory(this)

    val interlocutor = loadInterlocutorAsync()
    val messages = LivePagedListBuilder(dataSourceFactory, config).build()
    val errors = MutableLiveData<Exception?>()

    private var isHistoryEmpty = false

    init {
        scope.launch {
            channel.await().markAsRead()
            currentUser.await()
            interlocutor.await()
        }
    }

    private fun loadCurrentUserAsync() = loadUserByIdAsync(currentUserId)

    private fun loadInterlocutorAsync() =
        scope.async {
            val interlocutorId = channel.await().members.single { it.userId != currentUserId }.userId
            loadUserByIdAsync(interlocutorId).await()
        }


    private fun loadUserByIdAsync(id: String): Deferred<User> = scope.async {
        userDao.getUserById(id)?.let {
            return@async it
        }

        val channel = channel.await()

        val member = channel.members.single { it.userId == id }
        val user = member.let {
            User(it.userId, it.nickname, it.profileUrl)
        }
        scope.launch {
            userDao.insert(user)
        }
        return@async user
    }

    suspend fun getPrevMessages(lastMessageId: Long, count: Int = PAGE_SIZE): List<Message> {
        val channel = channel.await()
        val chan = Channel<Boolean>(1)
        var messages: List<Message> = emptyList()

        channel.getPreviousMessagesById(
            lastMessageId,
            false, count,
            true, BaseChannel.MessageTypeFilter.ALL, ""
        ) { loadedMessages, error ->

            if (error != null) {
                errors.postValue(error)
                scope.launch {
                    chan.send(false)
                }
                return@getPreviousMessagesById
            }

            val newMessages = loadedMessages.map {
                it.toMessage(currentUserId)
            }

            scope.launch {
                messages = newMessages
                updateDatabase(messages)
                chan.send(true)
            }

            if (loadedMessages.size < PAGE_SIZE)
                isHistoryEmpty = true
        }

        chan.receive()
        chan.close()

        return messages
    }

    suspend fun getPreviousMessagesByTime(time: Long, count: Int = PAGE_SIZE): List<Message> {
        val channel = channel.await()
        val chan = Channel<Boolean>(1)
        var messages: List<Message> = emptyList()

        channel.getPreviousMessagesByTimestamp(
            time,
            false, count,
            true, BaseChannel.MessageTypeFilter.ALL, ""
        ) { loadedMessages, error ->

            if (error != null) {
                errors.postValue(error)
                scope.launch {
                    chan.send(false)
                }
                return@getPreviousMessagesByTimestamp
            }

            val newMessages = loadedMessages.map {
                it.toMessage(currentUserId)
            }

            scope.launch {
                messages = newMessages
                updateDatabase(messages)
                chan.send(true)
            }

            if (loadedMessages.size < PAGE_SIZE)
                isHistoryEmpty = true
        }

        chan.receive()
        chan.close()

        return messages
    }

    private suspend fun updateDatabase(messages: List<Message>) {
        messageDao.insert(messages)
    }

    private suspend fun saveChannel() {
        channelMutex.withLock {
            channel.await().saveOnDevice()
        }
    }

    suspend fun sendTextMessage(text: String) {
        val channel = channel.await()
        val tempMessage = channel.sendUserMessage(text) { message, error ->
            if (error != null) {
                errors.postValue(error)
                return@sendUserMessage
            }

            val roomMessage = message.toMessage(out = true, sent = true)

            Log.i("MessageRepository", "sent: " + roomMessage.requestId)
            scope.launch {
                database.withTransaction {
                    messageDao.updateByRequestId(roomMessage)
                    dialogDao.updateDialogLastMessageId(channel.url, roomMessage.id)
                }
                saveChannel()
                dataSourceFactory.invalidate()
            }
        }
        val lastMessageTime = channel.lastMessage.createdAt
        val tempRoomMessage = tempMessage.toMessage(out = true, sent = false).apply {
            time = lastMessageTime + 1
        }
        Log.i("MessageRepository", "sending: " + tempRoomMessage.requestId)
        messageDao.insert(tempRoomMessage)
        saveChannel()
        dataSourceFactory.invalidate()
    }

    suspend fun startTyping() {
        channel.await().startTyping()
    }

    suspend fun endTyping() {
        channel.await().endTyping()
    }

    fun clear() {
        job.cancel()
    }
}
