package com.heckfyxe.chatty.util.sendbird

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