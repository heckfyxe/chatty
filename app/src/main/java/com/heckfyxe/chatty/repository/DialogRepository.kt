package com.heckfyxe.chatty.repository

import androidx.lifecycle.LiveData
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import com.heckfyxe.chatty.model.Dialog
import com.heckfyxe.chatty.remote.SendBirdApi
import com.heckfyxe.chatty.room.*
import com.heckfyxe.chatty.util.sendbird.getInterlocutor
import com.heckfyxe.chatty.util.sendbird.toDomain
import com.heckfyxe.chatty.util.sendbird.toRoomDialog
import com.heckfyxe.chatty.util.sendbird.toRoomMessage
import com.sendbird.android.BaseChannel
import com.sendbird.android.BaseMessage
import com.sendbird.android.GroupChannel
import com.sendbird.android.SendBird
import kotlinx.coroutines.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import kotlin.coroutines.resume

private const val CHANNEL_HANDLER_IDENTIFIER =
    "com.heckfyxe.chatty.ui.main.CHANNEL_HANDLER_IDENTIFIER"

class DialogRepository : KoinComponent {

    private val sendBirdApi: SendBirdApi by inject()

    private val database: AppDatabase by inject()
    private val dialogDao: DialogDao by inject()
    private val messageDao: MessageDao by inject()
    private val userDao: UserDao by inject()

    private val auth: FirebaseAuth by inject()

    val chats: LiveData<List<RoomDialog>> by lazy { dialogDao.getDialogsLiveData() }

    suspend fun refresh() {
        for (channels in sendBirdApi.getChannels()) {
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
            dialog.interlocutor?.toRoomUser()?.let {
                userDao.insert(it)
            }
            dialog.lastMessage?.apply {
                RoomMessage(id, dialog.id, time, sender, text, out, sent, requestId).let {
                    messageDao.insert(it)
                }
            }
            dialogDao.insert(dialog.toRoomDialog())
        }
    }

    fun launchChannelHandler(scope: CoroutineScope) {
        sendBirdApi.addChannelHandler(
            CHANNEL_HANDLER_IDENTIFIER,
            object : SendBird.ChannelHandler() {
                override fun onMessageReceived(channel: BaseChannel, baseMessage: BaseMessage) {
                    if (channel !is GroupChannel) return

                    val dialog = channel.toDomain()

                    scope.launch {
                        insertDialog(dialog)
                    }
                }
            })
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

    suspend fun logOut(action: () -> Unit) = withContext(Dispatchers.IO) {
        sendBirdApi.disconnect()
        auth.signOut()
        database.clearAllTables()
        withContext(Dispatchers.Main) {
            action()
        }
    }
}