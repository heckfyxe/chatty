package com.heckfyxe.chatty.repository.source

import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import com.heckfyxe.chatty.repository.MessageRepository
import com.heckfyxe.chatty.room.Message
import com.heckfyxe.chatty.room.MessageDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject

class MessagesDataSource(private val repository: MessageRepository) :
    ItemKeyedDataSource<Long, Message>(),
    KoinComponent {

    private val messagesDao: MessageDao by inject()

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    override fun loadInitial(
        params: LoadInitialParams<Long>,
        callback: LoadInitialCallback<Message>
    ) {
        scope.launch {
            val localMessages = messagesDao.getPreviousMessagesByTime(
                repository.channelId,
                params.requestedInitialKey!!,
                params.requestedLoadSize
            )
            val message = messagesDao.getMessageByTime(params.requestedInitialKey!!)
            callback.onResult(listOf(message, *localMessages.toTypedArray()))
            val messages = repository.getPreviousMessagesByTime(
                params.requestedInitialKey!!, params.requestedLoadSize
            )
            if (messages.toSet() != localMessages.toSet()) {
                invalidate()
            }
        }
    }

    override fun loadAfter(params: LoadParams<Long>, callback: LoadCallback<Message>) {
        scope.launch {
            val localMessages = messagesDao.getPreviousMessagesByTime(
                repository.channelId,
                params.key,
                params.requestedLoadSize
            )
            callback.onResult(localMessages)
            val messages =
                repository.getPreviousMessagesByTime(params.key, params.requestedLoadSize)
            if (messages.toSet() != localMessages.toSet())
                invalidate()
        }
    }

    override fun loadBefore(params: LoadParams<Long>, callback: LoadCallback<Message>) {
        scope.launch {
            val localMessages = messagesDao.getNextMessagesByTime(
                repository.channelId,
                params.key,
                params.requestedLoadSize
            )
            callback.onResult(localMessages)
            val messages =
                repository.getNextMessagesByTime(params.key, params.requestedLoadSize)
            if (messages.toSet() != localMessages.toSet())
                invalidate()
        }
    }

    override fun getKey(item: Message): Long = item.time

    class Factory(private val repository: MessageRepository) : DataSource.Factory<Long, Message>() {
        private var dataSource: MessagesDataSource? = null

        override fun create(): DataSource<Long, Message> {
            dataSource = MessagesDataSource(repository)
            return dataSource!!
        }

        fun invalidate() {
            dataSource?.invalidate()
        }
    }
}