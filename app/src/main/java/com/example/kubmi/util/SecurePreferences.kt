package com.example.kubmi.util

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import kotlin.random.Random

class SecurePreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_ALIAS = "kubmi_admin_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val IV_LENGTH = 12 // For GCM
        private const val TAG_LENGTH = 128 // For GCM
        
        // Default admin password - hardcoded
        const val DEFAULT_PASSWORD = "kubmiadmin"
    }
    
    fun savePassword(password: String) {
        try {
            val encryptedPassword = encrypt(password)
            preferences.edit().putString("admin_password", encryptedPassword).apply()
        } catch (e: Exception) {
            // Handle encryption error
            e.printStackTrace()
        }
    }
    
    fun verifyPassword(password: String): Boolean {
        // First check against default password
        if (password == DEFAULT_PASSWORD) {
            return true
        }
        
        // Then check against custom password if set
        return try {
            val storedPassword = preferences.getString("admin_password", null)
            if (storedPassword != null) {
                val decryptedPassword = decrypt(storedPassword)
                decryptedPassword == password
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun isPasswordSet(): Boolean {
        // Always return true - default password is always available
        return true
    }
    
    fun clearPassword() {
        preferences.edit().remove("admin_password").apply()
    }
    
    // Screensaver mode settings
    fun saveScreensaverMode(mode: String) {
        preferences.edit().putString("screensaver_mode", mode).apply()
    }
    
    fun getScreensaverMode(): String {
        return preferences.getString("screensaver_mode", "news") ?: "news"
    }
    
    // Screensaver delay settings (in seconds)
    fun saveScreensaverDelay(delaySeconds: Int) {
        preferences.edit().putInt("screensaver_delay", delaySeconds).apply()
    }
    
    fun getScreensaverDelay(): Int {
        return preferences.getInt("screensaver_delay", 300) // Default 5 minutes
    }
    
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        } else {
            return keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }
    }
    
    private fun encrypt(input: String): String {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        
        // Generate a random IV for GCM
        val iv = ByteArray(IV_LENGTH)
        Random.nextBytes(iv)
        
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        
        val encryptedBytes = cipher.doFinal(input.toByteArray(Charsets.UTF_8))
        
        // Combine IV and encrypted data
        val combined = iv + encryptedBytes
        
        // Encode to Base64 for storage
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }
    
    private fun decrypt(encrypted: String): String {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        
        // Decode from Base64
        val combined = Base64.decode(encrypted, Base64.DEFAULT)
        
        // Extract IV and encrypted data
        val iv = combined.copyOfRange(0, IV_LENGTH)
        val encryptedBytes = combined.copyOfRange(IV_LENGTH, combined.size)
        
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}

// Available screensaver delay options (in seconds)
object ScreensaverDelays {
    const val DELAY_1_MIN = 60
    const val DELAY_2_MIN = 120
    const val DELAY_5_MIN = 300
    const val DELAY_10_MIN = 600
    const val DELAY_15_MIN = 900
    const val DELAY_30_MIN = 1800
    
    val DELAY_OPTIONS = listOf(
        60 to "1 minute",
        120 to "2 minutes",
        300 to "5 minutes",
        600 to "10 minutes",
        900 to "15 minutes",
        1800 to "30 minutes"
    )
}