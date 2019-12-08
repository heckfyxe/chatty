package com.heckfyxe.chatty.repository

import androidx.room.withTransaction
import com.heckfyxe.chatty.model.Message
import com.heckfyxe.chatty.remote.SendBirdApi
import com.heckfyxe.chatty.room.AppDatabase
import com.heckfyxe.chatty.room.DialogDao
import com.heckfyxe.chatty.room.MessageDao
import com.heckfyxe.chatty.room.toDomain
import com.heckfyxe.chatty.util.sendbird.toRoomMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject

private const val PAGE_SIZE = 70

class MessageRepository(val channelId: String) :
    KoinComponent {

    private val sendBirdApi: SendBirdApi by inject()

    private val database: AppDatabase by inject()
    private val dialogDao: DialogDao by inject()
    private val messageDao: MessageDao by inject()

    suspend fun getMessageByTime(time: Long) = withContext(Dispatchers.IO) {
        messageDao.getMessageById(time)?.toDomain()
    }

    suspend fun getLastMessage() = withContext(Dispatchers.IO) {
        messageDao.getLastMessage(channelId)?.toDomain()
    }

    suspend fun refreshLastMessage() = withContext(Dispatchers.IO) {
        val message = sendBirdApi.getLastMessage(channelId) ?: return@withContext
        messageDao.insert(message.toRoomMessage())
    }

    suspend fun loadPreviousMessages(time: Long, count: Int = PAGE_SIZE) =
        withContext(Dispatchers.IO) {
            messageDao.getPreviousMessagesByTime(channelId, time, count).toDomain()
        }

    suspend fun refreshPreviousMessages(time: Long, count: Int = PAGE_SIZE) {
        withContext(Dispatchers.IO) {
            val messages = sendBirdApi.getPreviousMessagesByTime(channelId, time, count)
            messageDao.insert(messages)
        }
    }

    suspend fun loadNextMessages(time: Long, count: Int = PAGE_SIZE) =
        withContext(Dispatchers.IO) {
            messageDao.getNextMessagesByTime(channelId, time, count).toDomain()
        }

    suspend fun refreshNextMessages(time: Long, count: Int = PAGE_SIZE) {
        withContext(Dispatchers.IO) {
            val messages = sendBirdApi.getNextMessagesByTime(channelId, time, count)
            messageDao.insert(messages)
        }
    }

    suspend fun sendTextMessage(text: String) = coroutineScope<ReceiveChannel<Message>> {
        val result = Channel<Message>(2)
        launch {
            try {
                val channel = sendBirdApi.sendMessage(channelId, text)
                val tempMessage = channel.receive()
                result.send(tempMessage.toDomain())
                messageDao.insert(tempMessage)
                val message = channel.receive()
                result.send(message.toDomain())
                database.withTransaction {
                    messageDao.updateByRequestId(message)
                    dialogDao.updateDialogLastMessageId(channelId, message.id)
                }
            } finally {
                result.close()
            }
        }
        result
    }

    suspend fun startTyping() = sendBirdApi.startTyping(channelId)

    suspend fun endTyping() = sendBirdApi.endTyping(channelId)
}
