package com.heckfyxe.chatty.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["id"], unique = true)])
data class Dialog(
    var id: String,
    var lastMessageId: Long,
    var name: String,
    var unreadCount: Int,
    var photoUrl: String,
    var interlocutorId: String,
    @PrimaryKey(autoGenerate = true)
    var notificationId: Int = 0
)