package com.heckfyxe.chatty.room

import androidx.room.*
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.model.Dialog
import com.heckfyxe.chatty.model.Message
import com.heckfyxe.chatty.model.User
import com.heckfyxe.chatty.util.sendbird.getSender
import com.heckfyxe.chatty.util.sendbird.getText
import com.heckfyxe.chatty.util.sendbird.toDomain
import com.sendbird.android.BaseMessage
import com.sendbird.android.Member
import org.koin.core.context.GlobalContext.get

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
    @Embedded(prefix = "interlocutor_") var interlocutor: User,
    @Embedded(prefix = "last_message_") var lastMessage: Message,
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "notification_id") var notificationId: Int = 0
)

private val koin = get().koin
private val currentUserId: String by koin.inject(KOIN_USER_ID)

fun RoomDialog.toDomain(): Dialog =
    Dialog(
        id,
        name,
        photoUrl,
        with(interlocutor) { User(id, name, avatarUrl) },
        lastMessage,
        unreadCount
    )

fun List<RoomDialog>.toDomain(): List<Dialog> = map {
    it.toDomain()
}

fun BaseMessage.toDomain() = Message(
    messageId,
    createdAt,
    getSender().toDomain(),
    getText(),
    getSender().userId == currentUserId,
    true,
    ""
)

fun Member.toDomain() = User(userId, nickname, profileUrl)

