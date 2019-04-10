package com.heckfyxe.chatty.repository

import androidx.lifecycle.MutableLiveData
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.room.Message
import com.heckfyxe.chatty.room.MessageDao
import com.heckfyxe.chatty.room.UserDao
import com.heckfyxe.chatty.util.sendbird.channelFromDevice
import com.heckfyxe.chatty.util.sendbird.getSender
import com.heckfyxe.chatty.util.sendbird.getText
import com.sendbird.android.BaseChannel
import com.sendbird.android.GroupChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.standalone.KoinComponent
import org.koin.standalone.get
import org.koin.standalone.inject

class MessageRepository(channelId: String) : KoinComponent {

    companion object {
        private const val PREV_RESULT_SIZE = 20
    }

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    private val userId: String by inject(name = KOIN_USER_ID)

    private val userDao: UserDao by inject()
    private val messageDao: MessageDao by inject()

    private val channel: GroupChannel = channelFromDevice(get(), channelId) as GroupChannel

    val messages = messageDao.getMessagesLiveData(channel.url)
    val errors = MutableLiveData<Exception>()
    val interlocutor = userDao.getUserLiveDataById(userId)

    private var isLoading = false
    private var isHistoryEmpty = false

    fun getPrevMessages() {
        if (isLoading)
            return

        val lastMessage = messages.value?.firstOrNull() ?: return
        val lastMessageId = lastMessage.id

        isLoading = true
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

    private fun updateDatabase(messages: List<Message>) {
        scope.launch {
            messageDao.insert(messages)
        }
    }

    fun sendTextMessage(text: String) {
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
                messageDao.insert(roomMessage)
            }
        }
    }

    fun startTyping() {
        channel.startTyping()
    }

    fun endTyping() {
        channel.endTyping()
    }
}