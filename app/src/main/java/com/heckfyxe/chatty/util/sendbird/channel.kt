package com.heckfyxe.chatty.util.sendbird

import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.model.Dialog
import com.heckfyxe.chatty.room.RoomDialog
import com.heckfyxe.chatty.room.toDomain
import com.sendbird.android.GroupChannel
import com.sendbird.android.Member
import org.koin.core.context.GlobalContext.get

private val koin = get().koin
private val userId: String by koin.inject(KOIN_USER_ID)

fun List<Member>.getInterlocutor(): Member? {
    if (this.size > 2) {
        throw Exception("Members count must be <= 2!")
    }
    return this.singleOrNull { it.userId != userId }
}

fun GroupChannel.getInterlocutor(): Member? = members.getInterlocutor()

fun GroupChannel.toRoomDialog(): RoomDialog {
    val interlocutor = getInterlocutor()
    return RoomDialog(
        url,
        interlocutor?.nickname ?: "Deleted",
        unreadMessageCount,
        interlocutor?.profileUrl ?: "",
        interlocutor?.toDomain(),
        lastMessage.toDomain()
    )
}

fun GroupChannel.toDomain(): Dialog {
    val interlocutor = getInterlocutor()
    return Dialog(
        url,
        interlocutor?.nickname ?: "Deleted",
        interlocutor?.profileUrl ?: "",
        interlocutor?.toDomain(),
        lastMessage.toDomain(),
        unreadMessageCount
    )
}