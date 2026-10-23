package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.CryptoEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CipherChat", appName)
  }

  @Test
  fun `crypto engine generates valid RSA identity and mnemonic`() {
    val identity = CryptoEngine.generateIdentity()
    assertNotNull(identity.publicKeyBase64)
    assertNotNull(identity.privateKeyBase64)
    assertEquals(12, identity.mnemonicWords.size)
    assertTrue(identity.fingerprint.contains(":"))
  }

  @Test
  fun `aes gcm end to end encryption and decryption roundtrip`() {
    val plaintext = "Mensagem sigilosa de teste E2EE"
    val sharedSecret = "secret_seed_between_alice_and_bob"

    val payload = CryptoEngine.encryptAesGcm(plaintext, sharedSecret)
    assertNotNull(payload.ivBase64)
    assertNotNull(payload.encryptedData)

    val decrypted = CryptoEngine.decryptAesGcm(payload, sharedSecret)
    assertEquals(plaintext, decrypted)
  }

  @Test
  fun `safety number generation creates 12 blocks of 5 digits`() {
    val keyA = "PUB_KEY_A_1234567890"
    val keyB = "PUB_KEY_B_0987654321"

    val blocks = CryptoEngine.computeSafetyNumber(keyA, keyB)
    assertEquals(12, blocks.size)
    blocks.forEach { block ->
      assertEquals(5, block.length)
    }
  }

  @Test
  fun `call SAS verification generates emojis and code`() {
    val (emojis, code) = CryptoEngine.computeCallSas("SESSION_TEST")
    assertEquals(4, emojis.size)
    assertEquals(4, code.length)
  }
}
