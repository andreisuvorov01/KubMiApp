package com.example.kubmi.util

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject

class SecurePreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
    private val adminLogger by lazy { AdminLogger(context) }
    
    companion object {
        const val MIN_PASSWORD_LENGTH = 8

        private const val KEY_PASSWORD_HASH = "admin_password_hash"
        private const val KEY_PASSWORD_SALT = "admin_password_salt"
        private const val KEY_PASSWORD_ALGORITHM = "admin_password_algorithm"
        private const val KEY_LEGACY_ENCRYPTED_PASSWORD = "admin_password"
        private const val KEY_FAILED_ATTEMPTS = "admin_failed_attempts"
        private const val KEY_LOCKOUT_STARTED_ELAPSED = "admin_lockout_started_elapsed"
        private const val KEY_LOCKOUT_UNTIL_ELAPSED = "admin_lockout_until_elapsed"

        private const val PBKDF2_ITERATIONS = 210_000
        private const val PBKDF2_KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH_BYTES = 16
        private const val MAX_ATTEMPTS_BEFORE_LOCKOUT = 5
        private const val BASE_LOCKOUT_MS = 30_000L
        private const val MAX_LOCKOUT_MS = 15 * 60_000L

        // Retained only for one-time migration of passwords saved by older builds.
        private const val KEY_ALIAS = "kubmi_admin_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH = 128
    }
    
    fun savePassword(password: String) {
        require(password.length >= MIN_PASSWORD_LENGTH) {
            "Пароль должен содержать не менее $MIN_PASSWORD_LENGTH символов"
        }

        val salt = ByteArray(SALT_LENGTH_BYTES).also(SecureRandom()::nextBytes)
        val algorithm = preferredPbkdf2Algorithm()
        val hash = derivePasswordHash(password, salt, algorithm)
        preferences.edit()
            .putString(KEY_PASSWORD_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PASSWORD_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_PASSWORD_ALGORITHM, algorithm)
            .remove(KEY_LEGACY_ENCRYPTED_PASSWORD)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_STARTED_ELAPSED)
            .remove(KEY_LOCKOUT_UNTIL_ELAPSED)
            .apply()
        adminLogger.logAction("ADMIN_PASSWORD_SET")
    }
    
    fun verifyPassword(password: String): Boolean {
        if (getRemainingLockoutMillis() > 0L) {
            adminLogger.logAction("ADMIN_AUTH_BLOCKED", "lockout_active=true")
            return false
        }

        val verified = try {
            val saltBase64 = preferences.getString(KEY_PASSWORD_SALT, null)
            val hashBase64 = preferences.getString(KEY_PASSWORD_HASH, null)
            if (saltBase64 != null && hashBase64 != null) {
                val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
                val expectedHash = Base64.decode(hashBase64, Base64.NO_WRAP)
                val algorithm = preferences.getString(
                    KEY_PASSWORD_ALGORITHM,
                    preferredPbkdf2Algorithm()
                ) ?: preferredPbkdf2Algorithm()
                val actualHash = derivePasswordHash(password, salt, algorithm)
                MessageDigest.isEqual(expectedHash, actualHash)
            } else {
                verifyAndMigrateLegacyPassword(password)
            }
        } catch (e: Exception) {
            false
        }

        if (verified) {
            clearFailedAttempts()
            adminLogger.logAction("ADMIN_AUTH_SUCCESS")
        } else {
            recordFailedAttempt()
            adminLogger.logAction(
                "ADMIN_AUTH_FAILED",
                "failed_attempts=${preferences.getInt(KEY_FAILED_ATTEMPTS, 0)}"
            )
        }
        return verified
    }
    
    fun isPasswordSet(): Boolean {
        val hasHash = !preferences.getString(KEY_PASSWORD_HASH, null).isNullOrBlank() &&
            !preferences.getString(KEY_PASSWORD_SALT, null).isNullOrBlank()
        val hasLegacyPassword =
            !preferences.getString(KEY_LEGACY_ENCRYPTED_PASSWORD, null).isNullOrBlank()
        return hasHash || hasLegacyPassword
    }
    
    fun clearPassword() {
        preferences.edit()
            .remove(KEY_PASSWORD_HASH)
            .remove(KEY_PASSWORD_SALT)
            .remove(KEY_PASSWORD_ALGORITHM)
            .remove(KEY_LEGACY_ENCRYPTED_PASSWORD)
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_STARTED_ELAPSED)
            .remove(KEY_LOCKOUT_UNTIL_ELAPSED)
            .apply()
    }

    fun getRemainingLockoutMillis(): Long {
        val started = preferences.getLong(KEY_LOCKOUT_STARTED_ELAPSED, 0L)
        val until = preferences.getLong(KEY_LOCKOUT_UNTIL_ELAPSED, 0L)
        val now = SystemClock.elapsedRealtime()
        if (started > 0L && now < started) {
            preferences.edit()
                .remove(KEY_LOCKOUT_STARTED_ELAPSED)
                .remove(KEY_LOCKOUT_UNTIL_ELAPSED)
                .apply()
            return 0L
        }
        return (until - now).coerceAtLeast(0L)
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
    
    private fun derivePasswordHash(
        password: String,
        salt: ByteArray,
        algorithm: String
    ): ByteArray {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            PBKDF2_KEY_LENGTH_BITS
        )
        return try {
            SecretKeyFactory.getInstance(algorithm)
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun preferredPbkdf2Algorithm(): String {
        return runCatching {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            "PBKDF2WithHmacSHA256"
        }.getOrDefault("PBKDF2WithHmacSHA1")
    }

    private fun verifyAndMigrateLegacyPassword(password: String): Boolean {
        val encrypted = preferences.getString(KEY_LEGACY_ENCRYPTED_PASSWORD, null)
            ?: return false
        val matches = decryptLegacyPassword(encrypted) == password
        if (matches) savePassword(password)
        return matches
    }

    private fun decryptLegacyPassword(encrypted: String): String {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val combined = Base64.decode(encrypted, Base64.DEFAULT)
        val iv = combined.copyOfRange(0, IV_LENGTH)
        val encryptedBytes = combined.copyOfRange(IV_LENGTH, combined.size)
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun recordFailedAttempt() {
        val failures = preferences.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val editor = preferences.edit().putInt(KEY_FAILED_ATTEMPTS, failures)
        if (failures >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
            val exponent = (failures - MAX_ATTEMPTS_BEFORE_LOCKOUT).coerceIn(0, 5)
            val duration = (BASE_LOCKOUT_MS * (1L shl exponent))
                .coerceAtMost(MAX_LOCKOUT_MS)
            editor.putLong(
                KEY_LOCKOUT_STARTED_ELAPSED,
                SystemClock.elapsedRealtime()
            ).putLong(
                KEY_LOCKOUT_UNTIL_ELAPSED,
                SystemClock.elapsedRealtime() + duration
            )
        }
        editor.apply()
    }

    private fun clearFailedAttempts() {
        preferences.edit()
            .remove(KEY_FAILED_ATTEMPTS)
            .remove(KEY_LOCKOUT_STARTED_ELAPSED)
            .remove(KEY_LOCKOUT_UNTIL_ELAPSED)
            .apply()
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
