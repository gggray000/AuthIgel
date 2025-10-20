package com.example.authIgel

import com.example.authIgel.domain.otp.OtpGenerator
import org.junit.Test
import kotlin.test.assertEquals

class OtpGeneratorTest {
    @Test
    fun testTotp() {
        val secret = "12345678901234567890".toByteArray()
        val otp = OtpGenerator.generateTOTP(secret)
        println(otp)
        assertEquals(6, otp.length)
    }

    @Test
    fun hotp_rfc4226_vectors() {
        val secret = "12345678901234567890".toByteArray(Charsets.US_ASCII)

        val expected = listOf(
            0L to "755224",
            1L to "287082",
            2L to "359152",
            3L to "969429",
            4L to "338314",
            5L to "254676",
            6L to "287922",
            7L to "162583",
            8L to "399871",
            9L to "520489",
        )

        expected.forEach { (counter, code) ->
            val got = OtpGenerator.generateHOTP(secret, counter, digits = 6)
            assertEquals(code, got, "HOTP mismatch at counter=$counter")
        }
    }

    @Test
    fun totp_rfc6238_sha1_vectors() {
        val secret = "12345678901234567890".toByteArray(Charsets.US_ASCII)
        val step = 30L

        // time(seconds) -> expected TOTP (8 digits, SHA-1)
        val cases = listOf(
            59L          to "94287082",
            1111111109L  to "07081804",
            1111111111L  to "14050471",
            1234567890L  to "89005924",
            2000000000L  to "69279037",
            20000000000L to "65353130",
        )

        cases.forEach { (epochSeconds, expected) ->
            val counter = epochSeconds / step
            val got = OtpGenerator.generateHOTP(secret, counter, digits = 8)
            assertEquals(expected, got, "TOTP mismatch at t=$epochSeconds")
        }
    }
}