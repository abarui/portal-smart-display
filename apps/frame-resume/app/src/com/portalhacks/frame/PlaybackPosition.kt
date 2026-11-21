package com.portalhacks.frame

/** Photo identifiers only; no images or album credentials are stored here. */
internal data class PlaybackPosition(val order: List<String>, val current: String?) {
    /** Keep the saved shuffle queue, drop removed photos, and append newly added photos. */
    fun ordered(
        available: List<String>,
        keepOrder: Boolean,
    ): List<String> {
        val unique = available.distinct()
        if (!keepOrder) return unique
        val remaining = unique.toMutableSet()
        val kept = order.filter { remaining.remove(it) }
        return kept + unique.filter { it in remaining }
    }

    /** If the photo was removed, continue with the next surviving photo in the old queue. */
    fun startIndex(available: List<String>): Int {
        if (available.isEmpty()) return 0
        val direct = available.indexOf(current)
        if (direct >= 0) return direct
        val previous = order.indexOf(current)
        if (previous >= 0) {
            val positions = available.withIndex().associate { it.value to it.index }
            for (offset in 1..order.size) {
                val next = positions[order[(previous + offset) % order.size]]
                if (next != null) return next
            }
        }
        return 0
    }
}
