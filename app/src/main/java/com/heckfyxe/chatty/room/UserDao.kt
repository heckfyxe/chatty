package com.heckfyxe.chatty.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(users: List<User>)

    @Query("SELECT * FROM user WHERE id = :id LIMIT 1")
    fun getUserById(id: String): User?

    @Query("SELECT * FROM user WHERE id = :id LIMIT 1")
    fun getUserLiveDataById(id: String): LiveData<User>
}