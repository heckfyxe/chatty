package com.heckfyxe.chatty.room

import androidx.room.Entity

@Entity(primaryKeys = ["id", "dialogId", "requestId"])
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