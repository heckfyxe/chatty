package com.heckfyxe.chatty.room

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(messages: List<Message>)

    @Transaction
    @Query(
        "UPDATE message SET id = :messageId, time = :time, sent = 1, requestId = '' WHERE requestId = :requestId"
    )
    suspend fun updateByRequestId(requestId: String, messageId: Long, time: Long)

    @Transaction
    suspend fun updateByRequestId(message: Message) {
        message.apply {
            updateByRequestId(requestId, id, time)
        }
    }

    @Query("SELECT * FROM message WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: Long): Message

    @Query("SELECT * FROM message WHERE dialogId = :dialogId ORDER BY time DESC")
    fun getMessagesLiveData(dialogId: String): LiveData<List<Message>>

    @Query("SELECT * FROM message WHERE dialogId = :dialogId AND id < :from ORDER BY time DESC LIMIT :count")
    suspend fun getPreviousMessagesById(dialogId: String, from: Long, count: Int): List<Message>

    @Query("SELECT * FROM message WHERE dialogId = :dialogId AND time < :from ORDER BY time DESC LIMIT :count")
    suspend fun getPreviousMessagesByTime(dialogId: String, from: Long, count: Int): List<Message>

    @Query("SELECT * FROM message WHERE dialogId = :dialogId AND time = (SELECT MAX(time) FROM message WHERE dialogId = :dialogId) LIMIT 1")
    suspend fun getLastMessage(dialogId: String): Message
}