package com.heckfyxe.chatty.repository

import androidx.lifecycle.MutableLiveData
import androidx.room.withTransaction
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.room.*
import com.heckfyxe.chatty.util.sendbird.channelFromDevice
import com.heckfyxe.chatty.util.sendbird.getSender
import com.heckfyxe.chatty.util.sendbird.getText
import com.sendbird.android.BaseChannel
import com.sendbird.android.GroupChannel
import kotlinx.coroutines.*
import org.koin.standalone.KoinComponent
import org.koin.standalone.get
import org.koin.standalone.inject

class MessageRepository(channelId: String) : KoinComponent {

    companion object {
        private const val PREV_RESULT_SIZE = 20
    }

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    private val database: AppDatabase by inject()
    private val dialogDao: DialogDao by inject()
    private val userDao: UserDao by inject()
    private val messageDao: MessageDao by inject()

    private val channel: Deferred<GroupChannel> = scope.async {
        channelFromDevice(get(), channelId) as GroupChannel
    }

    private val currentUserId: String by inject(name = KOIN_USER_ID)
    val currentUser = loadCurrentUserAsync()

    val interlocutor = loadInterlocutorAsync()

    val messages = scope.async {
        messageDao.getMessagesLiveData(channel.await().url)
    }
    val errors = MutableLiveData<Exception>()

    private var isLoading = false
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

    fun getPrevMessages() {
        if (isLoading)
            return

        scope.launch {
            val lastMessage = messages.await().value?.firstOrNull() ?: return@launch
            val lastMessageId = lastMessage.id

            isLoading = true
            val channel = channel.await()
            channel.getPreviousMessagesById(
                lastMessageId,
                false, PREV_RESULT_SIZE,
                true, BaseChannel.MessageTypeFilter.ALL, ""
            ) { loadedMessages, error ->

                isLoading = false

                if (error != null) {
                    errors.postValue(error)
                    return@getPreviousMessagesById
                }

                if (loadedMessages.size < PREV_RESULT_SIZE)
                    isHistoryEmpty = true

                if (loadedMessages.isEmpty())
                    return@getPreviousMessagesById

                updateDatabase(loadedMessages.map {
                    Message(it.messageId, channel.url, it.createdAt, it.getSender().userId, it.getText())
                })
            }
        }
    }

    private fun updateDatabase(messages: List<Message>) {
        scope.launch {
            messageDao.insert(messages)
        }
    }

    fun sendTextMessage(text: String) {
        scope.launch {
            val channel = channel.await()
            channel.sendUserMessage(text) { message, error ->
                if (error != null) {
                    errors.postValue(error)
                    return@sendUserMessage
                }

                val roomMessage = Message(
                    message.messageId,
                    channel.url,
                    message.createdAt,
                    message.sender.userId,
                    message.getText()
                )

                scope.launch {
                    database.withTransaction {
                        messageDao.insert(roomMessage)
                        dialogDao.updateDialogLastMessageId(channel.url, roomMessage.id)
                    }
                }
            }
        }

    }

    fun startTyping() {
        scope.launch {
            channel.await().startTyping()
        }
    }

    fun endTyping() {
        scope.launch {
            channel.await().endTyping()
        }
    }

    fun clear() {
        job.cancel()
    }
}
