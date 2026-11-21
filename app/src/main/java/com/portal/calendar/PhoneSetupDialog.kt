package com.portal.calendar

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** Opens only from the Portal, so a person at the display controls pairing. */
class PhoneSetupDialog(ctx: Context, private val shoppingMode: Boolean = false) : Dialog(ctx) {
    private val pairing = App.instance.phoneSetup.pairing
    private val handler = Handler(Looper.getMainLooper())
    private val qr = ImageView(ctx)
    private val status = TextView(ctx)
    private val hint = TextView(ctx)
    private val tick = object : Runnable {
        override fun run() {
            val connected = pairing.connectedName()
            val seconds = pairing.remainingSeconds()
            status.text = when {
                connected.isNotEmpty() -> "Connected: $connected\nYour calendar is syncing to the display."
                seconds <= 0 -> "Code expired. Tap New code to try again."
                else -> "Code expires in ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
            }
            if (seconds <= 0) qr.alpha = 0.12f
            handler.postDelayed(this, 1000)
        }
    }

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(24), dp(28), dp(20))
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(22).toFloat() }
        }
        card.addView(label(if (shoppingMode) "Shared shopping lists" else "Add a calendar", 25f, "#292E38", true), row())
        card.addView(label("Scan with your phone’s camera", 17f, "#292E38"), row(6))
        card.addView(label("Keep your phone and Portal on the same Wi-Fi.", 14f, "#737983"), row(6))
        qr.contentDescription = if (shoppingMode) "Scan this QR code to use shared lists on your phone" else "Scan this QR code with your phone to connect a calendar"
        qr.setPadding(dp(10), dp(10), dp(10), dp(10))
        qr.setBackgroundColor(Color.WHITE)
        card.addView(qr, LinearLayout.LayoutParams(dp(250), dp(250)).apply { topMargin = dp(18) })
        hint.apply { textSize = 14f; gravity = Gravity.CENTER; setTextColor(Color.parseColor("#737983")) }
        card.addView(hint, row(8))
        status.apply { textSize = 15f; gravity = Gravity.CENTER; setTextColor(Color.parseColor("#2B8B7F")) }
        card.addView(status, row(12))
        val buttons = LinearLayout(ctx).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        buttons.addView(button(if (shoppingMode) "Reset link" else "New code") {
            if (shoppingMode) App.instance.phoneSetup.shopping.renew()
            renew()
        })
        buttons.addView(button("Done") { dismiss() })
        card.addView(buttons, row(16))
        val scroll = ScrollView(ctx).apply { isFillViewport = true; addView(card) }
        setContentView(scroll)
        setOnDismissListener { handler.removeCallbacks(tick); if (!shoppingMode) pairing.close() }
    }

    override fun show() {
        super.show()
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window?.setLayout(minOf(dp(510), context.resources.displayMetrics.widthPixels - dp(36)),
            WindowManager.LayoutParams.WRAP_CONTENT)
        renew()
        handler.removeCallbacks(tick)
        if (!shoppingMode) handler.post(tick)
    }

    private fun renew() {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wifi.connectionInfo?.ipAddress ?: 0
        if (ip == 0 || !App.instance.phoneSetupReady) {
            if (!shoppingMode) pairing.close()
            qr.setImageDrawable(null)
            hint.text = if (ip == 0) "Connect the Portal to Wi-Fi, then tap New code." else "Phone setup could not start. Reopen PortalHub and try again."
            return
        }
        val host = "${ip and 255}.${ip shr 8 and 255}.${ip shr 16 and 255}.${ip shr 24 and 255}"
        val url = if (shoppingMode) "http://$host:${PhoneSetupServer.PORT}/shopping#key=${App.instance.phoneSetup.shopping.token()}"
            else "http://$host:${PhoneSetupServer.PORT}/#key=${pairing.renew()}"
        val size = dp(250)
        val matrix = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 4))
        val pixels = IntArray(size * size) { n -> if (matrix[n % size, n / size]) Color.BLACK else Color.WHITE }
        qr.setImageBitmap(Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888))
        qr.alpha = 1f
        hint.text = if (shoppingMode) "Save the page on your phone. Add items and check them off together on your home Wi-Fi."
            else "Choose a calendar on your phone.\nNo Apple password needed for subscription links."
        if (shoppingMode) status.text = "Anyone with this link on your Wi-Fi can edit shared lists.\nReset link disconnects previously linked phones."
    }

    private fun label(text: String, size: Float, color: String, bold: Boolean = false) = TextView(context).apply {
        this.text = text; textSize = size; gravity = Gravity.CENTER; setTextColor(Color.parseColor(color))
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private fun button(text: String, click: () -> Unit) = label(text, 16f, "#F0584C", true).apply {
        minimumHeight = dp(48); minimumWidth = dp(130); setPadding(dp(14), 0, dp(14), 0)
        setOnClickListener { click() }
    }
    private fun row(top: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) }
    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
}
