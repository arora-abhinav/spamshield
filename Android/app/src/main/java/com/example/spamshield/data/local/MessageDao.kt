package com.example.spamshield.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE classification = 'spam' ORDER BY timestamp DESC")
    fun getSpamMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE date(timestamp) = date('now') ORDER BY timestamp DESC")
    fun getTodaysMessages(): Flow<List<MessageEntity>>

    @Update
    suspend fun update(message: MessageEntity)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM messages WHERE classification = 'spam'")
    suspend fun getSpamCount(): Int

    @Query("SELECT COUNT(*) FROM messages WHERE classification = 'spam' AND date(timestamp) = date('now')")
    suspend fun getTodaySpamCount(): Int

    @Query("SELECT * FROM messages WHERE predictionId = :predictionId LIMIT 1")
    suspend fun getByPredictionId(predictionId: Int): MessageEntity?
}
