package com.heckfyxe.chatty.ui.message

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.CollectionReference
import com.heckfyxe.chatty.koin.KOIN_USERS_FIRESTORE_COLLECTION
import com.heckfyxe.chatty.model.ChatUser
import com.heckfyxe.chatty.repository.MessageRepository
import com.heckfyxe.chatty.util.room.toChatUser
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.parameter.parametersOf

class MessageViewModel(channelId: String) : ViewModel(), KoinComponent {

    private val repository: MessageRepository by inject { parametersOf(channelId) }

    private val usersRef: CollectionReference by inject(KOIN_USERS_FIRESTORE_COLLECTION)

    val messages = repository.messages
    val errors = repository.errors
    val interlocutorLiveData = MutableLiveData<ChatUser>()
    val interlocutorEmotions = MutableLiveData<String>()

    private lateinit var interlocutor: ChatUser
    private lateinit var currentUser: ChatUser

    init {
        viewModelScope.launch {
            interlocutor = repository.interlocutor.await().toChatUser()
            startInterlocutorEmotionTracking()
            interlocutorLiveData.postValue(interlocutor)
            currentUser = repository.currentUser.await().toChatUser()
        }
    }

    private fun startInterlocutorEmotionTracking() {
        usersRef.document(interlocutor.id).addSnapshotListener { snapshot, e ->
            if (e != null) {
                return@addSnapshotListener
            }

            val emotion = snapshot?.getString("emotion") ?: return@addSnapshotListener
            interlocutorEmotions.postValue(emotion)
        }
    }

    fun sendTextMessage(text: String) = viewModelScope.launch { repository.sendTextMessage(text) }

    fun startTyping() = viewModelScope.launch { repository.startTyping() }

    fun endTyping() = viewModelScope.launch { repository.endTyping() }

    override fun onCleared() {
        super.onCleared()

        repository.clear()
    }
}