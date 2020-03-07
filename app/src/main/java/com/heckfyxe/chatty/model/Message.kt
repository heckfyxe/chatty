package com.heckfyxe.chatty.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.heckfyxe.chatty.ui.message.MessageListItem
import java.io.Serializable

data class Message(
    var id: Long,
    override var time: Long,
    @Embedded(prefix = "sender_") var sender: User?,
    var text: String?,
    var file: String?,
    var type: MessageType?,
    var out: Boolean,
    var sent: Boolean,
    @ColumnInfo(name = "request_id") var requestId: String?
) : Serializable, MessageListItem

enum class MessageType {
    TEXT, IMAGE;
}

enum class RequestState {
    NONE, PENDING, FAILED, SUCCESS
}