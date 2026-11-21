package com.portal.calendar

import java.net.URI
import java.util.Locale

/** Only fetch subscription endpoints of the providers offered by the phone UI. */
object SubscriptionLink {
    fun normalize(raw: String, provider: String): String {
        val text = raw.trim().replaceFirst(Regex("^webcal://", RegexOption.IGNORE_CASE), "https://")
        val uri = try { URI(text) } catch (_: Exception) {
            throw IllegalArgumentException("Paste the complete calendar subscription link.")
        }
        require(text.length <= 4096 && uri.scheme == "https" && uri.rawUserInfo == null &&
            (uri.port == -1 || uri.port == 443) && uri.fragment == null) {
            "Use the HTTPS or webcal subscription link from your calendar provider."
        }
        val host = uri.host?.lowercase(Locale.ROOT) ?: ""
        val path = uri.path ?: ""
        val allowed = when (provider) {
            "icloud" -> host.matches(Regex("(?:p[0-9]+-)?caldav\\.icloud\\.com")) && path.startsWith("/published/")
            "google" -> host == "calendar.google.com" && path.startsWith("/calendar/ical/") && path.endsWith(".ics")
            "outlook" -> host in listOf("outlook.live.com", "outlook.office.com", "outlook.office365.com") &&
                (path.startsWith("/owa/calendar/") || path.startsWith("/calendar/")) && path.endsWith(".ics")
            else -> false
        }
        require(allowed) { "That is not a ${providerName(provider)} subscription link. Follow the steps above and copy the calendar link again." }
        return uri.toASCIIString()
    }

    private fun providerName(provider: String) = when (provider) {
        "icloud" -> "valid iCloud"
        "google" -> "valid Google Calendar"
        "outlook" -> "valid Outlook"
        else -> "supported calendar"
    }
}
