package com.heckfyxe.chatty.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.heckfyxe.chatty.model.User

fun ImageView.loadCircleUserAvatar(user: User) {
    Glide.with(this)
        .load(user.avatar)
        .circleCrop()
        .into(this)
}