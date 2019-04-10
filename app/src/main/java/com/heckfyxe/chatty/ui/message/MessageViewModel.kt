package com.heckfyxe.chatty.ui.message

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.heckfyxe.chatty.model.ChatMessage
import com.heckfyxe.chatty.model.ChatUser
import com.heckfyxe.chatty.repository.MessageRepository
import com.heckfyxe.chatty.room.Message
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.*

class MessageViewModel(channelId: String) : ViewModel(), KoinComponent {

    private val repository: MessageRepository by inject { parametersOf(channelId) }

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<ChatMessage>> = Transformations.map(_messages) { messages ->
        messages.map {
            ChatMessage(it.id, Date(it.time), interlocutor.value!!, it.text)
        }
    }
    val errors = repository.errors
    val interlocutor: LiveData<ChatUser> = Transformations.map(repository.interlocutor) {
        ChatUser(it.id, it.name, it.avatarUrl)
    }

    private val messagesObserver = Observer<List<Message>> {
        _messages.postValue(it)
    }

    private val interlocutorObserver = Observer<ChatUser> {
        repository.messages.observeForever(messagesObserver)
    }

    init {
        interlocutor.observeForever(interlocutorObserver)
    }

    fun sendTextMessage(text: String) = repository.sendTextMessage(text)

    fun getPrevMessages() = repository.getPrevMessages()

    fun startTyping() = repository.startTyping()

    fun endTyping() = repository.endTyping()

    override fun onCleared() {
        super.onCleared()

        interlocutor.removeObserver(interlocutorObserver)
        repository.messages.removeObserver(messagesObserver)
    }
}