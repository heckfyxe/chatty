package com.heckfyxe.chatty.ui.message

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.CollectionReference
import com.heckfyxe.chatty.koin.KOIN_USERS_FIRESTORE_COLLECTION
import com.heckfyxe.chatty.model.Message
import com.heckfyxe.chatty.repository.MessageRepository
import com.heckfyxe.chatty.util.sendbird.toDomain
import com.sendbird.android.BaseChannel
import com.sendbird.android.BaseMessage
import com.sendbird.android.GroupChannel
import com.sendbird.android.SendBird
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.parameter.parametersOf

private const val CHANNEL_HANDLER_IDENTIFIER =
    "com.heckfyxe.chatty.ui.message.CHANNEL_HANDLER_IDENTIFIER"

class MessageViewModel(
    channelId: String,
    private val interlocutorId: String,
    lastMessageTime: Long
) : ViewModel(), MessageAdapter.LoadingListener, KoinComponent {

    private val repository: MessageRepository by inject { parametersOf(channelId) }

    private val usersRef: CollectionReference by inject(KOIN_USERS_FIRESTORE_COLLECTION)

    private val channelHandler = object : SendBird.ChannelHandler() {
        override fun onMessageReceived(channel: BaseChannel, baseMessage: BaseMessage) {
            if (channel !is GroupChannel) return

            val dialog = channel.toDomain()

            viewModelScope.launch {
                repository.insertDialog(dialog)
            }

            if (dialog.id == channelId) {
                adapter.addMessages(listOf(dialog.lastMessage))
                scrollDown()
            }
        }
    }

    val adapter = MessageAdapter().apply {
        setLoadingListener(this@MessageViewModel)
    }
    private var isPreviousMessagesLoading = false
    private var isNextMessagesLoading = false

    private var isHistoryEmpty = false

    private val lastMessageLiveData = MutableLiveData<Message>()

    private val _errors = MutableLiveData<Exception?>()
    val errors: LiveData<Exception?> = _errors
    private val _interlocutorEmotions = MutableLiveData<String>()
    val interlocutorEmotions: LiveData<String> = _interlocutorEmotions

    private val _scrollDown = MutableLiveData<Boolean>()
    val scrollDown: LiveData<Boolean> = _scrollDown

    init {
        viewModelScope.launch {
            val lastMessage =
                repository.getMessageByTime(lastMessageTime) ?: repository.getLastMessage()
                ?: repository.refreshLastMessage().run {
                    repository.getLastMessage()!!
                }
            adapter.addMessages(listOf(lastMessage))

            lastMessageLiveData.value = lastMessage
            repository.refreshLastMessage()
            lastMessageLiveData.value = repository.getLastMessage()
        }
        SendBird.addChannelHandler(CHANNEL_HANDLER_IDENTIFIER, channelHandler)
        startInterlocutorEmotionTracking()
    }

    override fun prefetchSize(): Int = 35

    override fun loadPreviousMessages(time: Long) {
        if (isPreviousMessagesLoading || isHistoryEmpty) return
        isPreviousMessagesLoading = true
        viewModelScope.launch {
            try {
                adapter.addMessages(repository.loadPreviousMessages(time).also {
                    isHistoryEmpty = it.isEmpty()
                })
                repository.refreshPreviousMessages(time)
                adapter.addMessages(repository.loadPreviousMessages(time).also {
                    isHistoryEmpty = it.isEmpty()
                })
            } catch (e: Exception) {
                _errors.value = e
            } finally {
                isPreviousMessagesLoading = false
            }
        }
    }

    override fun loadNextMessages(time: Long) {
        if (isNextMessagesLoading || time == lastMessageLiveData.value?.time) return
        isNextMessagesLoading = true
        viewModelScope.launch {
            try {
                adapter.addMessages(repository.loadNextMessages(time))
                repository.refreshNextMessages(time)
                adapter.addMessages(repository.loadNextMessages(time))
            } catch (e: Exception) {
                _errors.value = e
            } finally {
                isNextMessagesLoading = false
            }
        }
    }

    private fun scrollDown() {
        _scrollDown.value = true
    }

    fun onScrolledDown() {
        _scrollDown.value = false
    }

    private fun startInterlocutorEmotionTracking() {
        usersRef.document(interlocutorId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                return@addSnapshotListener
            }

            val emotion = snapshot?.getString("emotion") ?: return@addSnapshotListener
            _interlocutorEmotions.value = emotion
        }
    }

    fun onErrorMessagesDisplayed() {
        _errors.value = null
    }

    fun sendTextMessage(text: String) = viewModelScope.launch {
        try {
            val channel = repository.sendTextMessage(text)
            adapter.addMessages(listOf(channel.receive()))
            scrollDown()
            adapter.messageSent(channel.receive())
            scrollDown()
        } catch (e: Exception) {
            _errors.value = e
        }
    }

    fun startTyping() = viewModelScope.launch { repository.startTyping() }

    fun endTyping() = viewModelScope.launch { repository.endTyping() }

    override fun onCleared() {
        super.onCleared()

        SendBird.removeChannelHandler(CHANNEL_HANDLER_IDENTIFIER)
    }
}