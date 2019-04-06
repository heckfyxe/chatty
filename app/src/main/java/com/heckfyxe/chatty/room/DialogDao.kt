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

    @Query("SELECT * FROM Dialog ")
    fun getDialogsLiveData(): LiveData<List<Dialog>>
}