package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: ConversationEntity)

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isSelfDestructing = 1 AND isDestroyed = 0 AND selfDestructTriggeredAt > 0")
    suspend fun getActiveBurnMessages(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Update
    suspend fun update(message: MessageEntity)

    @Query("UPDATE messages SET isDestroyed = 1, decryptedContent = '[Arquivo / Mensagem autodestruída permanentemente]' WHERE id = :messageId")
    suspend fun markDestroyed(messageId: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun clearChat(conversationId: String)
}

@Dao
interface LinkedDeviceDao {
    @Query("SELECT * FROM linked_devices ORDER BY linkedAt DESC")
    fun getAllDevices(): Flow<List<LinkedDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: LinkedDeviceEntity)

    @Query("UPDATE linked_devices SET isActive = 0 WHERE id = :id")
    suspend fun revokeDevice(id: String)

    @Query("UPDATE linked_devices SET lastSyncAt = :timestamp WHERE id = :id")
    suspend fun updateSyncTime(id: String, timestamp: Long)

    @Query("DELETE FROM linked_devices WHERE id = :id")
    suspend fun deleteDevice(id: String)
}

@Dao
interface CallRecordDao {
    @Query("SELECT * FROM call_records ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(call: CallRecordEntity)

    @Query("DELETE FROM call_records")
    suspend fun clearAll()
}
