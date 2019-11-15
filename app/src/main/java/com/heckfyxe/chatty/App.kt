package com.heckfyxe.chatty

import androidx.multidex.MultiDexApplication
import androidx.room.withTransaction
import com.google.firebase.FirebaseApp
import com.heckfyxe.chatty.koin.koinModule
import com.heckfyxe.chatty.room.AppDatabase
import com.heckfyxe.chatty.room.DialogDao
import com.heckfyxe.chatty.room.MessageDao
import com.heckfyxe.chatty.room.UserDao
import com.heckfyxe.chatty.util.sendbird.getInterlocutor
import com.heckfyxe.chatty.util.sendbird.toRoomDialog
import com.heckfyxe.chatty.util.sendbird.toRoomMessage
import com.heckfyxe.chatty.util.sendbird.toRoomUser
import com.sendbird.android.BaseChannel
import com.sendbird.android.BaseMessage
import com.sendbird.android.GroupChannel
import com.sendbird.android.SendBird
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : MultiDexApplication() {

    companion object {
        private const val CHANNEL_HANDLER_IDENTIFIER = "com.heckfyxe.chatty.CHANNEL_HANDLER_IDENTIFIER"
    }

    private val database: AppDatabase by inject()
    private val dialogDao: DialogDao by inject()
    private val messageDao: MessageDao by inject()
    private val userDao: UserDao by inject()

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    private val channelHandler = object : SendBird.ChannelHandler() {
        override fun onMessageReceived(channel: BaseChannel, baseMessage: BaseMessage) {
            if (channel !is GroupChannel) return

            val interlocutor = channel.getInterlocutor().toRoomUser()
            val dialog = channel.toRoomDialog()

            val message = baseMessage.toRoomMessage()

            scope.launch {
                database.withTransaction {
                    userDao.insert(interlocutor)
                    messageDao.insert(message)
                    dialogDao.insert(dialog)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            modules(koinModule)
        }
        SendBird.init("CCC955EE-E041-483F-9866-590B1C4B1E30", this)
        FirebaseApp.initializeApp(this)

        SendBird.addChannelHandler(CHANNEL_HANDLER_IDENTIFIER, channelHandler)
    }

    override fun onTerminate() {
        super.onTerminate()

        SendBird.removeChannelHandler(CHANNEL_HANDLER_IDENTIFIER)
        job.cancel()
    }
}