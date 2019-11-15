package com.heckfyxe.chatty.room

import androidx.room.*
import com.heckfyxe.chatty.model.Dialog
import com.heckfyxe.chatty.util.sendbird.getText
import com.sendbird.android.BaseMessage
import com.sendbird.android.Member

@Entity(
    tableName = "dialog",
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["last_message_id"], unique = true),
        Index(value = ["interlocutor_id"], unique = true)
    ]
)
data class RoomDialog(
    var id: String,
    var name: String,
    @ColumnInfo(name = "unread_count") var unreadCount: Int,
    @ColumnInfo(name = "photo_url") var photoUrl: String,
    @Embedded(prefix = "interlocutor_") var interlocutor: Interlocutor,
    @Embedded(prefix = "last_message_") var lastMessage: LastMessage,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "notification_id") var notificationId: Int = 0
)

data class LastMessage(
    var id: Long,
    var text: String,
    var time: Long
)

data class Interlocutor(
    var id: String,
    var name: String,
    var imageUrl: String
)

fun RoomDialog.toDomain(): Dialog =
    Dialog(id, name, photoUrl, lastMessage.id, lastMessage.text, lastMessage.time, unreadCount)

fun BaseMessage.toLastMessage() = LastMessage(messageId, getText(), createdAt)

fun Member.toInterlocutor() = Interlocutor(userId, nickname, profileUrl)

