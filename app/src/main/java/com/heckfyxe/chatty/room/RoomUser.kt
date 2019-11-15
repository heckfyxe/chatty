package com.heckfyxe.chatty.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.heckfyxe.chatty.model.User
import com.sendbird.android.Member
import java.io.Serializable

@Entity(
    tableName = "user",
    indices = [
        Index(value = ["id"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = RoomDialog::class,
            parentColumns = ["interlocutor_id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        )]
)
data class RoomUser(
    @PrimaryKey
    var id: String,
    var name: String,
    var avatarUrl: String
) : Serializable

fun Member.toRoomUser() = RoomUser(userId, nickname, profileUrl)

fun RoomUser.toDomain() = User(id, name, avatarUrl)