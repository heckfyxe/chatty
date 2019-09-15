package com.heckfyxe.chatty.room

import androidx.room.Entity
import androidx.room.Index

@Entity(
    primaryKeys = ["id", "dialogId", "requestId"],
    indices = [
        Index("id", unique = true),
        Index("time", unique = true)
    ]
)
data class Message(
    var id: Long,
    var dialogId: String,
    var time: Long,
    var senderId: String,
    var text: String,
    var out: Boolean,
    var sent: Boolean,
    var requestId: String
)