package com.heckfyxe.chatty.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class User(
    @PrimaryKey
    var id: String,
    var name: String,
    var avatarUrl: String
): Serializable