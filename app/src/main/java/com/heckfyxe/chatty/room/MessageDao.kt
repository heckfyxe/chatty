package com.heckfyxe.chatty.room

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg message: RoomMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(messages: List<RoomMessage>)

    @Transaction
    @Query(
        "UPDATE message SET id = :messageId, time = :time, sent = 1, request_id = '' WHERE request_id = :requestId"
    )
    suspend fun updateByRequestId(requestId: String, messageId: Long, time: Long)

    @Transaction
    suspend fun updateByRequestId(message: RoomMessage) {
        message.apply {
            updateByRequestId(requestId, id, time)
        }
    }

    @Query("SELECT * FROM message WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: Long): RoomMessage

    @Query("SELECT * FROM message WHERE time = :time LIMIT 1")
    suspend fun getMessageByTime(time: Long): RoomMessage?

    @Query("SELECT * FROM message WHERE dialog_id = :dialogId")
    fun getMessagesLiveData(dialogId: String): LiveData<List<RoomMessage>>

    @Query("SELECT * FROM message WHERE dialog_id = :dialogId AND id < :from ORDER BY time DESC LIMIT :count")
    suspend fun getPreviousMessagesById(dialogId: String, from: Long, count: Int): List<RoomMessage>

    @Query("SELECT * FROM message WHERE dialog_id = :dialogId AND time < :from ORDER BY time DESC LIMIT :count")
    suspend fun getPreviousMessagesByTime(
        dialogId: String,
        from: Long,
        count: Int
    ): List<RoomMessage>

    @Query("SELECT * FROM message WHERE dialog_id = :dialogId AND time > :from ORDER BY time ASC LIMIT :count")
    suspend fun getNextMessagesByTime(dialogId: String, from: Long, count: Int): List<RoomMessage>

    @Query("SELECT * FROM message WHERE dialog_id = :dialogId AND time = (SELECT MAX(time) FROM message WHERE dialog_id = :dialogId) LIMIT 1")
    suspend fun getLastMessage(dialogId: String): RoomMessage?
}