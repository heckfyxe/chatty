package com.heckfyxe.chatty.repository

import com.heckfyxe.chatty.room.User
import com.heckfyxe.chatty.room.UserDao
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class UserRepository : KoinComponent {
    private val userDao: UserDao by inject()

    suspend fun saveUsersInDatabase(users: List<User>) {
        userDao.insert(users)
    }
}