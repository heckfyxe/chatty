package com.heckfyxe.chatty.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg user: RoomUser)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(users: List<RoomUser>)

    @Query("SELECT * FROM user WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): RoomUser?

    @Query("SELECT * FROM user WHERE id = :id LIMIT 1")
    fun getUserLiveDataById(id: String): LiveData<RoomUser>
}