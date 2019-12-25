package com.heckfyxe.chatty.room

import androidx.room.*
import com.heckfyxe.chatty.model.Message
import com.heckfyxe.chatty.model.MessageType
import com.heckfyxe.chatty.model.User

@Entity(
    tableName = "message",
    primaryKeys = ["id"],
    indices = [
        Index(value = ["time"], unique = true),
        Index(value = ["dialog_id"], unique = false)
    ],
    foreignKeys = [
        ForeignKey(
            entity = RoomDialog::class,
            parentColumns = ["id"],
            childColumns = ["dialog_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RoomMessage(
    var id: Long,
    @ColumnInfo(name = "dialog_id") var dialogId: String,
    var time: Long,
    @Embedded(prefix = "sender_") var sender: User,
    var text: String?,
    var file: String?,
    var type: MessageType,
    var out: Boolean,
    var sent: Boolean,
    @ColumnInfo(name = "request_id") var requestId: String
)

fun RoomMessage.toDomain(): Message =
    Message(id, time, sender, text, file, type, out, sent, requestId)

fun List<RoomMessage>.toDomain() = map {
    it.toDomain()
}

fun Message.toRoomMessage(dialogId: String): RoomMessage = RoomMessage(
    id,
    dialogId,
    time,
    sender,
    text,
    file,
    type,
    out,
    sent,
    requestId
)