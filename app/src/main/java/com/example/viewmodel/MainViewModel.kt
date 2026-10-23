package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CryptoEngine
import com.example.data.CallRecordEntity
import com.example.data.ConversationEntity
import com.example.data.LinkedDeviceEntity
import com.example.data.MessageEntity
import com.example.data.UserProfile
import com.example.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class ActiveCallInfo(
    val peerUsername: String,
    val isVideo: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserRepository(application)

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
    val isAppLocked: StateFlow<Boolean> = repository.isAppLocked

    val conversations: StateFlow<List<ConversationEntity>> = repository.conversationDao
        .getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val linkedDevices: StateFlow<List<LinkedDeviceEntity>> = repository.linkedDeviceDao
        .getAllDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callRecords: StateFlow<List<CallRecordEntity>> = repository.callRecordDao
        .getAllCalls()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId = _activeConversationId.asStateFlow()

    private val _activeMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val activeMessages = _activeMessages.asStateFlow()

    private val _activeCall = MutableStateFlow<ActiveCallInfo?>(null)
    val activeCall = _activeCall.asStateFlow()

    fun registerIdentity(username: String, enableBiometric: Boolean) {
        repository.registerNewIdentity(username, enableBiometric) {}
    }

    fun unlockApp() {
        repository.unlockApp()
    }

    fun lockApp() {
        repository.lockApp()
    }

    fun toggleBiometric(enabled: Boolean) {
        repository.setBiometricLockEnabled(enabled)
    }

    fun toggleAirGap(enabled: Boolean) {
        repository.setAirGapOffline(enabled)
    }

    fun setDefaultBurnTimer(seconds: Int) {
        repository.setDefaultBurnTimer(seconds)
    }

    fun openConversation(id: String) {
        _activeConversationId.value = id
        viewModelScope.launch {
            repository.messageDao.getMessagesForConversation(id).collect { msgs ->
                _activeMessages.value = msgs
            }
        }
    }

    fun closeConversation() {
        _activeConversationId.value = null
        _activeMessages.value = emptyList()
    }

    fun sendMessage(
        conversationId: String,
        content: String,
        type: String = "TEXT",
        isBurn: Boolean = false,
        burnSeconds: Int = 0,
        fileName: String? = null
    ) {
        viewModelScope.launch {
            repository.sendMessage(
                conversationId = conversationId,
                content = content,
                type = type,
                isSelfDestructing = isBurn,
                burnDurationSeconds = burnSeconds,
                fileName = fileName,
                fileSizeBytes = if (fileName != null) 102400L else 0L
            )
        }
    }

    fun burnMessage(messageId: String) {
        viewModelScope.launch {
            repository.messageDao.markDestroyed(messageId)
        }
    }

    fun toggleSafetyVerification(conversationId: String, verified: Boolean) {
        viewModelScope.launch {
            val conv = repository.conversationDao.getConversationById(conversationId) ?: return@launch
            repository.conversationDao.update(conv.copy(safetyVerified = verified))
        }
    }

    fun addNewContact(peerUsername: String, peerPublicKey: String) {
        viewModelScope.launch {
            val cleanUsername = if (peerUsername.startsWith("@")) peerUsername else "@$peerUsername"
            val id = "conv_${cleanUsername.replace("@", "")}"
            val fingerprint = CryptoEngine.computeFingerprint(peerPublicKey)

            val conv = ConversationEntity(
                id = id,
                peerUsername = cleanUsername,
                peerPublicKey = peerPublicKey,
                peerFingerprint = fingerprint,
                lastMessageText = "Canal cifrado estabelecido com sucesso.",
                lastMessageTimestamp = System.currentTimeMillis(),
                unreadCount = 0,
                safetyVerified = false,
                isPinned = false,
                disappearingTimerSeconds = 0
            )
            repository.conversationDao.insertOrUpdate(conv)
            openConversation(id)
        }
    }

    fun startCall(peerUsername: String, isVideo: Boolean) {
        _activeCall.value = ActiveCallInfo(peerUsername, isVideo)
    }

    fun endCall(durationSeconds: Int) {
        val current = _activeCall.value ?: return
        _activeCall.value = null

        viewModelScope.launch {
            val sas = CryptoEngine.computeCallSas("SESSION_SEED_${current.peerUsername}")
            repository.callRecordDao.insert(
                CallRecordEntity(
                    id = UUID.randomUUID().toString(),
                    peerUsername = current.peerUsername,
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = durationSeconds,
                    isVideo = current.isVideo,
                    isIncoming = false,
                    encryptionFingerprint = "${sas.first.joinToString("")} ${sas.second}",
                    wasConnected = true
                )
            )
        }
    }

    fun syncDevicesNow() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            linkedDevices.value.forEach { device ->
                if (device.isActive) {
                    repository.linkedDeviceDao.updateSyncTime(device.id, now)
                }
            }
        }
    }

    fun revokeDevice(id: String) {
        viewModelScope.launch {
            repository.linkedDeviceDao.revokeDevice(id)
        }
    }

    fun addLinkedDevice(name: String, type: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.linkedDeviceDao.insert(
                LinkedDeviceEntity(
                    id = "device_" + UUID.randomUUID().toString().take(8),
                    name = name,
                    deviceType = type,
                    linkedAt = now,
                    lastSyncAt = now,
                    devicePublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A" + UUID.randomUUID().toString().replace("-", ""),
                    isActive = true
                )
            )
        }
    }
}
