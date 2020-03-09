package com.heckfyxe.chatty.model

import androidx.room.ColumnInfo
import java.io.Serializable

data class User(
    var id: String,
    var name: String,
    @ColumnInfo(name = "avatar_url") var avatarUrl: String
) : Serializable {

    companion object {
        val DELETED: User by lazy {
            User("deleted_user_id", "deleted_user_name", "deleted_user_image_url")
        }
    }
}