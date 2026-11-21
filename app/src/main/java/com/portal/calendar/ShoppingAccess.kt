package com.portal.calendar

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** A persistent, revocable link for shared lists only. Never a calendar setup token. */
class ShoppingAccess(private val read: () -> String, private val write: (String) -> Unit) {
    @Synchronized fun token(): String = read().ifEmpty { renew() }
    @Synchronized fun renew(): String {
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(
            ByteArray(24).also { SecureRandom().nextBytes(it) })
        write(token)
        return token
    }
    @Synchronized fun valid(candidate: String?): Boolean {
        val current = read()
        return candidate != null && current.isNotEmpty() &&
            MessageDigest.isEqual(current.toByteArray(Charsets.UTF_8), candidate.toByteArray(Charsets.UTF_8))
    }
    @Synchronized fun <T> authorized(candidate: String?, action: () -> T): T {
        if (!valid(candidate)) throw SecurityException("Open Lists on the Portal and scan its phone code.")
        return action()
    }
}
