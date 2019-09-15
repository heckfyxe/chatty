package com.heckfyxe.chatty.util.sendbird

import android.content.Context
import com.sendbird.android.BaseChannel
import org.koin.core.context.GlobalContext.get
import java.io.File

private val koin = get().koin
private val context: Context by koin.inject()

fun BaseChannel.saveOnDevice() {
    val file = File(context.filesDir, url)
    file.writeBytes(serialize())
}

fun channelBytesFromDevice(channelId: String): ByteArray {
    val file = File(context.filesDir, channelId)
    return file.readBytes()
}

fun channelFromDevice(channelId: String): BaseChannel {
    val bytes = channelBytesFromDevice(channelId)
    return BaseChannel.buildFromSerializedData(bytes)
}