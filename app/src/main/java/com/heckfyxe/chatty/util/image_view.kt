package com.heckfyxe.chatty.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.heckfyxe.chatty.model.ChatUser

fun ImageView.loadCircleUserAvatar(user: ChatUser) {
    Glide.with(this)
        .load(user.avatar)
        .circleCrop()
        .into(this)
}