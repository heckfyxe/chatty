package com.heckfyxe.chatty.ui.message

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.CollectionReference
import com.heckfyxe.chatty.koin.KOIN_USERS_FIRESTORE_COLLECTION
import com.heckfyxe.chatty.model.Message
import com.heckfyxe.chatty.repository.DialogRepository
import com.heckfyxe.chatty.repository.MessageRepository
import com.heckfyxe.chatty.room.toDomain
import com.heckfyxe.chatty.util.sendbird.toDomain
import com.sendbird.android.BaseChannel
import com.sendbird.android.BaseMessage
import com.sendbird.android.GroupChannel
import com.sendbird.android.SendBird
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.parameter.parametersOf
import java.io.File

class MessageViewModel(
    private val channelId: String,
    private val interlocutorId: String?,
    lastMessageTime: Long
) : ViewModel(), MessageAdapter.LoadingListener, KoinComponent {

    private val dialogRepository: DialogRepository by inject()
    private val messageRepository: MessageRepository by inject { parametersOf(channelId) }

    private val usersRef: CollectionReference by inject(KOIN_USERS_FIRESTORE_COLLECTION)

    private val channelHandler = object : SendBird.ChannelHandler() {
        override fun onMessageReceived(channel: BaseChannel, baseMessage: BaseMessage) {
            if (channel !is GroupChannel) return

            val dialog = channel.toDomain()

            viewModelScope.launch {
                try {
                    dialogRepository.insertDialog(dialog)
                } catch (e: Exception) {
                }
            }

            if (dialog.id == channelId) {
                adapter.addMessages(listOf(baseMessage.toDomain()))
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

    private val _deleteImageFile = MutableLiveData<File?>()
    val deleteImageFile: LiveData<File?> = _deleteImageFile

    init {
        viewModelScope.launch {
            try {
                val lastMessage =
                    messageRepository.getMessageByTime(lastMessageTime)
                        ?: messageRepository.getLastMessage()
                        ?: messageRepository.refreshLastMessage().run {
                            messageRepository.getLastMessage()!!
                        }
                adapter.addMessages(listOf(lastMessage))

                lastMessageLiveData.value = lastMessage
                messageRepository.refreshLastMessage()
                lastMessageLiveData.value = messageRepository.getLastMessage()
            } catch (e: Exception) {
                _errors.value = e
            }
        }
        startInterlocutorEmotionTracking()
        launchChannelHandler()
    }

    private fun launchChannelHandler() = viewModelScope.launch {
        try {
            messageRepository.launchChannelHandler(channelHandler)
        } catch (e: Exception) {
            _errors.value = e
        }
    }

    override fun prefetchSize(): Int = 35

    override fun loadPreviousMessages(time: Long) {
        if (isPreviousMessagesLoading || isHistoryEmpty) return
        isPreviousMessagesLoading = true
        viewModelScope.launch {
            try {
                adapter.addMessages(messageRepository.loadPreviousMessages(time).also {
                    isHistoryEmpty = it.isEmpty()
                })
                messageRepository.refreshPreviousMessages(time)
                adapter.addMessages(messageRepository.loadPreviousMessages(time).also {
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
                adapter.addMessages(messageRepository.loadNextMessages(time))
                messageRepository.refreshNextMessages(time)
                adapter.addMessages(messageRepository.loadNextMessages(time))
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

    @UseExperimental(ExperimentalCoroutinesApi::class)
    private fun startInterlocutorEmotionTracking() = viewModelScope.launch {
        interlocutorId ?: return@launch
        try {
            callbackFlow {
                usersRef.document(interlocutorId).addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }

                    val emotion = snapshot?.getString("emotion") ?: return@addSnapshotListener
                    offer(emotion)
                }
                awaitClose()
            }.distinctUntilChanged().collect {
                _interlocutorEmotions.value = it
            }
        } catch (e: Exception) {
        }
    }

    fun onErrorMessagesDisplayed() {
        _errors.value = null
    }

    fun sendTextMessage(text: String) = GlobalScope.launch(Dispatchers.Main) {
        handleMessageSending(messageRepository.sendTextMessage(this, text))
    }

    fun sendImageMessage(file: File) = GlobalScope.launch(Dispatchers.Main) {
        handleMessageSending(messageRepository.sendImageMessage(this, file))
        _deleteImageFile.value = file
    }

    private suspend fun handleMessageSending(channel: ReceiveChannel<Message>) = coroutineScope {
        try {
            adapter.addMessages(listOf(channel.receive()))
            scrollDown()
            val message = channel.receive()
            launch {
                dialogRepository.insertMessage(channelId, message)
            }
            adapter.messageSent(message)
            scrollDown()
        } catch (e: Exception) {
            _errors.value = e
        }
    }

    fun onImageFileDeleted() {
        _deleteImageFile.value = null
    }

    fun startTyping() = viewModelScope.launch {
        try {
            messageRepository.startTyping()
        } catch (e: Exception) {
        }
    }

    fun endTyping() = viewModelScope.launch {
        try {
            messageRepository.endTyping()
        } catch (e: Exception) {
        }
    }

    override fun onCleared() {
        super.onCleared()

        messageRepository.stopChannelHandler()
    }
}