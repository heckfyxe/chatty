package com.heckfyxe.chatty.repository

import androidx.lifecycle.MutableLiveData
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.room.*
import com.heckfyxe.chatty.util.sendbird.getSender
import com.heckfyxe.chatty.util.sendbird.getText
import com.sendbird.android.GroupChannel
import com.sendbird.android.SendBird
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class DialogListRepository : KoinComponent {

    private val database: AppDatabase by inject()
    private val dialogDao: DialogDao by inject()
    private val messageDao: MessageDao by inject()
    private val userDao: UserDao by inject()

    private val userId: String by inject(KOIN_USER_ID)
    private val auth: FirebaseAuth by inject()

    val currentUser = MutableLiveData<com.sendbird.android.User>()
    val errors = MutableLiveData<Exception>()
    val chats = dialogDao.getDialogsLiveData()

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    fun connectUser() {
        SendBird.connect(userId) { sendBirdUser, e ->
            if (e != null) {
                errors.postValue(e)
                return@connect
            }

            currentUser.postValue(sendBirdUser)
        }
    }

    fun getMessageById(id: Long): Message = messageDao.getMessageById(id)

    fun getUserById(id: String): com.heckfyxe.chatty.room.User = userDao.getUserById(id)

    fun refresh() {
        GroupChannel.createMyGroupChannelListQuery().next { channels, e ->
            if (e != null) {
                errors.postValue(e)
                return@next
            }

            val users = ArrayList<User>(channels.size)
            val messages = ArrayList<Message>(channels.size)
            val dialogs = ArrayList<Dialog>(channels.size)

            channels.forEach { channel ->
                channel.lastMessage.apply {
                    users.add(with(getSender()) {
                        User(userId, nickname, profileUrl)
                    })
                    messages.add(Message(messageId, createdAt, getSender().userId, getText()))
                }
                with(channel) {
                    val consider = members.single { it.userId != userId }
                    dialogs.add(
                        Dialog(
                            url,
                            lastMessage.messageId,
                            consider.nickname,
                            unreadMessageCount,
                            consider.profileUrl
                        )
                    )
                }
            }

            scope.launch {
                database.withTransaction {
                    userDao.insert(users)
                    messageDao.insert(messages)
                    dialogDao.insert(dialogs)
                }
            }
        }
    }

    fun logOut(action: () -> Unit) {
        SendBird.disconnect {
            auth.signOut()
            action()
        }
    }

    fun clear() {
        job.cancel()
    }
}