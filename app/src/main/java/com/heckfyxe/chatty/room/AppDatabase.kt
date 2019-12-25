package com.heckfyxe.chatty.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [RoomDialog::class, RoomMessage::class, RoomUser::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            synchronized(AppDatabase::class.java) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context,
                        AppDatabase::class.java,
                        "app_database"
                    ).apply {
                        fallbackToDestructiveMigration()
                    }.build()
                }
            }

            return instance!!
        }

    }

    abstract fun getDialogDao(): DialogDao
    abstract fun getMessageDao(): MessageDao
    abstract fun getUserDao(): UserDao
}