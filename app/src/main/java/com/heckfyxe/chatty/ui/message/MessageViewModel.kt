package com.heckfyxe.chatty.ui.message

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.heckfyxe.chatty.model.Message
import com.heckfyxe.chatty.model.User
import com.sendbird.android.BaseChannel
import com.sendbird.android.BaseMessage
import com.sendbird.android.GroupChannel
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class MessageViewModel : ViewModel(), KoinComponent {

    val errors = MutableLiveData<Exception>()
    val messages = MutableLiveData<List<Message>>()
    val interlocutor = MutableLiveData<User>()

    private lateinit var channel: GroupChannel
    val userId: String by inject("uid")
    private var lastMessageId: Long = 0
    private var isInitialized = false
    private var isLoading = false

    fun init(channel: GroupChannel) {
        if (isInitialized)
            return

        this.channel = channel

        val lastMessage = channel.lastMessage
        lastMessageId = lastMessage.messageId
        messages.postValue(listOf(lastMessage).withoutUserAvatar())

        if (channel.memberCount != 2) {
            throw Exception("Channel members count must be 2")
        }

        val interlocutor = channel.members.single { it.userId != userId }
        this.interlocutor.postValue(User(interlocutor))
    }

    fun sendMessage(text: String, success: (Message) -> Unit) {
        channel.sendUserMessage(text) { message, e ->
            if (e != null) {
                errors.postValue(e)
                return@sendUserMessage
            }

            success(Message(message))
        }
    }

    fun getPrevMessages() {
        if (isLoading)
            return

        isLoading = true
        channel.getPreviousMessagesById(
            channel.lastMessage.messageId,
            false, 20,
            false, BaseChannel.MessageTypeFilter.ALL, "") { loadedMessages, error ->

            isLoading = false

            if (error != null) {
                errors.postValue(error)
                return@getPreviousMessagesById
            }

            if (loadedMessages.isEmpty())
                return@getPreviousMessagesById

            lastMessageId = loadedMessages.last().messageId
            messages.postValue(loadedMessages.withoutUserAvatar())
        }
    }
}

fun List<BaseMessage>.withoutUserAvatar() =
    map<BaseMessage, Message> {
        val message = Message(it)
        (message.user as User).avatar = null
        message
    }
