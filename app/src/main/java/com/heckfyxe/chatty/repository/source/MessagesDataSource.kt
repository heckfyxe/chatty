package com.heckfyxe.chatty.repository.source

import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import com.heckfyxe.chatty.model.Message
import com.heckfyxe.chatty.repository.MessageRepository
import com.heckfyxe.chatty.room.MessageDao
import com.heckfyxe.chatty.room.RoomMessage
import com.heckfyxe.chatty.room.toDomain
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

    private val messagesLiveData = messagesDao.getMessagesLiveData(repository.channelId)
    private val messagesChangesLiveData = Transformations.distinctUntilChanged(messagesLiveData)


    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    private var isChangesDetectionStarted = false
    private val changesObserver: Observer<List<RoomMessage>> = Observer {
        if (isChangesDetectionStarted)
            invalidate()
        else
            isChangesDetectionStarted = true
    }

    init {
        scope.launch(Dispatchers.Main) {
            messagesChangesLiveData.observeForever(changesObserver)
        }
    }

    override fun loadInitial(
        params: LoadInitialParams<Long>,
        callback: LoadInitialCallback<Message>
    ) {
        scope.launch {
            val localMessages = messagesDao.getPreviousMessagesByTime(
                repository.channelId,
                params.requestedInitialKey ?: return@launch,
                params.requestedLoadSize - 1
            )
            val message = messagesDao.getMessageByTime(params.requestedInitialKey!!)
            callback.onResult(
                sortAndFilter(
                    listOf(
                message, *localMessages.toTypedArray()
                    )
                )
            )
            repository.getPreviousMessagesByTime(
                params.requestedInitialKey!!,
                params.requestedLoadSize
            )
        }
    }

    override fun loadAfter(params: LoadParams<Long>, callback: LoadCallback<Message>) {
        scope.launch {
            val localMessages = messagesDao.getPreviousMessagesByTime(
                repository.channelId,
                params.key,
                params.requestedLoadSize
            )
            callback.onResult(sortAndFilter(localMessages))
            repository.getPreviousMessagesByTime(params.key, params.requestedLoadSize)
        }
    }

    override fun loadBefore(params: LoadParams<Long>, callback: LoadCallback<Message>) {
        scope.launch {
            val localMessages = messagesDao.getNextMessagesByTime(
                repository.channelId,
                params.key,
                params.requestedLoadSize
            )
            callback.onResult(sortAndFilter(localMessages))
            repository.getNextMessagesByTime(params.key, params.requestedLoadSize)
        }
    }

    private fun sortAndFilter(messages: List<RoomMessage?>): List<Message> =
        messages.filterNotNull().sortedByDescending { it.time }.map { it.toDomain() }

    override fun getKey(item: Message): Long = item.time

    override fun invalidate() {
        super.invalidate()
        scope.launch(Dispatchers.Main) {
            messagesChangesLiveData.removeObserver(changesObserver)
            job.cancel()
        }
    }

    class Factory(private val repository: MessageRepository) :
        DataSource.Factory<Long, Message>() {
        override fun create(): DataSource<Long, Message> = MessagesDataSource(repository)
    }
}