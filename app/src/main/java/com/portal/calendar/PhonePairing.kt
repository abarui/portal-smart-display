package com.portal.calendar

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** A short-lived, add-only phone session. Closing the QR dialog revokes it. */
class PhonePairing(private val now: () -> Long = { System.nanoTime() / 1_000_000L }) {
    private var token = ""
    private var deadline = 0L
    var lastConnected: String = ""
        private set

    @Synchronized fun renew(): String {
        token = Base64.getUrlEncoder().withoutPadding().encodeToString(
            ByteArray(24).also { SecureRandom().nextBytes(it) })
        deadline = now() + 10 * 60_000L
        lastConnected = ""
        return token
    }

    @Synchronized fun remainingSeconds(): Long = ((deadline - now()) / 1000L).coerceAtLeast(0L)

    @Synchronized fun valid(candidate: String?): Boolean =
        candidate != null && token.isNotEmpty() && now() < deadline &&
            MessageDigest.isEqual(token.toByteArray(Charsets.UTF_8), candidate.toByteArray(Charsets.UTF_8))

    @Synchronized fun close() { token = ""; deadline = 0L }

    @Synchronized fun <T> authorized(candidate: String?, action: () -> T): T {
        if (!valid(candidate)) throw Expired()
        return action()
    }

    @Synchronized fun connected(name: String) { lastConnected = name }
    @Synchronized fun connectedName(): String = lastConnected
    class Expired : Exception()
}
