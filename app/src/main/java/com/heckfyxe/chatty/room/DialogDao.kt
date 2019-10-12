package com.heckfyxe.chatty.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DialogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg dialog: Dialog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dialogs: List<Dialog>)

    @Query("SELECT * FROM Dialog ORDER BY lastMessageId DESC")
    fun getDialogsLiveData(): LiveData<List<Dialog>>

    @Query("UPDATE dialog SET lastMessageId = :lastMessageId WHERE id = :dialogId")
    suspend fun updateDialogLastMessageId(dialogId: String, lastMessageId: Long)

    @Query("SELECT notificationId FROM dialog WHERE id = :id LIMIT 1")
    fun getNotificationIdByDialogId(id: String): Int?

    @Query("""SELECT Dialog.id, Dialog.interlocutorId, User.* FROM Dialog
        JOIN User ON Dialog.interlocutorId = User.id WHERE Dialog.id = :dialogId""")
    suspend fun getInterlocutor(dialogId: String): User
}