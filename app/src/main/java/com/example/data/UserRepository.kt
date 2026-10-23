package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.crypto.CryptoEngine
import com.example.crypto.UserKeyPair
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class UserProfile(
    val username: String,
    val publicKeyBase64: String,
    val privateKeyBase64: String,
    val fingerprint: String,
    val mnemonic: List<String>,
    val biometricLockEnabled: Boolean,
    val isAirGapOffline: Boolean,
    val defaultBurnTimer: Int
)

class UserRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cipher_chat_secure_vault", Context.MODE_PRIVATE)
    private val db = AppDatabase.getDatabase(context)
    val conversationDao = db.conversationDao()
    val messageDao = db.messageDao()
    val linkedDeviceDao = db.linkedDeviceDao()
    val callRecordDao = db.callRecordDao()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked = _isAppLocked.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val username = prefs.getString("username", null)
        val pubKey = prefs.getString("pub_key", null)
        val privKey = prefs.getString("priv_key", null)
        val fingerprint = prefs.getString("fingerprint", null)
        val mnemonicStr = prefs.getString("mnemonic", "") ?: ""
        val biometric = prefs.getBoolean("biometric_lock", false)
        val airGap = prefs.getBoolean("air_gap_offline", false)
        val burnTimer = prefs.getInt("burn_timer", 0)

        if (username != null && pubKey != null && privKey != null) {
            _userProfile.value = UserProfile(
                username = username,
                publicKeyBase64 = pubKey,
                privateKeyBase64 = privKey,
                fingerprint = fingerprint ?: CryptoEngine.computeFingerprint(pubKey),
                mnemonic = mnemonicStr.split(",").filter { it.isNotBlank() },
                biometricLockEnabled = biometric,
                isAirGapOffline = airGap,
                defaultBurnTimer = burnTimer
            )
            if (biometric) {
                _isAppLocked.value = true
            }
        }
    }

    fun registerNewIdentity(
        username: String,
        enableBiometric: Boolean,
        onComplete: (UserProfile) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val keyPair: UserKeyPair = CryptoEngine.generateIdentity()
            val cleanUsername = if (username.startsWith("@")) username else "@$username"

            prefs.edit()
                .putString("username", cleanUsername)
                .putString("pub_key", keyPair.publicKeyBase64)
                .putString("priv_key", keyPair.privateKeyBase64)
                .putString("fingerprint", keyPair.fingerprint)
                .putString("mnemonic", keyPair.mnemonicWords.joinToString(","))
                .putBoolean("biometric_lock", enableBiometric)
                .putBoolean("air_gap_offline", false)
                .putInt("burn_timer", 0)
                .apply()

            val profile = UserProfile(
                username = cleanUsername,
                publicKeyBase64 = keyPair.publicKeyBase64,
                privateKeyBase64 = keyPair.privateKeyBase64,
                fingerprint = keyPair.fingerprint,
                mnemonic = keyPair.mnemonicWords,
                biometricLockEnabled = enableBiometric,
                isAirGapOffline = false,
                defaultBurnTimer = 0
            )
            _userProfile.value = profile
            _isAppLocked.value = false

            // Seed initial anonymous encrypted contacts
            seedInitialEncryptedChats(cleanUsername, keyPair.publicKeyBase64, keyPair.privateKeyBase64)

            onComplete(profile)
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_lock", enabled).apply()
        _userProfile.value = _userProfile.value?.copy(biometricLockEnabled = enabled)
    }

    fun setAirGapOffline(enabled: Boolean) {
        prefs.edit().putBoolean("air_gap_offline", enabled).apply()
        _userProfile.value = _userProfile.value?.copy(isAirGapOffline = enabled)
    }

    fun setDefaultBurnTimer(seconds: Int) {
        prefs.edit().putInt("burn_timer", seconds).apply()
        _userProfile.value = _userProfile.value?.copy(defaultBurnTimer = seconds)
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        _isAppLocked.value = true
    }

    suspend fun sendMessage(
        conversationId: String,
        content: String,
        type: String = "TEXT",
        isSelfDestructing: Boolean = false,
        burnDurationSeconds: Int = 0,
        fileName: String? = null,
        fileSizeBytes: Long = 0L
    ) {
        val user = _userProfile.value ?: return
        val conversation = conversationDao.getConversationById(conversationId) ?: return

        // Encrypt message payload with AES-256-GCM using derived shared secret between keys
        val secretSeed = user.privateKeyBase64 + conversation.peerPublicKey
        val encryptedPayload = CryptoEngine.encryptAesGcm(
            plaintext = content,
            secretSeed = secretSeed,
            signerPrivateKeyBase64 = user.privateKeyBase64
        )

        val messageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val msgEntity = MessageEntity(
            id = messageId,
            conversationId = conversationId,
            senderUsername = user.username,
            recipientUsername = conversation.peerUsername,
            encryptedPayload = "${encryptedPayload.ivBase64}:${encryptedPayload.encryptedData}:${encryptedPayload.signatureBase64}",
            decryptedContent = content,
            timestamp = now,
            isFromMe = true,
            status = if (user.isAirGapOffline) "PENDING" else "DELIVERED",
            type = type,
            isSelfDestructing = isSelfDestructing,
            selfDestructDurationSeconds = burnDurationSeconds,
            selfDestructTriggeredAt = if (isSelfDestructing) now else 0L,
            isDestroyed = false,
            fileName = fileName,
            fileSizeBytes = fileSizeBytes
        )

        messageDao.insert(msgEntity)

        // Update conversation last message
        val updatedConv = conversation.copy(
            lastMessageText = if (type == "IMAGE") "📷 Foto Criptografada" else if (type == "FILE") "📎 Arquivo Criptografado ($fileName)" else content,
            lastMessageTimestamp = now
        )
        conversationDao.insertOrUpdate(updatedConv)

        // If not offline, simulate peer auto-reply / delivery handshake
        if (!user.isAirGapOffline && !conversation.peerUsername.contains("Desktop")) {
            simulatePeerReply(conversation, user, content)
        }
    }

    private fun simulatePeerReply(conversation: ConversationEntity, user: UserProfile, myContent: String) {
        CoroutineScope(Dispatchers.IO).launch {
            kotlinx.coroutines.delay(1200)
            val now = System.currentTimeMillis()
            val replyText = when {
                myContent.contains("chave", ignoreCase = true) || myContent.contains("key", ignoreCase = true) ->
                    "Chave pública conferida no meu nó. Hash de segurança validado com sucesso!"
                myContent.contains("chamada", ignoreCase = true) || myContent.contains("call", ignoreCase = true) ->
                    "Estou disponível para chamada de voz/vídeo E2EE. O canal ZRTP está pronto."
                myContent.contains("arquivo", ignoreCase = true) || myContent.contains("documento", ignoreCase = true) ->
                    "Recebido com proteção AES-256. O temporizador de autodestruição está ativo no meu cache."
                else -> "Recebi seu pacote cifrado via relay P2P. Nenhuma mensagem retida em servidores."
            }

            val secretSeed = conversation.peerPublicKey + user.privateKeyBase64
            val enc = CryptoEngine.encryptAesGcm(replyText, secretSeed)

            val replyMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversation.id,
                senderUsername = conversation.peerUsername,
                recipientUsername = user.username,
                encryptedPayload = "${enc.ivBase64}:${enc.encryptedData}:${enc.signatureBase64}",
                decryptedContent = replyText,
                timestamp = now,
                isFromMe = false,
                status = "READ",
                type = "TEXT",
                isSelfDestructing = conversation.disappearingTimerSeconds > 0,
                selfDestructDurationSeconds = conversation.disappearingTimerSeconds,
                selfDestructTriggeredAt = if (conversation.disappearingTimerSeconds > 0) now else 0L,
                isDestroyed = false
            )
            messageDao.insert(replyMsg)

            conversationDao.insertOrUpdate(
                conversation.copy(
                    lastMessageText = replyText,
                    lastMessageTimestamp = now
                )
            )
        }
    }

    private suspend fun seedInitialEncryptedChats(myUsername: String, myPubKey: String, myPrivKey: String) {
        val now = System.currentTimeMillis()

        // 1. Satoshi Nakamoto
        val satoshiPub = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxSatoshiPubKeyVaultBlock99812..."
        val satoshiConv = ConversationEntity(
            id = "conv_satoshi",
            peerUsername = "@satoshi",
            peerPublicKey = satoshiPub,
            peerFingerprint = "7F:9B:21:A4:CC:88:01:DF:E4:10:98:23:44:AC:B2:FF",
            lastMessageText = "Seja bem-vindo ao CipherChat. Sem números, sem servidores centrais.",
            lastMessageTimestamp = now - 3600000,
            unreadCount = 1,
            safetyVerified = true,
            isPinned = true,
            disappearingTimerSeconds = 0
        )
        conversationDao.insertOrUpdate(satoshiConv)

        messageDao.insert(
            MessageEntity(
                id = "msg_satoshi_1",
                conversationId = "conv_satoshi",
                senderUsername = "@satoshi",
                recipientUsername = myUsername,
                encryptedPayload = "RSA2048:GCM:Seja bem-vindo...",
                decryptedContent = "Seja bem-vindo ao CipherChat. Sem números, sem servidores centrais. Você agora é o único dono da sua chave privada.",
                timestamp = now - 3600000,
                isFromMe = false,
                status = "READ",
                type = "TEXT"
            )
        )

        // 2. Alice (Security Researcher)
        val alicePub = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxAlicePubKeySecureLab..."
        val aliceConv = ConversationEntity(
            id = "conv_alice",
            peerUsername = "@alice_crypto",
            peerPublicKey = alicePub,
            peerFingerprint = "3A:C2:59:E0:11:84:87:66:B9:FD:90:71:02:44:DA:BC",
            lastMessageText = "Te enviei um relatório sigiloso com autodestruição de 30 segundos.",
            lastMessageTimestamp = now - 1800000,
            unreadCount = 1,
            safetyVerified = true,
            isPinned = false,
            disappearingTimerSeconds = 30
        )
        conversationDao.insertOrUpdate(aliceConv)

        messageDao.insert(
            MessageEntity(
                id = "msg_alice_1",
                conversationId = "conv_alice",
                senderUsername = "@alice_crypto",
                recipientUsername = myUsername,
                encryptedPayload = "CIPHER_AES_GCM_PAYLOAD",
                decryptedContent = "Te enviei um relatório sigiloso com autodestruição de 30 segundos. Ele se apagará do cache após visualização.",
                timestamp = now - 1800000,
                isFromMe = false,
                status = "READ",
                type = "TEXT"
            )
        )

        messageDao.insert(
            MessageEntity(
                id = "msg_alice_2",
                conversationId = "conv_alice",
                senderUsername = "@alice_crypto",
                recipientUsername = myUsername,
                encryptedPayload = "CIPHER_FILE_PAYLOAD_BURN",
                decryptedContent = "Auditoria_ZeroKnowledge_Protocolo_v2.pdf",
                timestamp = now - 1700000,
                isFromMe = false,
                status = "READ",
                type = "FILE",
                isSelfDestructing = true,
                selfDestructDurationSeconds = 45,
                selfDestructTriggeredAt = 0L, // will start when user taps to reveal
                isDestroyed = false,
                fileName = "Auditoria_ZeroKnowledge.pdf",
                fileSizeBytes = 184320L
            )
        )

        // 3. Whistleblower (Anonymous Source)
        val wbConv = ConversationEntity(
            id = "conv_whistleblower",
            peerUsername = "@whistleblower",
            peerPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxWhistlePub...",
            peerFingerprint = "91:E4:B7:32:00:DF:44:AC:19:88:51:30:EF:CA:88:12",
            lastMessageText = "Foto de credencial criptografada autodestrutiva.",
            lastMessageTimestamp = now - 600000,
            unreadCount = 1,
            safetyVerified = false,
            isPinned = false,
            disappearingTimerSeconds = 20
        )
        conversationDao.insertOrUpdate(wbConv)

        messageDao.insert(
            MessageEntity(
                id = "msg_wb_1",
                conversationId = "conv_whistleblower",
                senderUsername = "@whistleblower",
                recipientUsername = myUsername,
                encryptedPayload = "CIPHER_IMAGE_BURN_20S",
                decryptedContent = "Foto de credencial confidencial. Destruição após leitura.",
                timestamp = now - 600000,
                isFromMe = false,
                status = "READ",
                type = "IMAGE",
                isSelfDestructing = true,
                selfDestructDurationSeconds = 25,
                selfDestructTriggeredAt = 0L,
                isDestroyed = false,
                fileName = "evidencia_sigilosa.png",
                fileSizeBytes = 524288L
            )
        )

        // Seed linked devices
        linkedDeviceDao.insert(
            LinkedDeviceEntity(
                id = "device_desktop_linux",
                name = "CipherChat Desktop - Linux (x86_64)",
                deviceType = "DESKTOP",
                linkedAt = now - 86400000,
                lastSyncAt = now - 120000,
                devicePublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEADesktopLinux...",
                isActive = true
            )
        )

        linkedDeviceDao.insert(
            LinkedDeviceEntity(
                id = "device_tablet_mac",
                name = "CipherChat Mac App (Apple Silicon)",
                deviceType = "DESKTOP",
                linkedAt = now - 172800000,
                lastSyncAt = now - 3600000,
                devicePublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAMacApp...",
                isActive = true
            )
        )

        // Seed recent call logs
        callRecordDao.insert(
            CallRecordEntity(
                id = "call_1",
                peerUsername = "@satoshi",
                timestamp = now - 86400000,
                durationSeconds = 142,
                isVideo = false,
                isIncoming = true,
                encryptionFingerprint = "7F:9B:21:A4 / SAS 🛡️💎⚡🔒",
                wasConnected = true
            )
        )

        callRecordDao.insert(
            CallRecordEntity(
                id = "call_2",
                peerUsername = "@alice_crypto",
                timestamp = now - 14400000,
                durationSeconds = 310,
                isVideo = true,
                isIncoming = false,
                encryptionFingerprint = "3A:C2:59:E0 / SAS 🦊🗝️🚀🪐",
                wasConnected = true
            )
        )
    }
}
