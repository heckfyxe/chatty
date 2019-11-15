package com.heckfyxe.chatty.model

import java.io.Serializable

data class Dialog(
    var id: String,
    var name: String,
    var image: String,
    var lastMessageText: String,
    var lastMessageTime: Long,
    var unreadCount: Int
) : Serializable