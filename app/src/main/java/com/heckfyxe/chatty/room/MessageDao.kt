package com.heckfyxe.chatty.room

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(messages: List<Message>)

    @Transaction
    @Query(
        "UPDATE message SET id = :messageId, time = :time, sent = 1, requestId = '' WHERE requestId = :requestId"
    )
    fun updateByRequestId(requestId: String, messageId: Long, time: Long)

    @Transaction
    fun updateByRequestId(message: Message) {
        message.apply {
            updateByRequestId(requestId, id, time)
        }
    }

    @Query("SELECT * FROM message WHERE id = :id LIMIT 1")
    fun getMessageById(id: Long): Message

    @Query("SELECT * FROM message WHERE dialogId = :dialogId ORDER BY time DESC")
    fun getMessagesLiveData(dialogId: String): LiveData<List<Message>>
}