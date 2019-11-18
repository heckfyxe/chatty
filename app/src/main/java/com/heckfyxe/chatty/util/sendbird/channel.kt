package com.heckfyxe.chatty.util.sendbird

import android.content.Context
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.room.RoomDialog
import com.heckfyxe.chatty.room.toDomain
import com.sendbird.android.BaseChannel
import com.sendbird.android.GroupChannel
import com.sendbird.android.Member
import org.koin.core.context.GlobalContext.get
import java.io.File

private val koin = get().koin
private val context: Context by koin.inject()
private val userId: String by koin.inject(KOIN_USER_ID)

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

fun List<Member>.getInterlocutor(): Member {
    if (this.size > 2) {
        throw Exception("Members count must be 2!")
    }
    return this.single { it.userId != userId }
}

fun GroupChannel.getInterlocutor(): Member = members.getInterlocutor()

fun GroupChannel.toRoomDialog(): RoomDialog {
    val interlocutor = getInterlocutor()
    return RoomDialog(
        url,
        interlocutor.nickname,
        unreadMessageCount,
        interlocutor.profileUrl,
        interlocutor.toDomain(),
        lastMessage.toDomain()
    )
}