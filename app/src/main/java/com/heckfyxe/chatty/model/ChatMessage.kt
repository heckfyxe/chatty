package com.heckfyxe.chatty.model

import com.heckfyxe.chatty.util.sendbird.getSender
import com.heckfyxe.chatty.util.sendbird.getText
import com.sendbird.android.BaseMessage
import com.stfalcon.chatkit.commons.models.IMessage
import com.stfalcon.chatkit.commons.models.IUser
import java.util.*

class ChatMessage : IMessage {

    private val _id: Long
    private val _createdAt: Date
    private val _user: ChatUser
    private val _text: String

    constructor(id: Long, createdAt: Date, user: ChatUser, text: String) {
        _id = id
        _createdAt = createdAt
        _user = user
        _text = text
    }

    constructor(message: BaseMessage) :
            this(message.messageId, Date(message.createdAt), ChatUser(message.getSender()), message.getText())

    override fun getId(): String = _id.toString()

    override fun getCreatedAt(): Date = _createdAt

    override fun getUser(): IUser = _user

    override fun getText(): String = _text
}