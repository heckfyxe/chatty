package com.heckfyxe.chatty.repository

import androidx.room.withTransaction
import com.heckfyxe.chatty.model.Message
import com.heckfyxe.chatty.remote.SendBirdApi
import com.heckfyxe.chatty.room.*
import com.heckfyxe.chatty.util.sendbird.toRoomMessage
import com.sendbird.android.SendBird
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.broadcastIn
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.File

private const val PAGE_SIZE = 70

private const val CHANNEL_HANDLER_IDENTIFIER =
    "com.heckfyxe.chatty.ui.message.CHANNEL_HANDLER_IDENTIFIER"

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

    suspend fun sendTextMessage(scope: CoroutineScope, text: String): ReceiveChannel<Message> =
        sendMessage(scope, sendBirdApi.sendMessage(channelId, text))

    suspend fun sendImageMessage(scope: CoroutineScope, file: File): ReceiveChannel<Message> =
        sendMessage(scope, sendBirdApi.sendMessage(channelId, file))

    @UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private suspend fun sendMessage(
        scope: CoroutineScope,
        flow: Flow<RoomMessage>
    ): Channel<Message> {
        val result = Channel<Message>(2)
        scope.launch {
            try {
                val channel = flow.broadcastIn(scope).openSubscription()
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
        return result
    }

    suspend fun launchChannelHandler(handler: SendBird.ChannelHandler) {
        sendBirdApi.addChannelHandler(CHANNEL_HANDLER_IDENTIFIER, handler)
    }

    fun stopChannelHandler() {
        sendBirdApi.removeChannelHandler(CHANNEL_HANDLER_IDENTIFIER)
    }

    suspend fun startTyping() = sendBirdApi.startTyping(channelId)

    suspend fun endTyping() = sendBirdApi.endTyping(channelId)
}
