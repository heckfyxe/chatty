package com.heckfyxe.chatty.model

import com.sendbird.android.AdminMessage
import com.sendbird.android.BaseMessage
import com.sendbird.android.FileMessage
import com.sendbird.android.UserMessage
import com.stfalcon.chatkit.commons.models.IMessage
import com.stfalcon.chatkit.commons.models.IUser
import java.util.*

class Message(private val message: BaseMessage): IMessage {

    override fun getId(): String = message.messageId.toString()

    override fun getCreatedAt(): Date = Date(message.createdAt)

    override fun getUser(): IUser = when(message) {
        is UserMessage -> User(message.sender)
        is FileMessage -> User(message.sender)
        is AdminMessage -> User("admin", "admin", "admin")
        else -> throw Exception("Unknown message type")
    }

    override fun getText(): String = when (message) {
        is UserMessage -> message.message
        is FileMessage -> message.name
        else -> ""
    }
}