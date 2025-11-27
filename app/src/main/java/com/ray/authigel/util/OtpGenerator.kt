package com.ray.authigel.util

import org.apache.commons.codec.binary.Base32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object OtpGenerator {
    var decoder = Base32()
    fun generateTOTP(secret: String, timeStepSeconds: Long = 30, digits: Int = 6): String {
        val decodedBytes = decoder.decode(secret)
        val counter = System.currentTimeMillis() / 1000 / timeStepSeconds
        return generateHOTP(decodedBytes, counter, digits)
    }

    fun generateHOTP(bytes: ByteArray, counter: Long, digits: Int): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(bytes, "HmacSHA1"))

        // Convert counter (long) → 8-byte array (big endian)
        val counterBytes = ByteArray(8)
        for (i in 7 downTo 0) {
            counterBytes[i] = (counter shr (8 * (7 - i)) and 0xFF).toByte()
        }

        val hash = mac.doFinal(counterBytes)
        val offset = hash.last().toInt() and 0x0F

        val truncated = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val otp = truncated % 10.0.pow(digits).toInt()
        return otp.toString().padStart(digits, '0')
    }
}