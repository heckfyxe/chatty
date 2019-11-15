package com.heckfyxe.chatty.model

import java.io.Serializable

data class Message(
    var id: Long,
    var time: Long,
    var interlocutor: User,
    var text: String,
    var out: Boolean,
    var sent: Boolean
) : Serializable