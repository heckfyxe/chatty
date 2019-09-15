package com.heckfyxe.chatty.util.sendbird

import com.heckfyxe.chatty.room.Message
import com.sendbird.android.BaseMessage
import com.sendbird.android.FileMessage
import com.sendbird.android.Sender
import com.sendbird.android.UserMessage

fun BaseMessage.getSender(): Sender =
    when (this) {
        is UserMessage -> sender
        is FileMessage -> sender
        else -> throw Exception("Unknown message type")
    }

fun BaseMessage.getText(): String = when (this) {
    is UserMessage -> message
    is FileMessage -> name
    else -> ""
}

fun BaseMessage.getRequestId(): String = when (this) {
    is UserMessage -> requestId
    is FileMessage -> requestId
    else -> ""
}

fun BaseMessage.getDialogId(): String = when (this) {
    is UserMessage -> channelUrl
    is FileMessage -> channelUrl
    else -> ""
}

fun BaseMessage.toMessage(userId: String, isSent: Boolean = true): Message {
    val senderId = getSender().userId
    return Message(
        messageId,
        getDialogId(),
        createdAt,
        senderId,
        getText(),
        senderId == userId,
        isSent,
        getRequestId()
    )
}

fun UserMessage.toMessage(out: Boolean, sent: Boolean = true) =
    Message(
        messageId,
        channelUrl,
        createdAt,
        sender.userId,
        message,
        out,
        sent,
        requestId
    )


