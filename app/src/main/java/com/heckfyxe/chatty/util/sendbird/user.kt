package com.heckfyxe.chatty.util.sendbird

import com.heckfyxe.chatty.room.RoomUser
import com.sendbird.android.Sender
import com.sendbird.android.User

fun User.toRoomUser() = RoomUser(userId, nickname, profileUrl)

fun Sender.toDomain() = com.heckfyxe.chatty.model.User(userId, nickname, profileUrl)

fun User.toDomain() = com.heckfyxe.chatty.model.User(userId, nickname, profileUrl)