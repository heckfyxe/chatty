package com.heckfyxe.chatty.ui.message

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.CollectionReference
import com.heckfyxe.chatty.koin.KOIN_USERS_FIRESTORE_COLLECTION
import com.heckfyxe.chatty.model.ChatUser
import com.heckfyxe.chatty.repository.MessageRepository
import com.heckfyxe.chatty.room.Message
import com.heckfyxe.chatty.util.room.toChatUser
import kotlinx.coroutines.*
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class MessageViewModel(channelId: String) : ViewModel(), KoinComponent {

    private val repository: MessageRepository by inject { parametersOf(channelId) }

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    private val usersRef: CollectionReference by inject(KOIN_USERS_FIRESTORE_COLLECTION)

    private var messageCount = 0

    val messages = MutableLiveData<List<Message>>()
    val needsToScroll = MutableLiveData<Int>()
    val errors = repository.errors
    val interlocutorLiveData = MutableLiveData<ChatUser>()
    val interlocutorEmotions = MutableLiveData<String>()

    private lateinit var interlocutor: ChatUser
    private lateinit var currentUser: ChatUser

    private val messagesObserver = Observer<List<Message>> {
        if (it.size > messageCount) {
            messageCount = it.size
            needsToScroll.postValue(0)
        }
        messages.postValue(it)
    }

    init {
        scope.launch {
            interlocutor = repository.interlocutor.await().toChatUser()
            startInterlocutorEmotionTracking()
            interlocutorLiveData.postValue(interlocutor)
            currentUser = repository.currentUser.await().toChatUser()
            withContext(Dispatchers.Main) {
                repository.messages.await().observeForever(messagesObserver)
            }
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

    fun sendTextMessage(text: String) = repository.sendTextMessage(text)

    fun getPrevMessages() = repository.getPrevMessages()

    fun startTyping() = repository.startTyping()

    fun endTyping() = repository.endTyping()

    override fun onCleared() {
        super.onCleared()

        job.cancel()
        repository.clear()
        scope.launch {
            repository.messages.await().removeObserver(messagesObserver)
        }
    }
}