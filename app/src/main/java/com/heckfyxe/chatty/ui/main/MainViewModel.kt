package com.heckfyxe.chatty.ui.main

import android.view.View
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


class LaunchMessageEvent(
    val channelId: String,
    val interlocutor: User?,
    val lastMessageTime: Long,
    val sharedElements: Array<Pair<View, String>>
)

enum class Progress {
    LOADING,
    COMPLETED
}

class MainViewModel(private val repository: DialogRepository) : ViewModel() {

    private val _errors = MutableLiveData<Exception?>()
    val errors: LiveData<Exception?> = _errors

    private val _launchMessagesEvent = MutableLiveData<LaunchMessageEvent?>()
    val launchMessagesEvent: LiveData<LaunchMessageEvent?> = _launchMessagesEvent

    val chats: LiveData<List<Dialog>> = Transformations.map(repository.chats) {
        if (it != null)
            _progress.value = Progress.COMPLETED
        it.toDomain()
    }

    private val _progress = MutableLiveData<Progress>()
    val progress: LiveData<Progress> = _progress

    private val _isFABExpanded = MutableLiveData<Boolean>()
    val isFABExpanded: LiveData<Boolean> = _isFABExpanded

    private val channelHandler = object : SendBird.ChannelHandler() {
        override fun onMessageReceived(channel: BaseChannel, baseMessage: BaseMessage) {
            if (channel !is GroupChannel) return

            val dialog = channel.toDomain()

            viewModelScope.launch {
                repository.insertDialog(dialog)
            }
        }
    }

    init {
        _progress.value = Progress.LOADING
        _isFABExpanded.value = false
        launchChannelHandler()
        registerPushNotifications()
    }

    fun refreshChats() = viewModelScope.launch {
        try {
            repository.refresh()
        } catch (e: Exception) {
            _errors.value = e
        }
    }

    private fun launchChannelHandler() = viewModelScope.launch {
        try {
            repository.launchChannelHandler(channelHandler)
        } catch (e: Exception) {
            _errors.value = e
        }
    }

    private fun registerPushNotifications() = viewModelScope.launch {
        try {
            repository.registerPushNotifications()
        } catch (e: Exception) {
            _errors.value = e
        }
    }

    fun launchMessageFragment(dialog: Dialog, sharedElements: Array<Pair<View, String>>) {
        _launchMessagesEvent.value = LaunchMessageEvent(
            dialog.id,
            dialog.interlocutor,
            dialog.lastMessage!!.time,
            sharedElements
        )
    }

    fun addMessageFABClicked() {
        _isFABExpanded.value = !_isFABExpanded.value!!
    }

    fun onErrorGotten() {
        _errors.value = null
    }


    fun onMessageFragmentLaunched() {
        _launchMessagesEvent.value = null
    }

    fun logOut(action: () -> Unit) = viewModelScope.launch {
        try {
            repository.signOut(action)
        } catch (e: Exception) {
            _errors.value = e
        }
    }

    override fun onCleared() {
        repository.stopChannelHandler()
    }
}
