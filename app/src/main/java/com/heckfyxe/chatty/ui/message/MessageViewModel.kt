package com.heckfyxe.chatty.ui.message

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.model.ChatMessage
import com.heckfyxe.chatty.model.ChatUser
import com.sendbird.android.BaseChannel
import com.sendbird.android.GroupChannel
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.*

class MessageViewModel : ViewModel(), KoinComponent {

    companion object {
        private const val PREV_RESULT_SIZE = 20
    }

    val errors = MutableLiveData<Exception>()
    val messagesUpdatedLiveData = MutableLiveData<Boolean>()
    val interlocutor = MutableLiveData<ChatUser>()

    private lateinit var channel: GroupChannel
    val userId: String by inject(KOIN_USER_ID)
    private var lastMessageId: Long = 0
    var isInitialized = false
        private set
    private var isLoading = false
    private var isHistoryEmpty = false

    val messageList = LinkedList<ChatMessage>()

    fun init(channel: GroupChannel) {
        if (isInitialized)
            return

        isInitialized = true

        this.channel = channel

        val lastMessage = channel.lastMessage
        lastMessageId = lastMessage.messageId
        messageList.add(ChatMessage(lastMessage))
        messagesUpdatedLiveData.postValue(true)

        if (channel.memberCount != 2) {
            throw Exception("Channel members count must be 2")
        }

        val interlocutor = channel.members.single { it.userId != userId }
        this.interlocutor.postValue(ChatUser(interlocutor))
    }

    fun sendMessage(text: String, success: () -> Unit) {
        channel.sendUserMessage(text) { message, e ->
            if (e != null) {
                errors.postValue(e)
                return@sendUserMessage
            }

            messageList.addFirst(ChatMessage(message))
            messagesUpdatedLiveData.postValue(true)
            success()
        }
    }

    fun getPrevMessages() {
        if (isLoading)
            return

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


            lastMessageId = loadedMessages.last().messageId

            val messages = loadedMessages.map { ChatMessage(it) }
            messageList.addAll(messages)
            messagesUpdatedLiveData.postValue(true)
        }
    }

    fun startTyping() {
        channel.startTyping()
    }

    fun endTyping() {
        channel.endTyping()
    }
}