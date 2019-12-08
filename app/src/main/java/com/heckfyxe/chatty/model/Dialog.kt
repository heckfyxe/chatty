package com.heckfyxe.chatty.model

import java.io.Serializable

data class Dialog(
    var id: String,
    var name: String,
    var image: String,
    var interlocutor: User?,
    var lastMessage: Message?,
    var unreadCount: Int
) : Serializable