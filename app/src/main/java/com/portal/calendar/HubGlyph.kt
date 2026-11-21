package com.portal.calendar

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable

/** Small, consistent navigation symbols without font-dependent emoji. */
class HubGlyph(context: Context, private val kind: String, private val color: Int) : Drawable() {
    private val size = (22 * context.resources.displayMetrics.density).toInt()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = this@HubGlyph.color
        style = Paint.Style.STROKE
        strokeWidth = 1.7f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    override fun getIntrinsicWidth() = size
    override fun getIntrinsicHeight() = size
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter = filter }
    @Suppress("DEPRECATION") override fun getOpacity() = PixelFormat.TRANSLUCENT
    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.scale(bounds.width() / 24f, bounds.height() / 24f)
        val path = Path()
        when (kind) {
            "back" -> {
                path.moveTo(13f,5f); path.lineTo(6f,12f); path.lineTo(13f,19f)
                canvas.drawPath(path,paint); canvas.drawLine(6f,12f,21f,12f,paint)
            }
            "home" -> {
                path.moveTo(3f,11f); path.lineTo(12f,3f); path.lineTo(21f,11f)
                path.moveTo(5f,10f); path.lineTo(5f,21f); path.lineTo(10f,21f)
                path.lineTo(10f,15f); path.lineTo(14f,15f); path.lineTo(14f,21f)
                path.lineTo(19f,21f); path.lineTo(19f,10f); canvas.drawPath(path,paint)
            }
            "apps" -> for (x in listOf(4f,14f)) for (y in listOf(4f,14f))
                canvas.drawRoundRect(x,y,x+6,y+6,1.5f,1.5f,paint)
            "settings" -> {
                canvas.drawCircle(12f,12f,6.5f,paint); canvas.drawCircle(12f,12f,2.4f,paint)
                repeat(8) { canvas.save();canvas.rotate(it*45f,12f,12f);canvas.drawLine(12f,2f,12f,5f,paint);canvas.restore() }
            }
            "youtube" -> {
                canvas.drawRoundRect(2f,5f,22f,19f,4f,4f,paint)
                path.moveTo(10f,9f);path.lineTo(16f,12f);path.lineTo(10f,15f);path.close();canvas.drawPath(path,paint)
            }
            "radio" -> {
                canvas.drawRoundRect(3f,8f,21f,21f,3f,3f,paint)
                canvas.drawLine(5f,8f,17f,3f,paint);canvas.drawCircle(8f,14f,2f,paint)
                canvas.drawLine(14f,12f,18f,12f,paint);canvas.drawLine(14f,16f,18f,16f,paint)
            }
            "music" -> {
                path.moveTo(8f,17f);path.lineTo(8f,5f);path.lineTo(20f,3f);path.lineTo(20f,15f)
                path.moveTo(8f,9f);path.lineTo(20f,7f);canvas.drawPath(path,paint)
                canvas.drawOval(2f,16f,8f,21f,paint);canvas.drawOval(14f,14f,20f,19f,paint)
            }
            "camera" -> {
                path.moveTo(3f,7f);path.lineTo(7f,7f);path.lineTo(9f,4f);path.lineTo(15f,4f);path.lineTo(17f,7f);path.lineTo(21f,7f)
                path.lineTo(21f,21f);path.lineTo(3f,21f);path.close();canvas.drawPath(path,paint);canvas.drawCircle(12f,14f,4f,paint)
            }
            "weather" -> {
                canvas.drawCircle(8f,8f,4f,paint)
                path.moveTo(7f,20f);path.cubicTo(0f,20f,1f,12f,7f,12f);path.cubicTo(9f,6f,18f,8f,18f,13f)
                path.cubicTo(24f,13f,24f,20f,18f,20f);path.close();canvas.drawPath(path,paint)
            }
            "calendar" -> {
                canvas.drawRoundRect(3f,5f,21f,22f,3f,3f,paint)
                canvas.drawLine(3f,10f,21f,10f,paint);canvas.drawLine(8f,2f,8f,7f,paint);canvas.drawLine(16f,2f,16f,7f,paint)
                canvas.drawLine(9f,16f,15f,16f,paint);canvas.drawLine(12f,13f,12f,19f,paint)
            }
        }
        canvas.restore()
    }
}
