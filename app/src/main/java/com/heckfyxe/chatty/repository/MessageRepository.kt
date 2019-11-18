package com.heckfyxe.chatty.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.room.withTransaction
import com.heckfyxe.chatty.model.Message
import com.heckfyxe.chatty.remote.SendBirdApi
import com.heckfyxe.chatty.repository.source.MessagesDataSource
import com.heckfyxe.chatty.room.AppDatabase
import com.heckfyxe.chatty.room.DialogDao
import com.heckfyxe.chatty.room.MessageDao
import com.heckfyxe.chatty.room.RoomMessage
import org.koin.core.KoinComponent
import org.koin.core.inject

class MessageRepository(val channelId: String, private val lastMessageTime: Long) :
    KoinComponent {

    companion object {
        private const val PAGE_SIZE = 40
        private const val PREFETCH_SIZE = PAGE_SIZE / 3

        private val config = PagedList.Config.Builder()
            .setPageSize(PAGE_SIZE)
//            .setPrefetchDistance(1)
            .build()
    }

    private val sendBirdApi: SendBirdApi by inject()

    private val database: AppDatabase by inject()
    private val dialogDao: DialogDao by inject()
    private val messageDao: MessageDao by inject()

    private val dataSourceFactory = MessagesDataSource.Factory(this)

    private var messagesSource: LiveData<PagedList<Message>>? = null
    private val _messages = MediatorLiveData<PagedList<Message>>()
    val messages: LiveData<PagedList<Message>> = _messages

    suspend fun init() {
        setInitialLoadKey(lastMessageTime)
        sendBirdApi.loadChannel(channelId)
    }

    private suspend fun setInitialLoadKey(key: Long) {
        if (key == -1L) {
            val lastMessage = messageDao.getLastMessage(channelId) ?: return
            setInitialLoadKey(lastMessage.time)
            return
        }
        if (messagesSource != null) {
            _messages.removeSource(messagesSource!!)
        }
        messagesSource = LivePagedListBuilder(dataSourceFactory, config)
            .setInitialLoadKey(key)
            .build()
        _messages.addSource(messagesSource!!) {
            _messages.postValue(it)
        }
    }

    suspend fun getPreviousMessagesByTime(time: Long, count: Int = PAGE_SIZE): List<RoomMessage> {
        val messages = sendBirdApi.getPreviousMessagesByTime(channelId, time, count)
        updateDatabase(messages)
        return messages
    }

    suspend fun getNextMessagesByTime(time: Long, count: Int = PAGE_SIZE): List<RoomMessage> {
        val messages = sendBirdApi.getNextMessagesByTime(channelId, time, count)
        updateDatabase(messages)
        return messages
    }

    private suspend fun updateDatabase(messages: List<RoomMessage>) {
        messageDao.insert(messages)
    }

    suspend fun sendTextMessage(text: String) {
        val channel = sendBirdApi.sendMessage(channelId, text)
        val tempMessage = channel.receive()
        messageDao.insert(tempMessage)
//        setInitialLoadKey(tempMessage.time)
        val message = channel.receive()
        database.withTransaction {
            messageDao.updateByRequestId(message)
            dialogDao.updateDialogLastMessageId(channelId, message.id)
        }
//        setInitialLoadKey(message.time)
    }

    suspend fun startTyping() = sendBirdApi.startTyping(channelId)

    suspend fun endTyping() = sendBirdApi.endTyping(channelId)
}
