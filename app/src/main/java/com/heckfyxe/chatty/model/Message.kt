package com.heckfyxe.chatty.model

import java.io.Serializable

data class Message(
    var id: String,
    var time: Long,
    var interlocutor: User,
    var text: String
) : Serializable