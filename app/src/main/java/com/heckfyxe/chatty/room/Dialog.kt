package com.heckfyxe.chatty.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Dialog(
    @PrimaryKey
    var id: String,
    var lastMessageId: Long,
    var name: String,
    var unreadCount: Int,
    var photoUrl: String
)