package com.heckfyxe.chatty.repository

import androidx.lifecycle.LiveData
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import com.heckfyxe.chatty.koin.deleteUserScope
import com.heckfyxe.chatty.koin.userScope
import com.heckfyxe.chatty.model.Dialog
import com.heckfyxe.chatty.model.Message
import com.heckfyxe.chatty.remote.SendBirdApi
import com.heckfyxe.chatty.room.*
import com.heckfyxe.chatty.util.sendbird.getInterlocutor
import com.heckfyxe.chatty.util.sendbird.toRoomDialog
import com.heckfyxe.chatty.util.sendbird.toRoomMessage
import com.sendbird.android.GroupChannel
import com.sendbird.android.SendBird
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.koin.core.KoinComponent
import org.koin.core.inject
import kotlin.coroutines.resume

private const val CHANNEL_HANDLER_IDENTIFIER =
    "com.heckfyxe.chatty.ui.main.CHANNEL_HANDLER_IDENTIFIER"

class DialogRepository : KoinComponent {

    private val sendBirdApi: SendBirdApi by userScope.inject()

    private val database: AppDatabase by inject()
    private val dialogDao: DialogDao by inject()
    private val messageDao: MessageDao by inject()
    private val userDao: UserDao by inject()

    private val auth: FirebaseAuth by inject()

    val chats: LiveData<List<RoomDialog>> by lazy { dialogDao.getDialogsLiveData() }

    @OptIn(InternalCoroutinesApi::class)
    suspend fun refresh() {
        sendBirdApi.getChannels().collect { channels: List<GroupChannel> ->
            val users = ArrayList<RoomUser>(channels.size)
            val messages = ArrayList<RoomMessage>(channels.size)
            val dialogs = ArrayList<RoomDialog>(channels.size)

            channels.forEach { channel ->
                messages.add(channel.lastMessage.toRoomMessage())

                channel.getInterlocutor()?.let {
                    users.add(it.toRoomUser())
                }
                dialogs.add(channel.toRoomDialog())
            }
            database.withTransaction {
                dialogDao.insert(dialogs)
                userDao.insert(users.toList())
                messageDao.insert(messages)
            }
        }
    }

    suspend fun insertDialog(dialog: Dialog) = withContext(Dispatchers.IO) {
        database.withTransaction {
            dialogDao.insert(dialog.toRoomDialog())
            dialog.interlocutor?.toRoomUser()?.let {
                userDao.insert(it)
            }
            dialog.lastMessage?.toRoomMessage(dialog.id)?.let {
                messageDao.insert(it)
            }
        }
    }

    suspend fun createChannel(interlocutorId: String): String = withContext(Dispatchers.IO) {
        val channel = sendBirdApi.createChannel(interlocutorId)
        dialogDao.insert(channel.toRoomDialog())
        channel.url
    }

    suspend fun getDialogIdByInterlocutorId(interlocutorId: String): String? =
        withContext(Dispatchers.IO) {
            var id = dialogDao.getDialogIdByInterlocutorId(interlocutorId)
            if (id != null) {
                return@withContext id
            }
            id = createChannel(interlocutorId)
            return@withContext id
        }

    suspend fun insertMessage(dialogId: String, message: Message) = withContext(Dispatchers.IO) {
        dialogDao.updateLastMessage(dialogId, message)
    }

    suspend fun getDialogById(id: String): Dialog? = withContext(Dispatchers.IO) {
        dialogDao.getDialogById(id)?.toDomain()
    }

    suspend fun launchChannelHandler(handler: SendBird.ChannelHandler) {
        sendBirdApi.addChannelHandler(CHANNEL_HANDLER_IDENTIFIER, handler)
    }

    fun stopChannelHandler() {
        sendBirdApi.removeChannelHandler(CHANNEL_HANDLER_IDENTIFIER)
    }

    suspend fun registerPushNotifications() = coroutineScope {
        suspendCancellableCoroutine<Unit> { cont ->
            FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
                if (!it.isSuccessful) {
                    cont.cancel(it.exception)
                    return@addOnCompleteListener
                }
                launch {
                    try {
                        sendBirdApi.registerPushNotifications(it.result!!.token)
                        cont.resume(Unit)
                    } catch (e: Exception) {
                        cont.cancel(e)
                    }
                }
            }
        }
    }

    suspend fun signOut(action: () -> Unit) = withContext(Dispatchers.IO) {
        stopChannelHandler()
        sendBirdApi.signOut()
        deleteUserScope()
        auth.signOut()
        database.clearAllTables()
        withContext(Dispatchers.Main) {
            action()
        }
    }
}