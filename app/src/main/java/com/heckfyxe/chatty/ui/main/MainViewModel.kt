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


data class LaunchMessageEvent(
    val channelId: String,
    val interlocutor: User?,
    val lastMessageTime: Long
)

enum class Progress {
    LOADING,
    COMPLETED
}

private const val CHANNEL_HANDLER_IDENTIFIER =
    "com.heckfyxe.chatty.ui.main.CHANNEL_HANDLER_IDENTIFIER"

class MainViewModel(private val repository: DialogRepository) : ViewModel() {

    private val channelHandler = object : SendBird.ChannelHandler() {
        override fun onMessageReceived(channel: BaseChannel, baseMessage: BaseMessage) {
            if (channel !is GroupChannel) return

            val dialog = channel.toDomain()

            viewModelScope.launch {
                repository.insertDialog(dialog)
            }
        }
    }

    private val _errors = MutableLiveData<Exception?>()
    val errors: LiveData<Exception?> = _errors

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
        refreshChats()
        viewModelScope.launch {
            repository.registerPushNotifications()
        }
    }

    private fun refreshChats() = viewModelScope.launch {
        try {
            repository.refresh()
        } catch (e: Exception) {
            _errors.value = e
        }
    }

    fun launchMessageFragment(dialog: Dialog) {
        _launchMessagesEvent.value = LaunchMessageEvent(
            dialog.id,
            dialog.interlocutor,
            dialog.lastMessage!!.time
        )
    }

    fun onErrorGotten() {
        _errors.value = null
    }


    fun onMessageFragmentLaunched() {
        _launchMessagesEvent.value = null
    }

    fun logOut(action: () -> Unit) = viewModelScope.launch {
        repository.logOut(action)
    }

    override fun onCleared() {
        super.onCleared()

        SendBird.removeChannelHandler(CHANNEL_HANDLER_IDENTIFIER)
    }
}
