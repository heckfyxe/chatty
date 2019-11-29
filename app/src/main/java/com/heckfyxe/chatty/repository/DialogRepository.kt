package com.heckfyxe.chatty.repository

import androidx.lifecycle.MutableLiveData
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.model.Dialog
import com.heckfyxe.chatty.remote.SendBirdApi
import com.heckfyxe.chatty.room.*
import com.heckfyxe.chatty.util.sendbird.getInterlocutor
import com.heckfyxe.chatty.util.sendbird.toRoomDialog
import com.heckfyxe.chatty.util.sendbird.toRoomMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject

class DialogRepository : KoinComponent {

    private val sendBirdApi: SendBirdApi by inject()

    private val database: AppDatabase by inject()
    private val dialogDao: DialogDao by inject()
    private val messageDao: MessageDao by inject()
    private val userDao: UserDao by inject()

    private val userId: String by inject(KOIN_USER_ID)
    private val auth: FirebaseAuth by inject()

    val currentUser = MutableLiveData<RoomUser>()
    val errors = MutableLiveData<Exception?>()
    val chats = dialogDao.getDialogsLiveData()

    suspend fun connectUser() {
        try {
            currentUser.postValue(sendBirdApi.connect(userId))
        } catch (e: Exception) {
            errors.postValue(e)
        }
    }

    suspend fun refresh() {
        try {
            for (channels in sendBirdApi.getChannels()) {
                val users = ArrayList<RoomUser>(channels.size)
                val messages = ArrayList<RoomMessage>(channels.size)
                val dialogs = ArrayList<RoomDialog>(channels.size)

                channels.forEach { channel ->
                    messages.add(channel.lastMessage.toRoomMessage())
                    users.add(channel.getInterlocutor().toRoomUser())
                    dialogs.add(channel.toRoomDialog())
                }
                database.withTransaction {
                    dialogDao.insert(dialogs)
                    userDao.insert(users.toList())
                    messageDao.insert(messages)
                }
            }
        } catch (e: Exception) {
            errors.postValue(e)
        }
    }

    suspend fun insertDialog(dialog: Dialog) = withContext(Dispatchers.IO) {
        database.withTransaction {
            userDao.insert(dialog.interlocutor.toRoomUser())
            messageDao.insert(dialog.lastMessage.run {
                RoomMessage(id, dialog.id, time, sender, text, out, sent, requestId)
            })
            dialogDao.insert(dialog.toRoomDialog())
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