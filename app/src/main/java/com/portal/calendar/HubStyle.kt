package com.portal.calendar

import android.content.Context
import android.graphics.drawable.GradientDrawable

/** Shared surfaces keep the calendar hub and radio visually consistent. */
object HubStyle {
    fun backdrop() = GradientDrawable(GradientDrawable.Orientation.TL_BR,
        if (Palette.dark) intArrayOf(0xFF203A63.toInt(), 0xFF282442.toInt(), 0xFF101C30.toInt())
        else intArrayOf(0xFFDDEBFF.toInt(), 0xFFEAE1F5.toInt(), 0xFFE0EEF2.toInt()))

    fun panel(context: Context) = GradientDrawable(GradientDrawable.Orientation.TL_BR,
        if (Palette.dark) intArrayOf(0xD92A3C59.toInt(), 0xE51B263D.toInt())
        else intArrayOf(0xEEFFFFFF.toInt(), 0xD9F6F8FF.toInt())
    ).apply {
        cornerRadius = dp(context, 28).toFloat()
        setStroke(dp(context, 1), if (Palette.dark) 0x405E779C else 0xD9FFFFFF.toInt())
    }

    fun button(context: Context, primary: Boolean = false) = GradientDrawable().apply {
        cornerRadius = dp(context, 18).toFloat()
        setColor(if (primary) Palette.ACCENT else Palette.withAlpha(Palette.PILL, 190))
        if (!primary) setStroke(dp(context, 1), Palette.withAlpha(Palette.LINE, 160))
    }

    private fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()
}
