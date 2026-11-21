package com.portal.calendar

import org.junit.Assert.*
import org.junit.Test

class SubscriptionLinkTest {
    @Test fun normalizesAnIphoneWebcalLink() {
        assertEquals("https://p123-caldav.icloud.com/published/2/example",
            SubscriptionLink.normalize(" webcal://p123-caldav.icloud.com/published/2/example ", "icloud"))
    }
    @Test fun acceptsGoogleAndOutlookSubscriptions() {
        val google = "https://calendar.google.com/calendar/ical/example/private-abc/basic.ics"
        assertEquals(google, SubscriptionLink.normalize(google, "google"))
        val outlook = "https://outlook.office365.com/owa/calendar/example/calendar.ics"
        assertEquals(outlook, SubscriptionLink.normalize(outlook, "outlook"))
    }
    @Test fun rejectsLocalServersPlainHttpCredentialsAndProviderLookalikes() {
        val invalid = listOf(
            "http://p123-caldav.icloud.com/published/2/example",
            "https://127.0.0.1/published/2/example",
            "https://192.168.4.1/published/2/example",
            "https://[::1]/published/2/example",
            "https://p123-caldav.icloud.com.evil.example/published/2/example",
            "https://user:password@p123-caldav.icloud.com/published/2/example",
            "https://p123-caldav.icloud.com:8090/published/2/example",
            "https://p123-caldav.icloud.com/published/2/example#secret",
            "https://p123-caldav.icloud.com/private/calendar",
            "javascript:alert(1)"
        )
        for (url in invalid) {
            try { SubscriptionLink.normalize(url, "icloud"); fail("Accepted an unsafe or non-subscription URL: $url") }
            catch (_: IllegalArgumentException) {}
        }
    }
    @Test fun rejectsBrowserCalendarAddressesAndWrongProvider() {
        for ((url, provider) in listOf(
            "https://calendar.google.com/calendar/u/0/r" to "google",
            "https://p123-caldav.icloud.com/published/2/example" to "google",
            "https://calendar.google.com/calendar/ical/example/public/basic.ics" to "other"
        )) {
            try { SubscriptionLink.normalize(url, provider); fail("Expected a provider-specific subscription URL") }
            catch (_: IllegalArgumentException) {}
        }
    }
}
