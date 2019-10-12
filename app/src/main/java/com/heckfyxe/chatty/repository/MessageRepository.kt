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
import com.heckfyxe.chatty.room.Message
import com.heckfyxe.chatty.room.MessageDao
import org.koin.core.KoinComponent
import org.koin.core.inject

class MessageRepository(val channelId: String) :
    KoinComponent {

    companion object {
        private const val PAGE_SIZE = 40
        private const val PREFETCH_SIZE = 17

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

    private val _messages = MediatorLiveData<PagedList<Message>>()
    val messages: LiveData<PagedList<Message>> = _messages

    suspend fun init() {
        sendBirdApi.loadChannel(channelId)
        val pagedList = LivePagedListBuilder(dataSourceFactory, config)
            .setInitialLoadKey(messageDao.getLastMessage(channelId).time)
            .build()
        _messages.addSource(pagedList) {
            _messages.postValue(it)
        }
    }

    suspend fun getPreviousMessagesByTime(time: Long, count: Int = PAGE_SIZE): List<Message> {
        val messages = sendBirdApi.getPreviousMessagesByTime(channelId, time, count)
        updateDatabase(messages)
        return messages
    }

    suspend fun getNextMessagesByTime(time: Long, count: Int = PAGE_SIZE): List<Message> {
        val messages = sendBirdApi.getNextMessagesByTime(channelId, time, count)
        updateDatabase(messages)
        return messages
    }

    private suspend fun updateDatabase(messages: List<Message>) {
        messageDao.insert(messages)
    }

    suspend fun sendTextMessage(text: String) {
        val channel = sendBirdApi.sendMessage(channelId, text)
        val tempMessage = channel.receive()
        messageDao.insert(tempMessage)
        dataSourceFactory.invalidate()
        val message = channel.receive()
        database.withTransaction {
            messageDao.updateByRequestId(message)
            dialogDao.updateDialogLastMessageId(channelId, message.id)
        }
        dataSourceFactory.invalidate()
    }

    suspend fun startTyping() = sendBirdApi.startTyping(channelId)

    suspend fun endTyping() = sendBirdApi.endTyping(channelId)
}
