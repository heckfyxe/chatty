package com.heckfyxe.chatty.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Message(
    @PrimaryKey
    var id: Long,
    var time: Long,
    var senderId: String,
    var text: String
)