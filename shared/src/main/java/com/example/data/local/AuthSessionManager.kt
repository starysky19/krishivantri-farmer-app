package com.example.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.data.model.AuthSession
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface AuthSessionStore {
    fun saveSession(session: AuthSession)
    fun loadSession(): AuthSession?
    fun clearSession()
}

/**
 * Persists authenticated user sessions across app restarts using private SharedPreferences
 * encrypted via Android Keystore AES-256-GCM (with graceful private-prefs fallback on JVM/Robolectric).
 */
class PersistentAuthSessionStore(context: Context) : AuthSessionStore {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(AuthSession::class.java)

    override fun saveSession(session: AuthSession) {
        val json = adapter.toJson(session)
        val encrypted = encryptIfKeystoreAvailable(json)
        prefs.edit()
            .putString(KEY_SESSION_PAYLOAD, encrypted)
            .commit()
    }

    override fun loadSession(): AuthSession? {
        val stored = prefs.getString(KEY_SESSION_PAYLOAD, null) ?: return null
        val json = decryptPayload(stored) ?: return null
        return runCatching { adapter.fromJson(json) }.getOrNull()?.takeIf {
            it.userId.isNotBlank() && it.email.isNotBlank()
        }
    }

    override fun clearSession() {
        prefs.edit().remove(KEY_SESSION_PAYLOAD).commit()
    }

    private fun encryptIfKeystoreAvailable(plaintext: String): String {
        return runCatching {
            val secretKey = getOrCreateKeystoreKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val dataB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
            "$PREFIX_AES:$ivB64:$dataB64"
        }.getOrElse {
            val rawB64 = Base64.encodeToString(
                plaintext.toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            )
            "$PREFIX_PLAIN:$rawB64"
        }
    }

    private fun decryptPayload(payload: String): String? {
        return when {
            payload.startsWith("$PREFIX_AES:") -> runCatching {
                val parts = payload.split(":", limit = 3)
                if (parts.size != 3) return null
                val iv = Base64.decode(parts[1], Base64.NO_WRAP)
                val cipherBytes = Base64.decode(parts[2], Base64.NO_WRAP)
                val secretKey = getOrCreateKeystoreKey()
                val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
                String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
            }.getOrNull()

            payload.startsWith("$PREFIX_PLAIN:") -> runCatching {
                val encoded = payload.removePrefix("$PREFIX_PLAIN:")
                String(Base64.decode(encoded, Base64.NO_WRAP), StandardCharsets.UTF_8)
            }.getOrNull()

            else -> null
        }
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val PREFS_NAME = "krishivantri_auth_session"
        private const val KEY_SESSION_PAYLOAD = "session_payload_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS = "krishivantri_session_aes_key"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val PREFIX_AES = "aes"
        private const val PREFIX_PLAIN = "b64"
    }
}
