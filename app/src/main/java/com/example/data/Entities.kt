package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val peerUsername: String,
    val peerPublicKey: String,
    val peerFingerprint: String,
    val lastMessageText: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int = 0,
    val safetyVerified: Boolean = false,
    val isPinned: Boolean = false,
    val disappearingTimerSeconds: Int = 0 // 0 = off, 10, 30, 60, etc.
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderUsername: String,
    val recipientUsername: String,
    val encryptedPayload: String,
    val decryptedContent: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val status: String, // "PENDING", "SENT", "DELIVERED", "READ"
    val type: String, // "TEXT", "IMAGE", "VOICE", "FILE", "CALL"
    val isSelfDestructing: Boolean = false,
    val selfDestructDurationSeconds: Int = 0,
    val selfDestructTriggeredAt: Long = 0L,
    val isDestroyed: Boolean = false,
    val fileName: String? = null,
    val fileSizeBytes: Long = 0L
)

@Entity(tableName = "linked_devices")
data class LinkedDeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val deviceType: String, // "DESKTOP", "BROWSER", "TABLET"
    val linkedAt: Long,
    val lastSyncAt: Long,
    val devicePublicKey: String,
    val isActive: Boolean = true
)

@Entity(tableName = "call_records")
data class CallRecordEntity(
    @PrimaryKey val id: String,
    val peerUsername: String,
    val timestamp: Long,
    val durationSeconds: Int,
    val isVideo: Boolean,
    val isIncoming: Boolean,
    val encryptionFingerprint: String,
    val wasConnected: Boolean
)
