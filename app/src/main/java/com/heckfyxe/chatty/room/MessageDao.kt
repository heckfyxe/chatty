package com.heckfyxe.chatty.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(messages: List<Message>)

    @Query("SELECT * FROM message WHERE id = :id LIMIT 1")
    fun getMessageById(id: Long): Message
}