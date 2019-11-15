package com.heckfyxe.chatty.repository

import com.heckfyxe.chatty.room.RoomUser
import com.heckfyxe.chatty.room.UserDao
import org.koin.core.KoinComponent
import org.koin.core.inject

class UserRepository : KoinComponent {
    private val userDao: UserDao by inject()

    suspend fun saveUsersInDatabase(users: List<RoomUser>) {
        userDao.insert(users)
    }
}