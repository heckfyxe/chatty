package com.heckfyxe.chatty.room

import androidx.lifecycle.LiveData
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

    @Query("SELECT * FROM message WHERE dialogId = :dialogId ORDER BY time DESC")
    fun getMessagesLiveData(dialogId: String): LiveData<List<Message>>
}