package com.heckfyxe.chatty.ui.main

import androidx.lifecycle.*
import com.heckfyxe.chatty.model.Dialog
import com.heckfyxe.chatty.model.User
import com.heckfyxe.chatty.repository.DialogRepository
import com.heckfyxe.chatty.room.toDomain
import com.heckfyxe.chatty.util.sendbird.toDomain
import com.sendbird.android.BaseChannel
import com.sendbird.android.BaseMessage
import com.sendbird.android.GroupChannel
import com.sendbird.android.SendBird
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent


data class LaunchMessageEvent(
    val channelId: String,
    val interlocutor: User,
    val lastMessageTime: Long
)

enum class Progress {
    LOADING,
    COMPLETED
}

private const val CHANNEL_HANDLER_IDENTIFIER =
    "com.heckfyxe.chatty.ui.main.CHANNEL_HANDLER_IDENTIFIER"

class MainViewModel(private val repository: DialogRepository) : ViewModel(), KoinComponent {

    private val channelHandler = object : SendBird.ChannelHandler() {
        override fun onMessageReceived(channel: BaseChannel, baseMessage: BaseMessage) {
            if (channel !is GroupChannel) return

            val dialog = channel.toDomain()

            viewModelScope.launch {
                repository.insertDialog(dialog)
            }
        }
    }

    val currentUser: LiveData<User> = Transformations.map(repository.currentUser) {
        refreshChats()
        it.toDomain()
    }

    val errors: MutableLiveData<Exception?> = repository.errors

    private val _launchMessagesEvent = MutableLiveData<LaunchMessageEvent?>()
    val launchMessagesEvent: LiveData<LaunchMessageEvent?>
        get() = _launchMessagesEvent

    val chats: LiveData<List<Dialog>> = Transformations.map(repository.chats) {
        _progress.value = Progress.COMPLETED
        it.toDomain()
    }

    private val _progress = MutableLiveData<Progress>()
    val progress: LiveData<Progress> = _progress

    init {
        _progress.value = Progress.LOADING
        SendBird.addChannelHandler(CHANNEL_HANDLER_IDENTIFIER, channelHandler)
    }

    fun connectUser() = viewModelScope.launch {
        repository.connectUser()
    }

    private fun refreshChats() = viewModelScope.launch {
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

    override fun onCleared() {
        super.onCleared()

        SendBird.removeChannelHandler(CHANNEL_HANDLER_IDENTIFIER)
    }
}
