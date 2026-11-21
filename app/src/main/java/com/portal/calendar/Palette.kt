package com.portal.calendar

/**
 * The design language, in one place. Cool glass panels, a blue accent,
 * and preserved per-calendar color fills with white text
 * (or black/auto per calendar — see [textOn]).
 *
 * Patterns the screens follow:
 *  - Section headers: 13sp ACCENT sans-serif-medium MICROCAPS (letterSpacing .14)
 *  - Containers (columns, panels): CARD fill, radius [R_CARD], elevation 1–2dp
 *  - Elements inside containers (rows, kid cards): radius [R_ELEMENT]
 *  - Buttons: pills — PILL fill + INK text, or ACCENT fill + white text for
 *    the primary action; radius [R_PILL]
 *  - Overlays: CARD card radius [R_OVERLAY] on a [SCRIM] scrim, elevation 10dp
 */
object Palette {
    /**
     * Whether the board renders dark. Set ONCE per board build (the board
     * rebuilds on a theme change), before any view is created. Every color
     * below is a getter so it reflects the current value live — the companion
     * color aliases in the screens are getters too, for the same reason.
     */
    var dark = false

    // light value ... dark value. Never pure #000/#FFF (harsh on this panel).
    val BG get() = pick(0xFFE9EFF9, 0xFF101A2F)
    val CARD get() = pick(0xFFF8FAFF, 0xFF1B2941)
    val CARD_SOFT get() = pick(0xFFF0F4FC, 0xFF253550)
    val TODAY_CARD get() = pick(0xFFE8F1FF, 0xFF213B60)
    val OUT_CARD get() = pick(0xFFE8EDF6, 0xFF152238)
    val PILL get() = pick(0xFFE7EDF7, 0xFF2B3C57)
    val INK get() = pick(0xFF202C43, 0xFFF4F7FF)
    val MUTED get() = pick(0xFF52647F, 0xFFB4C3DA)
    val FAINT get() = pick(0xFF6A7C97, 0xFF8D9FB9)
    val ACCENT = 0xFF286CEB.toInt()
    val LINE get() = pick(0xFFD7E1EF, 0xFF354761)
    val FIELD get() = pick(0xFFF1F5FC, 0xFF18263D)
    val FIELD_STROKE get() = pick(0xFFCCD8EA, 0xFF405575)
    val SCRIM get() = if (dark) 0x99000000.toInt() else 0x59000000

    private fun pick(light: Long, dark2: Long): Int = (if (dark) dark2 else light).toInt()

    // ---- text on a solid calendar color ------------------------------------
    // The one place that decides white vs black text for a calendar's color.
    // config.html mirrors [autoTextOn] for its live preview — keep them in step.

    /** WCAG 2.x relative luminance of an sRGB color (alpha ignored), 0 = black .. 1 = white. */
    fun luminance(color: Int): Double {
        fun lin(channel: Int): Double {
            val s = channel / 255.0
            return if (s <= 0.04045) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * lin((color shr 16) and 0xFF) +
            0.7152 * lin((color shr 8) and 0xFF) +
            0.0722 * lin(color and 0xFF)
    }

    /** Black or white, whichever has the higher WCAG contrast ratio against [fill]; a tie goes to white. */
    fun autoTextOn(fill: Int): Int {
        val l = luminance(fill)
        val vsWhite = 1.05 / (l + 0.05)
        val vsBlack = (l + 0.05) / 0.05
        return if (vsBlack > vsWhite) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }

    /** Text color on [fill] for a calendar's `text` setting: "black", "auto", else white. */
    fun textOn(fill: Int, setting: String): Int = when (setting) {
        "black" -> android.graphics.Color.BLACK
        "auto" -> autoTextOn(fill)
        else -> android.graphics.Color.WHITE
    }

    /** [color] at the given 0..255 alpha — for the secondary line on a colored chip. */
    fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)

    const val R_CARD = 16
    const val R_ELEMENT = 12
    const val R_PILL = 20
    const val R_OVERLAY = 20
}
