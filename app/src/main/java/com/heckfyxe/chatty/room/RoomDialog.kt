package com.heckfyxe.chatty.room

import androidx.room.*
import com.heckfyxe.chatty.model.Dialog
import com.heckfyxe.chatty.model.Message
import com.heckfyxe.chatty.model.User
import com.heckfyxe.chatty.util.sendbird.*
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
    @Embedded(prefix = "interlocutor_") var interlocutor: User?,
    @Embedded(prefix = "last_message_") var lastMessage: Message?,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "notification_id") var notificationId: Int = 0
)

fun RoomDialog.toDomain(): Dialog =
    Dialog(
        id,
        name,
        photoUrl,
        interlocutor?.run { User(id, name, avatarUrl) },
        lastMessage,
        unreadCount
    )

fun List<RoomDialog>.toDomain(): List<Dialog> = map {
    it.toDomain()
}

fun BaseMessage.toDomain() = Message(
    messageId,
    createdAt,
    sender.toDomain(),
    text,
    file,
    type,
    isSentByMe(),
    isSent(),
    requestId
)

fun Member.toDomain() = User(userId, nickname, profileUrl)

fun Dialog.toRoomDialog() = RoomDialog(id, name, unreadCount, image, interlocutor, lastMessage)


