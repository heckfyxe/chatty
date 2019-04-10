package com.heckfyxe.chatty.util.sendbird

import android.content.Context
import com.sendbird.android.BaseChannel
import java.io.File

fun BaseChannel.saveOnDevice(context: Context) {
    val file = File(context.filesDir, url)
    file.writeBytes(serialize())
}

fun channelBytesFromDevice(context: Context, channelId: String): ByteArray {
    val file = File(context.filesDir, channelId)
    return file.readBytes()
}

fun channelFromDevice(context: Context, channelId: String): BaseChannel {
    val bytes = channelBytesFromDevice(context, channelId)
    return BaseChannel.buildFromSerializedData(bytes)
}