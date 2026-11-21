package com.portal.calendar

import org.junit.Assert.*
import org.junit.Test

class PhonePairingTest {
    @Test fun sessionIsUnavailableUntilOpenedAndRejectsOtherTokens() {
        val session = PhonePairing()
        assertFalse(session.valid(null))
        assertFalse(session.valid(""))
        val token = session.renew()
        assertEquals(32, token.length)
        assertTrue(session.valid(token))
        assertFalse(session.valid(token + "x"))
        assertFalse(session.valid("guess"))
    }
    @Test fun sessionExpiresAtTenMinutes() {
        var time = 0L
        val session = PhonePairing { time }
        val token = session.renew()
        time = 599_999L
        assertTrue(session.valid(token))
        time = 600_000L
        assertFalse(session.valid(token))
        assertEquals(0L, session.remainingSeconds())
    }
    @Test fun refreshAndCloseRevokePreviousCodes() {
        val session = PhonePairing()
        val first = session.renew()
        val second = session.renew()
        assertNotEquals(first, second)
        assertFalse(session.valid(first))
        assertTrue(session.valid(second))
        session.close()
        assertFalse(session.valid(second))
    }
    @Test fun closingDuringNetworkValidationPreventsCommit() {
        val session = PhonePairing()
        val token = session.renew()
        session.close()
        var saved = false
        try { session.authorized(token) { saved = true }; fail("A closed code must not save a calendar") }
        catch (_: PhonePairing.Expired) {}
        assertFalse(saved)
    }
    @Test fun aFreshCodeClearsThePreviousConnectionMessage() {
        val session = PhonePairing()
        session.renew()
        session.connected("Family")
        assertEquals("Family", session.connectedName())
        session.renew()
        assertEquals("", session.connectedName())
    }
}
