package com.heckfyxe.chatty.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.heckfyxe.chatty.model.ChatDialog
import com.heckfyxe.chatty.model.ChatMessage
import com.heckfyxe.chatty.model.ChatUser
import com.heckfyxe.chatty.repository.DialogRepository
import com.heckfyxe.chatty.room.Dialog
import com.sendbird.android.User
import kotlinx.coroutines.*
import org.koin.standalone.KoinComponent
import java.util.*

class MainViewModel(private val repository: DialogRepository) : ViewModel(), KoinComponent {

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    val currentUser: LiveData<User> = repository.currentUser

    val errors: LiveData<Exception> = repository.errors

    private val _chats = MutableLiveData<List<ChatDialog>>()
    val chats: LiveData<List<ChatDialog>> = Transformations.map(_chats) { it }

    private val observer = Observer<List<Dialog>> {
        scope.launch {
            val chatDialogs = it.map {
                scope.async {
                    val lastMessage = repository.getMessageById(it.lastMessageId)
                    val messageSender = repository.getUserById(lastMessage.senderId)
                    val user = with(messageSender) { ChatUser(id, name, avatarUrl) }
                    val chatMessage = with(lastMessage) { ChatMessage(id, Date(time), user, text) }
                    with(it) {
                        ChatDialog(id, name, photoUrl, chatMessage, unreadCount)
                    }
                }
            }.awaitAll()
            _chats.postValue(chatDialogs)
        }
    }

    init {
        repository.chats.observeForever(observer)
    }

    fun connectUser() = repository.connectUser()

    fun getMessageById(id: Long) = repository.getMessageById(id)

    fun loadChats() = repository.refresh()

    fun logOut(action: () -> Unit) = repository.logOut(action)

    override fun onCleared() {
        super.onCleared()

        job.cancel()
        repository.clear()
        repository.chats.removeObserver(observer)
    }
}
