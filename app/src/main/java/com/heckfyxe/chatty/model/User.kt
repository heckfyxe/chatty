package com.heckfyxe.chatty.model

import androidx.room.ColumnInfo
import java.io.Serializable

data class User(
    var id: String,
    var name: String,
    @ColumnInfo(name = "avatar_url") var avatarUrl: String
) : Serializable