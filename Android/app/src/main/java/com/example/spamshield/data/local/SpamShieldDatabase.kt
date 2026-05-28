package com.example.spamshield.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MessageEntity::class], version = 1, exportSchema = false)
abstract class SpamShieldDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
