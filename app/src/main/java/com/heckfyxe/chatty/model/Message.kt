package com.heckfyxe.chatty.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import java.io.Serializable

data class Message(
    var id: Long,
    var time: Long,
    @Embedded(prefix = "sender_") var sender: User,
    var text: String,
    var out: Boolean,
    var sent: Boolean,
    @ColumnInfo(name = "request_id") var requestId: String
) : Serializable