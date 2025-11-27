package com.ray.authigel.util

import android.net.Uri
import org.apache.commons.codec.binary.Base32
import java.security.SecureRandom

object OtpSeedFactory {
    private var encoder = Base32()
    private val rng = SecureRandom()
    private val BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray()

    /** Generate a random Base32 secret (default: 20 bytes ≈ 32 Base32 chars). */
    fun generateRandomSecret(numBytes: Int = 20): String {
        val bytes = ByteArray(numBytes)
        rng.nextBytes(bytes)
        return encoder.encodeAsString(bytes)
    }

    /** Build an otpauth URL for TOTP. */
    fun buildOtpAuthUrl(
        issuer: String,
        holder: String,
        secretBase32: String,
        algorithm: String = "SHA1",
        digits: Int = 6,
        period: Int = 30
    ): String {
        // Label is "Issuer:holder"
        val label = "$issuer:$holder"
        // Manually compose to keep label colon readable; encode query params safely
        val qIssuer = Uri.encode(issuer)
        val qSecret = Uri.encode(secretBase32)
        val qAlgo = Uri.encode(algorithm)
        return "otpauth://totp/$label?secret=$qSecret&issuer=$qIssuer&algorithm=$qAlgo&digits=$digits&period=$period"
    }
}