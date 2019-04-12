package com.heckfyxe.chatty.ui.message

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.model.ChatMessage
import com.heckfyxe.chatty.model.ChatUser
import com.heckfyxe.chatty.repository.MessageRepository
import com.heckfyxe.chatty.room.Message
import com.heckfyxe.chatty.util.room.toChatUser
import kotlinx.coroutines.*
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.*

class MessageViewModel(channelId: String) : ViewModel(), KoinComponent {

    private val repository: MessageRepository by inject { parametersOf(channelId) }

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    private val currentUserId: String by inject(KOIN_USER_ID)

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<ChatMessage>> = Transformations.map(_messages) { messages ->
        messages.map {
            ChatMessage(
                it.id,
                Date(it.time),
                if (it.senderId == currentUserId)
                    currentUser
                else
                    interlocutor,
                it.text
            )
        }
    }
    val errors = repository.errors
    val interlocutorLiveData = MutableLiveData<ChatUser>()

    private lateinit var interlocutor: ChatUser
    private lateinit var currentUser: ChatUser

    private val messagesObserver = Observer<List<Message>> {
        _messages.postValue(it)
    }

    init {
        scope.launch {
            interlocutor = repository.interlocutor.await().toChatUser()
            interlocutorLiveData.postValue(interlocutor)
            currentUser = repository.currentUser.await().toChatUser()
            withContext(Dispatchers.Main) {
                repository.messages.observeForever(messagesObserver)
            }
        }
    }

    fun sendTextMessage(text: String) = repository.sendTextMessage(text)

    fun getPrevMessages() = repository.getPrevMessages()

    fun startTyping() = repository.startTyping()

    fun endTyping() = repository.endTyping()

    override fun onCleared() {
        super.onCleared()

        job.cancel()
        repository.clear()
        repository.messages.removeObserver(messagesObserver)
    }
}