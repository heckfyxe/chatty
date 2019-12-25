package com.heckfyxe.chatty.room

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface DialogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg dialog: RoomDialog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dialogs: List<RoomDialog>)

    @Update
    suspend fun update(dialog: RoomDialog)

    @Query("SELECT * FROM dialog WHERE id = :dialogId LIMIT 1")
    suspend fun getDialogById(dialogId: String): RoomDialog?

    @Query("SELECT * FROM dialog ORDER BY last_message_id DESC")
    fun getDialogsLiveData(): LiveData<List<RoomDialog>>

    @Query("UPDATE dialog SET last_message_id = :lastMessageId WHERE id = :dialogId")
    suspend fun updateDialogLastMessageId(dialogId: String, lastMessageId: Long)

    @Query("SELECT notification_id FROM dialog WHERE id = :id LIMIT 1")
    fun getNotificationIdByDialogId(id: String): Int?
}