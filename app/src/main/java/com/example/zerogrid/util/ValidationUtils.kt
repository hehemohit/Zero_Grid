package com.example.zerogrid.util

import android.util.Patterns

/**
 * Validation utilities for emails, phone numbers, and emergency contact inputs.
 */
object ValidationUtils {
    private val PHONE_REGEX = Regex("^\\+?[0-9\\s\\-()]{7,20}\$")

    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        return Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    fun isValidPhone(phone: String?): Boolean {
        if (phone.isNullOrBlank()) return false
        val digits = phone.filter { it.isDigit() }
        if (digits.length < 7 || digits.length > 15) return false
        return PHONE_REGEX.matches(phone.trim())
    }

    fun isValidEmailOrPhone(input: String?): Boolean {
        return isValidEmail(input) || isValidPhone(input)
    }
}
