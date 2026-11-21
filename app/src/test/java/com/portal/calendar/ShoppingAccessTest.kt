package com.portal.calendar

import org.junit.Assert.*
import org.junit.Test

class ShoppingAccessTest {
    @Test fun linkIsCreatedOnRequestAndSurvivesRestart() {
        var saved = ""
        val access = ShoppingAccess({ saved }, { saved = it })
        assertFalse(access.valid(null))
        assertFalse(access.valid(""))
        val token = access.token()
        assertEquals(32, token.length)
        assertEquals(token, access.token())
        assertTrue(ShoppingAccess({ saved }, { saved = it }).valid(token))
    }
    @Test fun rotatingLinkRevokesAccessBeforeMutation() {
        var saved = ""
        val access = ShoppingAccess({ saved }, { saved = it })
        val old = access.token()
        val current = access.renew()
        assertFalse(access.valid(old))
        assertFalse(access.valid(current + "x"))
        var changed = false
        try { access.authorized(old) { changed = true }; fail("Old link must not mutate lists") }
        catch (_: SecurityException) {}
        assertFalse(changed)
        access.authorized(current) { changed = true }
        assertTrue(changed)
    }
}
