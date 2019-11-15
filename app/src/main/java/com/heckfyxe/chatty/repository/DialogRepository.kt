package com.heckfyxe.chatty.repository

import androidx.lifecycle.MutableLiveData
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.remote.SendBirdApi
import com.heckfyxe.chatty.room.*
import com.heckfyxe.chatty.util.sendbird.getInterlocutor
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

    suspend fun getInterlocutor(dialogId: String): RoomUser = dialogDao.getInterlocutor(dialogId)

    suspend fun getMessageById(id: Long): RoomMessage? = messageDao.getMessageById(id)

    suspend fun getLastMessageId(dialogId: String) =
        messageDao.getLastMessage(dialogId).id

    suspend fun getUserById(id: String): RoomUser = userDao.getUserById(id)!!

    suspend fun refresh() {
        try {
            for (channels in sendBirdApi.getChannels()) {
                val users = ArrayList<RoomUser>(channels.size)
                val messages = ArrayList<RoomMessage>(channels.size)
                val dialogs = ArrayList<RoomDialog>(channels.size)

                channels.forEach { channel ->
                    messages.add(channel.lastMessage.toRoomMessage())

                    val interlocutor = channel.getInterlocutor()
                    users.add(interlocutor.toRoomUser())

                    dialogs.add(
                        RoomDialog(
                            channel.url,
                            interlocutor.nickname,
                            channel.unreadMessageCount,
                            interlocutor.profileUrl,
                            interlocutor.toInterlocutor(),
                            channel.lastMessage.toLastMessage()
                        )
                    )
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

    suspend fun logOut(action: () -> Unit) = withContext(Dispatchers.IO) {
        sendBirdApi.disconnect()
        auth.signOut()
        database.clearAllTables()
        withContext(Dispatchers.Main) {
            action()
        }
    }
}