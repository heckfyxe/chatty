package com.heckfyxe.chatty.util.sendbird

import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.model.MessageType
import com.heckfyxe.chatty.model.RequestState
import com.heckfyxe.chatty.room.RoomMessage
import com.sendbird.android.BaseMessage
import com.sendbird.android.FileMessage
import com.sendbird.android.Sender
import com.sendbird.android.UserMessage
import org.koin.core.context.GlobalContext.get

private val koin = get().koin
private val currentUserId: String by koin.inject(KOIN_USER_ID)

val BaseMessage.sender: Sender
    get() = when (this) {
        is UserMessage -> sender
        is FileMessage -> sender
        else -> throw Exception("Unknown message type")
    }

val BaseMessage.text: String?
    get() = when (this) {
        is UserMessage -> message
        is FileMessage -> null
        else -> null
    }

val BaseMessage.file: String?
    get() = when (this) {
        is UserMessage -> null
        is FileMessage -> url
        else -> null
    }

val BaseMessage.type: MessageType
    get() = when (this) {
        is UserMessage -> MessageType.TEXT
        is FileMessage -> MessageType.IMAGE
        else -> throw Exception("Unknown message type")
    }

val BaseMessage.requestId: String
    get() = when (this) {
        is UserMessage -> requestId
        is FileMessage -> requestId
        else -> throw Exception("Unknown message type")
    }

fun UserMessage.getDomainRequestState(): RequestState = when (this.requestState) {
    UserMessage.RequestState.NONE -> RequestState.NONE
    UserMessage.RequestState.PENDING -> RequestState.PENDING
    UserMessage.RequestState.FAILED -> RequestState.FAILED
    UserMessage.RequestState.SUCCEEDED -> RequestState.SUCCESS
    null -> RequestState.NONE
}

fun FileMessage.getDomainRequestState(): RequestState = when (this.requestState) {
    FileMessage.RequestState.NONE -> RequestState.NONE
    FileMessage.RequestState.PENDING -> RequestState.PENDING
    FileMessage.RequestState.FAILED -> RequestState.FAILED
    FileMessage.RequestState.SUCCEEDED -> RequestState.SUCCESS
    null -> RequestState.NONE
}

val BaseMessage.requestState: RequestState
    get() = when (this) {
        is UserMessage -> getDomainRequestState()
        is FileMessage -> getDomainRequestState()
        else -> throw Exception("Unknown message type")
    }

fun BaseMessage.isSentByMe() = sender.userId == currentUserId

fun BaseMessage.isSent() = requestState == RequestState.SUCCESS

fun BaseMessage.toRoomMessage(): RoomMessage = when (this) {
    is UserMessage -> toRoomMessage()
    is FileMessage -> toRoomMessage()
    else -> throw Exception("Unknown message type")
}

fun UserMessage.toRoomMessage() = RoomMessage(
    messageId,
    channelUrl,
    createdAt,
    sender.toDomain(),
    message,
    null,
    MessageType.TEXT,
    isSentByMe(),
    isSent(),
    requestId
)

fun FileMessage.toRoomMessage() = RoomMessage(
    messageId,
    channelUrl,
    createdAt,
    sender.toDomain(),
    null,
    url,
    MessageType.IMAGE,
    isSentByMe(),
    isSent(),
    requestId
)

