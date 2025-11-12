package com.ray.authigel.util

import android.net.Uri
import java.security.SecureRandom

object OtpSeedFactory {
    private val rng = SecureRandom()
    private val BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray()

    /** Generate a random Base32 secret (default: 20 bytes ≈ 32 Base32 chars). */
    fun randomSecret(numBytes: Int = 20): String {
        val bytes = ByteArray(numBytes)
        rng.nextBytes(bytes)
        return base32EncodeNoPadding(bytes)
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
        val qHolder = Uri.encode(holder)
        val qSecret = Uri.encode(secretBase32)
        val qAlgo = Uri.encode(algorithm)
        return "otpauth://totp/$label?secret=$qSecret&issuer=$qIssuer&algorithm=$qAlgo&digits=$digits&period=$period"
    }

    // --- Minimal Base32 (RFC 4648) encoder, no padding ('=') ---
    private fun base32EncodeNoPadding(input: ByteArray): String {
        val out = StringBuilder((input.size * 8 + 4) / 5)
        var i = 0
        var curr = 0
        var bits = 0

        while (i < input.size) {
            curr = (curr shl 8) or (input[i].toInt() and 0xFF)
            bits += 8
            i++
            while (bits >= 5) {
                val index = (curr shr (bits - 5)) and 0x1F
                bits -= 5
                out.append(BASE32[index])
            }
        }
        if (bits > 0) {
            val index = (curr shl (5 - bits)) and 0x1F
            out.append(BASE32[index])
        }
        return out.toString()
    }
}