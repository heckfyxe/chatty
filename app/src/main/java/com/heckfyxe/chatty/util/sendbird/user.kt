package com.heckfyxe.chatty.util.sendbird

import com.sendbird.android.User

fun User.toRoomUser() = com.heckfyxe.chatty.room.RoomUser(userId, nickname, profileUrl)