package com.neomelt.mizuki

import android.content.Context
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

/** Stores the GitHub token encrypted with a non-exportable Android Keystore key. */
class SecureTokenStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun save(token: String) {
        require(token.isNotBlank()) { "Token cannot be empty" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun read(): String? {
        val ivEncoded = preferences.getString(KEY_IV, null) ?: return null
        val ciphertextEncoded = preferences.getString(KEY_CIPHERTEXT, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(TAG_LENGTH_BITS, Base64.decode(ivEncoded, Base64.NO_WRAP)),
            )
            val plaintext = cipher.doFinal(Base64.decode(ciphertextEncoded, Base64.NO_WRAP))
            plaintext.toString(Charsets.UTF_8)
        }.getOrNull()
    }

    fun clear() {
        preferences.edit().remove(KEY_IV).remove(KEY_CIPHERTEXT).apply()
    }

    fun hasToken(): Boolean = preferences.contains(KEY_IV) && preferences.contains(KEY_CIPHERTEXT)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "mizuki-github-token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
        const val PREFERENCES = "mizuki-secure-settings"
        const val KEY_IV = "github-token-iv"
        const val KEY_CIPHERTEXT = "github-token-ciphertext"
    }
}
