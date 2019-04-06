package com.heckfyxe.chatty.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Dialog::class, Message::class, User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
            }

            return instance!!
        }

    }

    abstract fun getDialogDao(): DialogDao
    abstract fun getMessageDao(): MessageDao
    abstract fun getUserDao(): UserDao
}