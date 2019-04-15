package com.heckfyxe.chatty.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DialogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg dialog: Dialog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dialogs: List<Dialog>)

    @Query("SELECT * FROM Dialog ORDER BY lastMessageId DESC")
    fun getDialogsLiveData(): LiveData<List<Dialog>>

    @Query("UPDATE dialog SET lastMessageId = :lastMessageId WHERE id = :dialogId")
    fun updateDialogLastMessageId(dialogId: String, lastMessageId: Long)

    @Query("SELECT notificationId FROM dialog WHERE id = :id LIMIT 1")
    fun getNotificationIdByDialogId(id: String): Int?
}