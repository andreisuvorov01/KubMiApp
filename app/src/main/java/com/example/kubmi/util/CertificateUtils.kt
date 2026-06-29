package com.example.kubmi.util

import android.util.Base64
import okhttp3.CertificatePinner
import java.security.MessageDigest
import java.security.cert.Certificate

/**
 * Utility class for handling certificate pinning operations.
 * In a real application, you would obtain the actual certificate
 * hashes from the server's SSL certificate.
 */
object CertificateUtils {
    
    /**
     * Creates a CertificatePinner for kubmi.ru with proper certificate hashes.
     *
     * Note: These are placeholder values. In a real application, you would:
     * 1. Obtain the actual SSL certificate from kubmi.ru
     * 2. Extract the public key or certificate hash
     * 3. Convert to SHA-256 base64 format
     *
     * To get real certificate pins, you can use:
     * openssl s_client -connect kubmi.ru:443 -showcerts
     * Then extract the certificate and compute its hash.
     *
     * For development, you can also use tools like:
     * - Android Security Scanner
     * - Certificate Pinning Generator online tools
     * - Custom scripts to extract certificates from servers
     */
    fun createCertificatePinner(): CertificatePinner {
        return CertificatePinner.Builder()
            // Реальные хеши сертификатов kubmi.ru из ваших логов
            .add("kubmi.ru", "sha256/zGSJI/nadJFb0JM6Mkv90luKw3/drYdSTd3S0SmqyGM=")
            .add("kubmi.ru", "sha256/kZwN96eHtZftBWrOZUsd6cA4es80n3NzSk/XtYz2EqQ=")
            .add("kubmi.ru", "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=")
            .build()
    }
    
    /**
     * Utility function to compute the SHA-256 hash of a certificate for pinning.
     * This would be used during development to generate the actual pins.
     */
    fun computeCertificatePin(certificate: Certificate): String {
        val publicKey = certificate.publicKey.encoded
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(publicKey)
        return "sha256/${Base64.encodeToString(hash, Base64.NO_WRAP)}"
    }
}