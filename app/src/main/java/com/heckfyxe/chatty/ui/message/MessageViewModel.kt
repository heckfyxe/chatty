package com.heckfyxe.chatty.ui.message

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.CollectionReference
import com.heckfyxe.chatty.koin.KOIN_USERS_FIRESTORE_COLLECTION
import com.heckfyxe.chatty.repository.MessageRepository
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.parameter.parametersOf

class MessageViewModel(channelId: String, private val interlocutorId: String) : ViewModel(), KoinComponent {

    private val repository: MessageRepository by inject { parametersOf(channelId) }

    private val usersRef: CollectionReference by inject(KOIN_USERS_FIRESTORE_COLLECTION)

    val messages = repository.messages
    val errors = MutableLiveData<Exception?>()
    val interlocutorEmotions = MutableLiveData<String>()
    private val _scrollDownEvent = MutableLiveData<Boolean>()
    val scrollDownEvent: LiveData<Boolean>
        get() = _scrollDownEvent

    init {
        viewModelScope.launch {
            repository.init()
            startInterlocutorEmotionTracking()
        }
    }

    private suspend fun startInterlocutorEmotionTracking() {
        usersRef.document(interlocutorId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                return@addSnapshotListener
            }

            val emotion = snapshot?.getString("emotion") ?: return@addSnapshotListener
            interlocutorEmotions.postValue(emotion)
        }
    }

    fun onScrolledDown() {
        _scrollDownEvent.postValue(false)
    }

    fun sendTextMessage(text: String) = viewModelScope.launch {
        repository.sendTextMessage(text)
        _scrollDownEvent.postValue(true)
    }

    fun startTyping() = viewModelScope.launch { repository.startTyping() }

    fun endTyping() = viewModelScope.launch { repository.endTyping() }
}