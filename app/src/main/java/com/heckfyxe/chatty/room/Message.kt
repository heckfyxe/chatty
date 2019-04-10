package com.heckfyxe.chatty.room

import androidx.room.Entity

@Entity(primaryKeys = ["id", "dialogId"])
data class Message(
    var id: Long,
    var dialogId: String,
    var time: Long,
    var senderId: String,
    var text: String
)