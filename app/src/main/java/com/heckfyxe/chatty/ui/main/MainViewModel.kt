package com.heckfyxe.chatty.ui.main

import androidx.lifecycle.*
import com.heckfyxe.chatty.model.Dialog
import com.heckfyxe.chatty.model.User
import com.heckfyxe.chatty.repository.DialogRepository
import com.heckfyxe.chatty.room.toDomain
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent


data class LaunchMessageEvent(
    val channelId: String,
    val interlocutor: User,
    val lastMessageTime: Long
)

class MainViewModel(private val repository: DialogRepository) : ViewModel(), KoinComponent {

    val currentUser: LiveData<User> = Transformations.map(repository.currentUser) {
        it.toDomain()
    }

    val errors: MutableLiveData<Exception?> = repository.errors

    private val _launchMessagesEvent = MutableLiveData<LaunchMessageEvent?>()
    val launchMessagesEvent: LiveData<LaunchMessageEvent?>
        get() = _launchMessagesEvent

    val chats: LiveData<List<Dialog>> = Transformations.map(repository.chats) {
        it.map { dialog ->
            dialog.toDomain()
        }
    }

    fun connectUser() = viewModelScope.launch {
        repository.connectUser()
    }

    fun loadChats() = viewModelScope.launch {
        repository.refresh()
    }

    fun launchMessageFragment(dialog: Dialog) {
        viewModelScope.launch {
            _launchMessagesEvent.postValue(
                LaunchMessageEvent(
                    dialog.id,
                    dialog.interlocutor,
                    dialog.lastMessage.time
                )
            )
        }
    }

    fun onMessageFragmentLaunched() {
        _launchMessagesEvent.postValue(null)
    }

    fun logOut(action: () -> Unit) = viewModelScope.launch {
        repository.logOut(action)
    }
}
