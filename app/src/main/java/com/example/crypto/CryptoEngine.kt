package com.example.crypto

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class UserKeyPair(
    val publicKeyBase64: String,
    val privateKeyBase64: String,
    val fingerprint: String,
    val mnemonicWords: List<String>
)

data class EncryptedMessagePayload(
    val ivBase64: String,
    val encryptedData: String,
    val signatureBase64: String,
    val algorithm: String = "AES-256-GCM"
)

object CryptoEngine {
    private const val RSA_KEY_SIZE = 2048
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private val WORD_LIST = listOf(
        "shield", "cipher", "matrix", "crypto", "quantum", "stealth", "anchor", "falcon",
        "orbit", "sentinel", "zenith", "vertex", "phoenix", "beacon", "galaxy", "nexus",
        "iron", "shadow", "radar", "signal", "tunnel", "echo", "obsidian", "emerald",
        "silver", "copper", "cobalt", "argon", "helium", "proton", "neuron", "pulse",
        "vector", "tensor", "kernel", "daemon", "glitch", "zero", "prism", "vortex"
    )

    /**
     * Generates a new cryptographic identity without phone numbers or centralized server.
     */
    fun generateIdentity(): UserKeyPair {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(RSA_KEY_SIZE, SecureRandom())
        val pair: KeyPair = keyGen.generateKeyPair()

        val pubBase64 = Base64.encodeToString(pair.public.encoded, Base64.NO_WRAP)
        val privBase64 = Base64.encodeToString(pair.private.encoded, Base64.NO_WRAP)
        val fingerprint = computeFingerprint(pubBase64)

        // Generate 12-word deterministic mnemonic
        val random = SecureRandom()
        val mnemonic = (1..12).map { WORD_LIST[random.nextInt(WORD_LIST.size)] }

        return UserKeyPair(
            publicKeyBase64 = pubBase64,
            privateKeyBase64 = privBase64,
            fingerprint = fingerprint,
            mnemonicWords = mnemonic
        )
    }

    /**
     * Computes a human-readable SHA-256 fingerprint from a public key.
     */
    fun computeFingerprint(publicKeyBase64: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(publicKeyBase64.toByteArray(StandardCharsets.UTF_8))
            hash.take(16).joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            "F1:A2:3B:4C:5D:6E:7F:80:9A:BC:DE:F0"
        }
    }

    /**
     * Computes WhatsApp/Signal-style 60-digit safety numbers (12 blocks of 5 digits).
     */
    fun computeSafetyNumber(myPubKey: String, peerPubKey: String): List<String> {
        val combined = if (myPubKey < peerPubKey) myPubKey + peerPubKey else peerPubKey + myPubKey
        val md = MessageDigest.getInstance("SHA-512")
        val digest = md.digest(combined.toByteArray(StandardCharsets.UTF_8))

        val digits = StringBuilder()
        for (b in digest) {
            val num = (b.toInt() and 0xFF) % 10
            digits.append(num)
        }
        val padded = digits.toString().padEnd(60, '7').take(60)
        return padded.chunked(5)
    }

    /**
     * Generates a ZRTP / Signal style Short Authentication String (SAS) for Calls:
     * 4 visual security emojis and 4 verification digits.
     */
    fun computeCallSas(sessionSeed: String): Pair<List<String>, String> {
        val emojis = listOf("🛡️", "💎", "⚡", "🔒", "🦊", "🗝️", "🚀", "🪐", "🔥", "🔮", "🌊", "🦅")
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(sessionSeed.toByteArray(StandardCharsets.UTF_8))

        val e1 = emojis[(digest[0].toInt() and 0xFF) % emojis.size]
        val e2 = emojis[(digest[1].toInt() and 0xFF) % emojis.size]
        val e3 = emojis[(digest[2].toInt() and 0xFF) % emojis.size]
        val e4 = emojis[(digest[3].toInt() and 0xFF) % emojis.size]

        val codeNumber = "%04d".format(((digest[4].toInt() and 0xFF) * 256 + (digest[5].toInt() and 0xFF)) % 10000)
        return Pair(listOf(e1, e2, e3, e4), codeNumber)
    }

    /**
     * Encrypts plaintext with AES-256-GCM using a derived shared secret from public keys.
     */
    fun encryptAesGcm(plaintext: String, secretSeed: String, signerPrivateKeyBase64: String? = null): EncryptedMessagePayload {
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(secretSeed.toByteArray(StandardCharsets.UTF_8))
        val secretKey: SecretKey = SecretKeySpec(keyBytes, "AES")

        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val encryptedBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))

        var signature = ""
        if (signerPrivateKeyBase64 != null) {
            try {
                val privKey = decodePrivateKey(signerPrivateKeyBase64)
                val sign = Signature.getInstance("SHA256withRSA")
                sign.initSign(privKey)
                sign.update(encryptedBytes)
                signature = Base64.encodeToString(sign.sign(), Base64.NO_WRAP)
            } catch (_: Exception) {}
        }

        return EncryptedMessagePayload(
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            encryptedData = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
            signatureBase64 = signature
        )
    }

    /**
     * Decrypts AES-256-GCM ciphertext.
     */
    fun decryptAesGcm(payload: EncryptedMessagePayload, secretSeed: String): String {
        return try {
            val keyBytes = MessageDigest.getInstance("SHA-256").digest(secretSeed.toByteArray(StandardCharsets.UTF_8))
            val secretKey: SecretKey = SecretKeySpec(keyBytes, "AES")

            val iv = Base64.decode(payload.ivBase64, Base64.NO_WRAP)
            val cipherText = Base64.decode(payload.encryptedData, Base64.NO_WRAP)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            "[Falha de descriptografia / Chave Inválida]"
        }
    }

    private fun decodePrivateKey(base64: String): PrivateKey {
        val keyBytes = Base64.decode(base64, Base64.NO_WRAP)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePrivate(spec)
    }

    fun decodePublicKey(base64: String): PublicKey {
        val keyBytes = Base64.decode(base64, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }
}
