package com.heckfyxe.chatty.repository

import androidx.lifecycle.MutableLiveData
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.remote.SendBirdApi
import com.heckfyxe.chatty.room.*
import com.heckfyxe.chatty.util.sendbird.getInterlocutor
import com.heckfyxe.chatty.util.sendbird.toMessage
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

    val currentUser = MutableLiveData<User>()
    val errors = MutableLiveData<Exception?>()
    val chats = dialogDao.getDialogsLiveData()

    suspend fun connectUser() {
        try {
            currentUser.postValue(sendBirdApi.connect(userId))
        } catch (e: Exception) {
            errors.postValue(e)
        }
    }

    suspend fun getInterlocutor(dialogId: String): User = dialogDao.getInterlocutor(dialogId)

    suspend fun getMessageById(id: Long): Message? = messageDao.getMessageById(id)

    suspend fun getUserById(id: String): User = userDao.getUserById(id)!!

    suspend fun refresh() {
        try {
            for (channels in sendBirdApi.getChannels()) {
                val users = mutableSetOf<User>()
                val messages = ArrayList<Message>(channels.size)
                val dialogs = ArrayList<Dialog>(channels.size)

                channels.forEach { channel ->
                    channel.lastMessage.let {
                        messages.add(it.toMessage(userId))
                    }

                    users.addAll(channel.members.map {
                        User(it.userId, it.nickname, it.profileUrl)
                    })

                    val interlocutor = channel.getInterlocutor()

                    dialogs.add(
                        Dialog(
                            channel.url, channel.lastMessage.messageId, interlocutor.nickname,
                            channel.unreadMessageCount, interlocutor.profileUrl, interlocutor.userId
                        )
                    )
                }
                database.withTransaction {
                    userDao.insert(users.toList())
                    messageDao.insert(messages)
                    dialogDao.insert(dialogs)
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