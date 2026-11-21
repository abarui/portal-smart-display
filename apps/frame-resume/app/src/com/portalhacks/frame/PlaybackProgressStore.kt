package com.portalhacks.frame

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import java.security.MessageDigest

/** Private on-device bookmarks, separate from albums and uploaded photos. */
internal class PlaybackProgressStore(context: Context) {
    private val settings = context.getSharedPreferences(ConfigReceiver.PREFS, Context.MODE_PRIVATE)

    // The potentially large queue is written only when it changes. Each photo updates the
    // small position file, rather than rewriting the album cache or queue every six seconds.
    private val queue = context.getSharedPreferences("resume_queue", Context.MODE_PRIVATE)
    private val position = context.getSharedPreferences("resume_position", Context.MODE_PRIVATE)
    private var savedOrder = readOrder()
    private val identifiers = mutableMapOf<String, String>()

    private fun enabled(): Boolean = settings.getBoolean(ConfigReceiver.KEY_RESUME, true)

    private fun key(id: String): String =
        identifiers.getOrPut(id) {
            MessageDigest.getInstance("SHA-256").digest(id.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }

    private fun bookmark(): PlaybackPosition = PlaybackPosition(savedOrder, position.getString("current", null))

    fun hasMissingBookmark(slides: List<Slide>): Boolean =
        enabled() && bookmark().current?.let { current -> slides.none { key(it.id) == current } } == true

    fun restore(
        slides: List<Slide>,
        shuffle: Boolean,
    ): Pair<List<Slide>, Int> {
        if (!enabled()) return slides to 0
        val saved = bookmark()
        val byId = slides.associateBy { key(it.id) }
        val sameMode = queue.getBoolean("shuffle", shuffle) == shuffle
        val order = saved.ordered(byId.keys.toList(), shuffle && sameMode)
        return order.mapNotNull { byId[it] } to saved.startIndex(order)
    }

    /** Call only after a photo becomes visible, never for failed or cancelled loads. */
    fun record(
        slides: List<Slide>,
        index: Int,
        shuffle: Boolean,
    ) {
        if (!enabled() || index !in slides.indices) return
        val order = slides.map { key(it.id) }
        if (order != savedOrder || queue.getBoolean("shuffle", !shuffle) != shuffle) {
            queue.edit().putString("order", JSONArray(order).toString())
                .putBoolean("shuffle", shuffle).apply()
            savedOrder = order
        }
        val current = order[index]
        position.edit().putString("current", current).apply()
        // A one-way fingerprint allows ADB playback checks without revealing photo/album URLs.
        Log.i("FrameResumePlayback", "displayed=${current.take(16)} position=$index total=${order.size}")
    }

    private fun readOrder(): List<String> =
        try {
            val array = JSONArray(queue.getString("order", "[]") ?: "[]")
            (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotEmpty) }
        } catch (_: JSONException) {
            emptyList()
        }
}
