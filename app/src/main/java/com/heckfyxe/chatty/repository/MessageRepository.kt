package com.heckfyxe.chatty.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.room.withTransaction
import com.heckfyxe.chatty.remote.SendBirdApi
import com.heckfyxe.chatty.repository.source.MessagesDataSource
import com.heckfyxe.chatty.room.AppDatabase
import com.heckfyxe.chatty.room.DialogDao
import com.heckfyxe.chatty.room.MessageDao
import com.heckfyxe.chatty.room.RoomMessage
import org.koin.core.KoinComponent
import org.koin.core.inject

class MessageRepository(val channelId: String) :
    KoinComponent {

    companion object {
        private const val PAGE_SIZE = 20
        private const val PREFETCH_SIZE = 8

        private val config = PagedList.Config.Builder()
            .setPageSize(PAGE_SIZE)
            .setPrefetchDistance(PREFETCH_SIZE)
            .setInitialLoadSizeHint(PAGE_SIZE)
            .build()
    }

    private val sendBirdApi: SendBirdApi by inject()

    private val database: AppDatabase by inject()
    private val dialogDao: DialogDao by inject()
    private val messageDao: MessageDao by inject()

    private val dataSourceFactory = MessagesDataSource.Factory(this)

    private var messagesSource: LiveData<PagedList<RoomMessage>>? = null
    private val _messages = MediatorLiveData<PagedList<RoomMessage>>()
    val messages: LiveData<PagedList<RoomMessage>> = _messages

    suspend fun init() {
        sendBirdApi.loadChannel(channelId)
        setInitialLoadKey(messageDao.getLastMessage(channelId).time)
    }

    private fun setInitialLoadKey(key: Long) {
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
        setInitialLoadKey(tempMessage.time)
        val message = channel.receive()
        database.withTransaction {
            messageDao.updateByRequestId(message)
            dialogDao.updateDialogLastMessageId(channelId, message.id)
        }
        setInitialLoadKey(message.time)
    }

    suspend fun startTyping() = sendBirdApi.startTyping(channelId)

    suspend fun endTyping() = sendBirdApi.endTyping(channelId)
}
