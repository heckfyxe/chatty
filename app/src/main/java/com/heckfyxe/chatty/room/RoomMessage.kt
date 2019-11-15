package com.heckfyxe.chatty.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "message",
    primaryKeys = ["id"],
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
    @ColumnInfo(name = "sender_id") var senderId: String,
    var text: String,
    var out: Boolean,
    var sent: Boolean,
    @ColumnInfo(name = "request_id") var requestId: String
)