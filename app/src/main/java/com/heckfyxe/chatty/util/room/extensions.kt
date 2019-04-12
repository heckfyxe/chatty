package com.heckfyxe.chatty.util.room

import com.heckfyxe.chatty.model.ChatUser
import com.heckfyxe.chatty.room.User

fun User.toChatUser() = ChatUser(id, name, avatarUrl)
