package com.heckfyxe.chatty.room

import androidx.room.*
import com.heckfyxe.chatty.model.Message
import com.heckfyxe.chatty.model.User

@Entity(
    tableName = "message",
    primaryKeys = ["id"],
    indices = [
        Index(value = ["time"], unique = true)
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
    var text: String,
    var out: Boolean,
    var sent: Boolean,
    @ColumnInfo(name = "request_id") var requestId: String
)

fun RoomMessage.toDomain(): Message = Message(id, time, sender, text, out, sent)