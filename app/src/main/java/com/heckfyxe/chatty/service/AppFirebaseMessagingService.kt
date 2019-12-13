package com.heckfyxe.chatty.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.heckfyxe.chatty.MainActivity
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.remote.SendBirdApi
import com.heckfyxe.chatty.room.DialogDao
import com.sendbird.android.shadow.com.google.gson.JsonObject
import com.sendbird.android.shadow.com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject


class AppFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

    private val firebaseAuth: FirebaseAuth by inject()

    private val dialogDao: DialogDao by inject()
    private val sendBirdApi: SendBirdApi by inject()

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch {
            firebaseAuth.currentUser ?: return@launch
            try {
                sendBirdApi.registerPushNotifications(token)
            } catch (e: Exception) {
                Log.e("MessagingService", "register push notifications error", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val message = remoteMessage.data["message"]
        val payload = JsonParser().parse(remoteMessage.data["sendbird"]).asJsonObject
        scope.launch {
            sendNotification(message, payload)
        }
    }

    private fun sendNotification(message: String?, payload: JsonObject) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val channel = payload["channel"].asJsonObject
        val sender = payload["sender"].asJsonObject
        val channelId = channel["channel_url"].asString
        val senderAvatar = sender["profile_url"].asString
        val avatarDrawable = Glide.with(this)
            .load(senderAvatar)
            .priority(Priority.IMMEDIATE)
            .submit()
            .get()

        val notificationId = dialogDao.getNotificationIdByDialogId(channelId) ?: -1

        val notificationChannelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(avatarDrawable.toBitmap())
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                notificationChannelId,
                getString(R.string.message),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                enableLights(true)
                setSound(
                    defaultSoundUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(notificationChannel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    override fun onDestroy() {
        super.onDestroy()

        job.cancel()
    }
}