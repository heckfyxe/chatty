package com.heckfyxe.chatty.util.room

import android.util.Base64
import androidx.room.TypeConverter
import com.sendbird.android.BaseChannel
import com.sendbird.android.GroupChannel

object Converters {

    @TypeConverter
    @JvmStatic
    fun convertChannelToString(channel: GroupChannel): String {
        val bytes = channel.serialize()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    @TypeConverter
    @JvmStatic
    fun getChannelFromString(strBytes: String): GroupChannel {
        val bytes = Base64.decode(strBytes, Base64.DEFAULT)
        return BaseChannel.buildFromSerializedData(bytes) as GroupChannel
    }
}