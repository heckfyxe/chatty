package com.heckfyxe.chatty.room

import androidx.room.TypeConverter
import com.heckfyxe.chatty.model.MessageType

class Converters {
    @TypeConverter
    fun fromMessageType(type: MessageType?): String? = type?.toString()

    @TypeConverter
    fun toMessageType(type: String?): MessageType? {
        type ?: return null
        return MessageType.valueOf(type)
    }
}