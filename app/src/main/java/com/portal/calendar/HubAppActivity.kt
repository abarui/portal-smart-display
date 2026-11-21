package com.portal.calendar

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.*
import android.os.*
import android.view.*
import android.widget.*
import kotlin.math.abs

/** Shared presentation and Portal rotation behavior for companion apps. */
@Suppress("DEPRECATION")
abstract class HubAppActivity : Activity() {
    protected val ui = Handler(Looper.getMainLooper())
    protected val wide get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    private val sensors by lazy { getSystemService(SensorManager::class.java) }
    private var naturalPortrait = true
    private var pending = 0
    private var count = 0
    private var builtDark = false
    private val idle = Runnable { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    private val sensor = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            if (App.instance.store.orientation() != "auto") return
            val x = abs(e.values[0]); val y = abs(e.values[1])
            if (x+y < 4.5f || abs(x-y) < 2f) return
            var portrait = if (x>y) !naturalPortrait else naturalPortrait
            if (App.instance.store.orientationSwap()) portrait = !portrait
            val desired = if (portrait) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            if (desired == requestedOrientation) { count=0; return }
            if (desired != pending) { pending=desired;count=1 }
            else if (++count>=6) { requestedOrientation=desired;count=0 }
        }
        override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
    }
    protected abstract fun renderScreen()
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        naturalPortrait = (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) !=
            (windowManager.defaultDisplay.rotation in listOf(Surface.ROTATION_90, Surface.ROTATION_270))
        applyOrientation(); refreshTheme(); renderScreen()
    }
    override fun onResume() {
        super.onResume(); applyOrientation(); armIdle(); immersive()
        if (builtDark != App.instance.store.isDark(this)) { refreshTheme();renderScreen() }
        if (App.instance.store.orientation()=="auto") sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensors.registerListener(sensor,it,SensorManager.SENSOR_DELAY_UI)
        }
    }
    override fun onPause() { sensors.unregisterListener(sensor);ui.removeCallbacks(idle);super.onPause() }
    override fun onConfigurationChanged(config: Configuration) { super.onConfigurationChanged(config);refreshTheme();renderScreen();immersive() }
    override fun onUserInteraction() { super.onUserInteraction();armIdle() }
    override fun onWindowFocusChanged(focus: Boolean) { super.onWindowFocusChanged(focus);if(focus) immersive() }
    override fun onBackPressed() { goHome() }
    protected fun goHome() {
        startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP));finish()
    }
    private fun refreshTheme() { builtDark=App.instance.store.isDark(this);Palette.dark=builtDark }
    private fun applyOrientation() {
        requestedOrientation=when(App.instance.store.orientation()) {
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> if(wide) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    private fun armIdle() {
        ui.removeCallbacks(idle);window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if(Screensaver.isYield(this)) ui.postDelayed(idle,Screensaver.yieldMinutes(this)*60_000L)
    }
    protected fun immersive() {
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
    protected fun shell(title: String, actions: List<View> = emptyList()): LinearLayout {
        val root=column().apply { background=HubStyle.backdrop();setPadding(dp(24),dp(64),dp(24),dp(24)) }
        val nav=row()
        nav.addView(button("Home","home",true) { goHome() },LinearLayout.LayoutParams(dp(114),dp(52)))
        nav.addView(label(title,23f,true).apply { setPadding(dp(24),0,dp(16),0) },LinearLayout.LayoutParams(0,WRAP,1f))
        actions.forEach { nav.addView(it,LinearLayout.LayoutParams(WRAP,dp(52)).apply { leftMargin=dp(8) }) }
        root.addView(nav,lp(bottom=20));return root
    }
    protected fun label(text: String, size: Float=16f, bold: Boolean=false, color: Int=Palette.INK)=TextView(this).apply {
        this.text=text;textSize=size;setTextColor(color);includeFontPadding=false
        typeface=Typeface.create(if(bold) "sans-serif-medium" else "sans-serif",Typeface.NORMAL)
    }
    protected fun button(text: String, icon: String?=null, primary: Boolean=false, action:()->Unit)=label(text,15f,true,if(primary) Color.WHITE else Palette.INK).apply {
        gravity=Gravity.CENTER;maxLines=1;minimumHeight=dp(50);setPadding(dp(16),dp(10),dp(16),dp(10))
        background=HubStyle.button(this@HubAppActivity,primary);contentDescription=text
        icon?.let { setCompoundDrawablesWithIntrinsicBounds(HubGlyph(this@HubAppActivity,it,if(primary) Color.WHITE else Palette.INK),null,null,null);compoundDrawablePadding=dp(9) }
        setOnClickListener { action() }
    }
    protected fun panel()=column().apply { background=HubStyle.panel(this@HubAppActivity);setPadding(dp(22),dp(22),dp(22),dp(22)) }
    protected fun styleDialog(dialog:AlertDialog) {
        // Portal's system dialog skin otherwise puts light text on a white panel.
        dialog.window?.setBackgroundDrawable(GradientDrawable().apply { setColor(Palette.CARD);cornerRadius=dp(28).toFloat() })
        val titleId=resources.getIdentifier("alertTitle","id","android")
        dialog.findViewById<TextView>(titleId)?.setTextColor(Palette.INK)
        dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(Palette.INK)
        listOf(AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL).forEach {
            dialog.getButton(it)?.setTextColor(if(Palette.dark) 0xFF89B5FF.toInt() else Palette.ACCENT)
        }
    }
    protected fun column()=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
    protected fun row()=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL }
    protected fun lp(top: Int=0,bottom: Int=0)=LinearLayout.LayoutParams(MATCH,WRAP).apply { topMargin=dp(top);bottomMargin=dp(bottom) }
    protected fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    protected companion object { const val MATCH=-1;const val WRAP=-2 }
}
