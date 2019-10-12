package com.heckfyxe.chatty.ui.main

import androidx.lifecycle.*
import com.heckfyxe.chatty.model.ChatDialog
import com.heckfyxe.chatty.model.ChatMessage
import com.heckfyxe.chatty.model.ChatUser
import com.heckfyxe.chatty.repository.DialogRepository
import com.heckfyxe.chatty.room.User
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import java.util.*

class MainViewModel(private val repository: DialogRepository) : ViewModel(), KoinComponent {

    val currentUser: LiveData<User> = repository.currentUser

    val errors: MutableLiveData<Exception?> = repository.errors

    private val _launchMessagesEvent = MutableLiveData<LaunchMessageEvent?>()
    val launchMessagesEvent: LiveData<LaunchMessageEvent?>
        get() = _launchMessagesEvent

    private val _chats = MediatorLiveData<List<ChatDialog>>()
    val chats: LiveData<List<ChatDialog>> = Transformations.map(_chats) { it }

    init {
        _chats.addSource(repository.chats) {
            viewModelScope.launch {
                val chatDialogs: List<ChatDialog> = it.map {
                    async {
                        val lastMessage = repository.getMessageById(it.lastMessageId) ?: return@async null
                        val messageSender = repository.getUserById(lastMessage.senderId)
                        val user = with(messageSender) { ChatUser(id, name, avatarUrl) }
                        val chatMessage = with(lastMessage) { ChatMessage(id, Date(time), user, text) }
                        with(it) {
                            ChatDialog(id, name, photoUrl, chatMessage, unreadCount)
                        }
                    }
                }.awaitAll().filterNotNull()
                _chats.postValue(chatDialogs)
            }
        }
    }

    fun connectUser() = viewModelScope.launch {
        repository.connectUser()
    }

    fun loadChats() = viewModelScope.launch {
        repository.refresh()
    }

    fun launchMessageFragment(dialogId: String) {
        viewModelScope.launch {
            val interlocutor = repository.getInterlocutor(dialogId)
            _launchMessagesEvent.postValue(LaunchMessageEvent(dialogId, interlocutor))
        }
    }

    fun onMessageFragmentLaunched() {
        _launchMessagesEvent.postValue(null)
    }

    fun logOut(action: () -> Unit) = viewModelScope.launch {
        repository.logOut(action)
    }
}
