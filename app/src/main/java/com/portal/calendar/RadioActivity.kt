package com.portal.calendar

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.*
import android.widget.*
import kotlin.math.abs

/** Portal-sized controls for the installed Transistor player's public media session. */
@Suppress("DEPRECATION")
class RadioActivity : Activity() {
    private data class Station(val id: String, val name: String) {
        val bengali get() = listOf("Bangla", "Kolkata", "Maitree").any { name.contains(it, true) }
        val shortName get() = name.removePrefix("AIR ")
        val detail get() = when {
            name.contains("News") -> "News & current affairs"
            name.contains("Raagam") -> "Indian classical music"
            bengali -> "Bengali · West Bengal"
            else -> "Hindi · New Delhi"
        }
        val artwork get() = when {
            name.contains("Bangla") -> "বাংলা"
            name.contains("News") -> "NEWS"
            name.contains("Raagam") -> "RAAGAM"
            name.contains("Gold") -> "GOLD"
            name.contains("Rainbow") -> "RAINBOW"
            name.contains("Maitree") -> "মৈত্রী"
            else -> "কলকাতা"
        }
        val badge get() = when {
            name.contains("Bangla") -> "বাং"
            name.contains("News") -> "24/7"
            name.contains("Raagam") -> "R"
            name.contains("Gold") -> "G"
            name.contains("Rainbow") -> "FM"
            name.contains("Maitree") -> "মৈ"
            name.contains("Sanchayita") -> "স"
            else -> "গী"
        }
        val tint get() = Color.parseColor(when {
            name.contains("Gold") -> if (Palette.dark) "#F2D295" else "#8B631F"
            name.contains("Rainbow") -> if (Palette.dark) "#CBC2F7" else "#7054AC"
            name.contains("News") -> if (Palette.dark) "#A8CEFA" else "#2265A3"
            name.contains("Raagam") -> if (Palette.dark) "#F1BFAF" else "#AB4E40"
            else -> if (Palette.dark) "#B2CDFF" else "#265FC7"
        })
    }
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("portal_radio", MODE_PRIVATE) }
    private val audio by lazy { getSystemService(AudioManager::class.java) }
    private var browser: MediaBrowser? = null
    private var controller: MediaController? = null
    private var stations = emptyList<Station>()
    private var selectedId = ""
    private var requestedId: String? = null
    private var playback: PlaybackState? = null
    private var connectionMessage = "Connecting to radio…"
    private var filter = "All stations"
    private var favorites = mutableSetOf<String>()
    private var wide = true
    private var builtDark = false
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var status: TextView
    private lateinit var artwork: RadioArtwork
    private lateinit var playButton: LinearLayout
    private lateinit var playIcon: RadioIcon
    private lateinit var playLabel: TextView
    private lateinit var favorite: TextView
    private lateinit var grid: LinearLayout
    private lateinit var stationCount: TextView
    private lateinit var volume: SeekBar
    private lateinit var volumeLabel: TextView
    private lateinit var filters: LinearLayout
    private val cards = mutableListOf<Triple<Station, LinearLayout, RadioIcon>>()
    private val sensorManager by lazy { getSystemService(SensorManager::class.java) }
    private var naturalPortrait = true
    private var pendingOrientation = 0
    private var orientationCount = 0
    private val idle = Runnable { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    private val tick = object : Runnable {
        override fun run() {
            if (builtDark != App.instance.store.isDark(this@RadioActivity)) buildScreen()
            else updateVolume()
            handler.postDelayed(this, 1000)
        }
    }
    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            playback = state
            if (state?.state in listOf(PlaybackState.STATE_PLAYING, PlaybackState.STATE_ERROR, PlaybackState.STATE_PAUSED)) requestedId = null
            refreshPlayer()
        }
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            val id = metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
            if (id != null && stations.any { it.id == id } && (requestedId == null || requestedId == id)) {
                selectedId = id; requestedId = null
                prefs.edit().putString("selected", id).apply()
            }
            refreshPlayer()
        }
        override fun onSessionDestroyed() { controller = null; connectionMessage = "Radio disconnected. Tap Retry."; refreshPlayer() }
    }
    private val sensor = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            if (App.instance.store.orientation() != "auto") return
            val x = abs(e.values[0]); val y = abs(e.values[1])
            if (x + y < 4.5f || abs(x - y) < 2f) return
            var portrait = if (x > y) !naturalPortrait else naturalPortrait
            if (App.instance.store.orientationSwap()) portrait = !portrait
            val desired = if (portrait) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            if (desired == requestedOrientation) { orientationCount = 0; return }
            if (desired != pendingOrientation) { pendingOrientation = desired; orientationCount = 1 }
            else if (++orientationCount >= 6) { requestedOrientation = desired; orientationCount = 0 }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val quarterTurn = windowManager.defaultDisplay.rotation in listOf(Surface.ROTATION_90, Surface.ROTATION_270)
        naturalPortrait = (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) != quarterTurn
        selectedId = prefs.getString("selected", "") ?: ""
        favorites = prefs.getStringSet("favorites", emptySet())!!.toMutableSet()
        applyOrientation()
        buildScreen()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }
    override fun onStart() { super.onStart(); connectPlayer() }
    override fun onResume() {
        super.onResume(); applyOrientation(); armIdle(); hideSystemUi()
        if (builtDark != App.instance.store.isDark(this)) buildScreen()
        handler.post(tick)
        if (App.instance.store.orientation() == "auto") sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(sensor, it, SensorManager.SENSOR_DELAY_UI)
        }
    }
    override fun onPause() {
        sensorManager.unregisterListener(sensor); handler.removeCallbacks(tick); handler.removeCallbacks(idle)
        super.onPause()
    }
    override fun onStop() {
        controller?.unregisterCallback(mediaCallback); controller = null
        browser?.disconnect(); browser = null
        super.onStop()
    }
    override fun onConfigurationChanged(config: Configuration) { super.onConfigurationChanged(config); buildScreen(); hideSystemUi() }
    override fun onUserInteraction() { super.onUserInteraction(); armIdle() }
    override fun onWindowFocusChanged(focus: Boolean) { super.onWindowFocusChanged(focus); if (focus) hideSystemUi() }
    override fun onBackPressed() { if (isTaskRoot) goHome() else finish() }

    private fun applyOrientation() {
        requestedOrientation = when (App.instance.store.orientation()) {
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }
    private fun armIdle() {
        handler.removeCallbacks(idle); window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Screensaver.isYield(this)) handler.postDelayed(idle, Screensaver.yieldMinutes(this) * 60_000L)
    }
    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }
    private fun connectPlayer() {
        controller?.unregisterCallback(mediaCallback); controller = null
        browser?.disconnect()
        connectionMessage = "Connecting to radio…"
        browser = MediaBrowser(this, ComponentName("org.y20k.transistor", "org.y20k.transistor.PlayerService"),
            object : MediaBrowser.ConnectionCallback() {
                override fun onConnected() {
                    val connected = browser ?: return
                    controller = MediaController(this@RadioActivity, connected.sessionToken).also {
                        it.registerCallback(mediaCallback, handler); playback = it.playbackState
                    }
                    connectionMessage = ""
                    connected.subscribe(connected.root, object : MediaBrowser.SubscriptionCallback() {
                        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowser.MediaItem>) {
                            stations = children.filter { it.isPlayable }.map { Station(it.mediaId ?: "", it.description.title?.toString() ?: "Radio station") }
                            if (!prefs.contains("favorites")) {
                                favorites = stations.filter { it.name.contains("Bangla", true) }.map { it.id }.toMutableSet()
                                prefs.edit().putStringSet("favorites", favorites.toSet()).apply()
                            }
                            val active = controller?.metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
                            if (active != null && stations.any { it.id == active }) selectedId = active
                            if (stations.none { it.id == selectedId }) selectedId = stations.firstOrNull()?.id ?: ""
                            renderStations(); refreshPlayer()
                        }
                        override fun onError(parentId: String) { connectionMessage = "Couldn’t load stations. Tap Retry."; refreshPlayer() }
                    })
                    refreshPlayer()
                }
                override fun onConnectionFailed() { connectionMessage = "Couldn’t connect to radio. Tap Retry."; refreshPlayer() }
                override fun onConnectionSuspended() { controller = null; connectionMessage = "Radio disconnected. Tap Retry."; refreshPlayer() }
            }, null).also { it.connect() }
    }
    private fun play(station: Station) {
        if (controller == null) { connectPlayer(); return }
        selectedId = station.id; requestedId = station.id
        prefs.edit().putString("selected", station.id).apply()
        controller?.transportControls?.playFromMediaId(station.id, null)
        refreshPlayer()
    }
    private fun togglePlay() {
        if (connectionMessage.isNotEmpty()) { connectPlayer(); return }
        if (playback?.state in listOf(PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING, PlaybackState.STATE_CONNECTING)) {
            requestedId = null; controller?.transportControls?.pause()
        } else stations.firstOrNull { it.id == selectedId }?.let { play(it) }
    }
    private fun skip(delta: Int) {
        if (stations.isEmpty()) return
        val index = stations.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        play(stations[(index + delta + stations.size) % stations.size])
    }

    private fun buildScreen() {
        builtDark = App.instance.store.isDark(this)
        Palette.dark = builtDark
        wide = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val root = column().apply {
            background = HubStyle.backdrop()
            // Portal's system Home/Back overlay occupies the top edge.
            setPadding(dp(24), dp(64), dp(24), dp(24))
        }
        val navigation = row()
        navigation.addView(navButton("back", "Back") { onBackPressed() }, LinearLayout.LayoutParams(dp(106), dp(52)))
        navigation.addView(navButton("home", "Home", primary = true) { goHome() },
            LinearLayout.LayoutParams(dp(114), dp(52)).apply { leftMargin = dp(8) })
        navigation.addView(label("Radio", 23f, INK, true).apply { setPadding(dp(24), 0, 0, 0) },
            LinearLayout.LayoutParams(0, WRAP, 1f))
        navigation.addView(label("ALL INDIA RADIO", 11f, MUTED, true).apply { letterSpacing = .16f })
        root.addView(navigation, lp(bottom = 20))

        val body = LinearLayout(this).apply { orientation = if (wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL }
        val player = buildPlayer()
        body.addView(player, if (wide) LinearLayout.LayoutParams(dp(350), MATCH)
            else LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(20) })
        val collection = column().apply {
            background = HubStyle.panel(this@RadioActivity)
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        val heading = row()
        heading.addView(label("Your stations", 24f, INK, true), LinearLayout.LayoutParams(0, WRAP, 1f))
        stationCount = label("", 13f, MUTED)
        heading.addView(stationCount)
        collection.addView(heading, lp(bottom = 14))
        filters = row().apply {
            background = rounded(Palette.withAlpha(EDGE, 150), 18)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        collection.addView(filters, LinearLayout.LayoutParams(WRAP, dp(52)).apply { bottomMargin = dp(14) })
        grid = column()
        collection.addView(ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(grid) }, LinearLayout.LayoutParams(MATCH, 0, 1f))
        body.addView(collection, if (wide) LinearLayout.LayoutParams(0, MATCH, 1f).apply { leftMargin = dp(20) }
            else LinearLayout.LayoutParams(MATCH, 0, 1f))
        root.addView(body, LinearLayout.LayoutParams(MATCH, 0, 1f))
        setContentView(root)
        renderFilters(); renderStations(); refreshPlayer(); updateVolume()
    }
    private fun buildPlayer(): LinearLayout {
        val panel = column().apply { background = HubStyle.panel(this@RadioActivity); setPadding(dp(24), dp(22), dp(24), dp(20)) }
        val top = row()
        top.addView(label("NOW PLAYING", 11f, MUTED, true).apply { letterSpacing = .16f }, LinearLayout.LayoutParams(0, WRAP, 1f))
        favorite = label("★", 25f, HIGHLIGHT).apply {
            gravity = Gravity.CENTER; minimumWidth = dp(44); minimumHeight = dp(40)
            setOnClickListener {
                if (selectedId.isNotEmpty()) {
                    if (!favorites.add(selectedId)) favorites.remove(selectedId)
                    prefs.edit().putStringSet("favorites", favorites.toSet()).apply()
                    renderStations(); refreshPlayer()
                }
            }
        }
        top.addView(favorite); panel.addView(top)
        val stationInfo = LinearLayout(this).apply { orientation = if (wide) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        artwork = RadioArtwork(this)
        stationInfo.addView(artwork, LinearLayout.LayoutParams(if (wide) MATCH else dp(116), if (wide) dp(176) else dp(108)).apply { if (wide) bottomMargin = dp(19) else rightMargin = dp(22) })
        val words = column()
        title = label("AIR Bangla", if (wide) 31f else 32f, INK, true).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END }
        subtitle = label("Bengali · West Bengal", 14f, MUTED)
        status = label("Connecting to radio…", 13f, HIGHLIGHT).apply { maxLines = 2; minHeight = dp(36); gravity = Gravity.CENTER_VERTICAL }
        words.addView(title); words.addView(subtitle, lp(top = 7)); words.addView(status, lp(top = 9))
        stationInfo.addView(words, if (wide) lp() else LinearLayout.LayoutParams(0, WRAP, 1f))
        panel.addView(stationInfo, if (wide) LinearLayout.LayoutParams(MATCH, 0, 1f) else lp(bottom = 12))
        val controls = row()
        controls.addView(iconButton("previous", "Previous station") { skip(-1) }, LinearLayout.LayoutParams(dp(54), dp(64)))
        playButton = row().apply { gravity = Gravity.CENTER; background = HubStyle.button(this@RadioActivity, primary = true); minimumHeight = dp(64); setOnClickListener { togglePlay() } }
        playIcon = RadioIcon(this, "play", Color.WHITE)
        playLabel = label("Play", 18f, Color.WHITE, true)
        playButton.addView(playIcon, LinearLayout.LayoutParams(dp(26), dp(26)).apply { rightMargin = dp(10) }); playButton.addView(playLabel)
        controls.addView(playButton, LinearLayout.LayoutParams(0, dp(64), 1f).apply { leftMargin = dp(7); rightMargin = dp(7) })
        controls.addView(iconButton("next", "Next station") { skip(1) }, LinearLayout.LayoutParams(dp(54), dp(64)))
        panel.addView(controls, lp(top = if (wide) 16 else 0))
        val volumeRow = row()
        volumeRow.addView(RadioIcon(this, "volume", MUTED), LinearLayout.LayoutParams(dp(22), dp(22)))
        volume = SeekBar(this).apply {
            max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            progressTintList = ColorStateList.valueOf(ACCENT); thumbTintList = ColorStateList.valueOf(ACCENT)
            progressBackgroundTintList = ColorStateList.valueOf(EDGE)
            contentDescription = "Radio volume"
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
                    volumeLabel.text = "${if (max > 0) value * 100 / max else 0}%"
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        }
        volumeLabel = label("", 12f, MUTED).apply { gravity = Gravity.END; minWidth = dp(34) }
        volumeRow.addView(volume, LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(8); rightMargin = dp(5) })
        volumeRow.addView(volumeLabel)
        panel.addView(volumeRow, lp(top = if (wide) 16 else 4))
        return panel
    }
    private fun renderFilters() {
        filters.removeAllViews()
        listOf("All stations", "Bengali", "Favorites").forEach { text ->
            filters.addView(label(text, 14f, if (filter == text) Color.WHITE else MUTED, true).apply {
                gravity = Gravity.CENTER; minHeight = dp(44); setPadding(dp(17), 0, dp(17), 0)
                background = rounded(if (filter == text) ACCENT else Color.TRANSPARENT, 16)
                setOnClickListener { filter = text; renderFilters(); renderStations() }
            }, LinearLayout.LayoutParams(WRAP, dp(44)))
        }
    }
    private fun renderStations() {
        if (!::grid.isInitialized) return
        grid.removeAllViews(); cards.clear()
        val shown = stations.filter { filter != "Bengali" || it.bengali }.filter { filter != "Favorites" || it.id in favorites }
        stationCount.text = "${shown.size} ${if (shown.size == 1) "station" else "stations"}"
        if (shown.isEmpty()) {
            grid.addView(label(if (stations.isEmpty()) "Your stations will appear here." else "Tap the star on a station to save it here.", 16f, MUTED), lp(top = 30)); return
        }
        shown.chunked(2).forEach { pair ->
            val line = row()
            pair.forEachIndexed { i, station ->
                val card = row().apply {
                    gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(14), dp(12), dp(14))
                    background = rounded(PANEL, 18); contentDescription = "Play ${station.name}"
                    setOnClickListener { if (station.id == selectedId) togglePlay() else play(station) }
                }
                card.addView(label(station.badge, if (station.badge.length > 2) 13f else 21f, station.tint, true).apply {
                    gravity = Gravity.CENTER; background = rounded(Color.argb(23, Color.red(station.tint), Color.green(station.tint), Color.blue(station.tint)), 14)
                }, LinearLayout.LayoutParams(dp(52), dp(56)).apply { rightMargin = dp(12) })
                val words = column()
                words.addView(label(station.shortName, 17f, INK, true).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END })
                words.addView(label(if (station.bengali) "Bengali" else if (station.name.contains("Raagam")) "Classical" else "Hindi / English", 12f, MUTED), lp(top = 5))
                card.addView(words, LinearLayout.LayoutParams(0, WRAP, 1f))
                val icon = RadioIcon(this, "play", MUTED)
                card.addView(icon, LinearLayout.LayoutParams(dp(23), dp(23)).apply { leftMargin = dp(8) })
                line.addView(card, LinearLayout.LayoutParams(0, dp(88), 1f).apply { if (i == 0) rightMargin = dp(12) })
                cards.add(Triple(station, card, icon))
            }
            if (pair.size == 1) line.addView(View(this), LinearLayout.LayoutParams(0, dp(88), 1f))
            grid.addView(line, lp(bottom = 10))
        }
        refreshPlayer()
    }
    private fun refreshPlayer() {
        if (!::title.isInitialized) return
        val station = stations.firstOrNull { it.id == selectedId }
        val state = playback?.state ?: PlaybackState.STATE_NONE
        val playing = state == PlaybackState.STATE_PLAYING
        val buffering = requestedId != null || state in listOf(PlaybackState.STATE_BUFFERING, PlaybackState.STATE_CONNECTING)
        if (station != null) {
            title.text = station.name; subtitle.text = station.detail
            artwork.word = station.artwork; artwork.accent = station.tint; artwork.invalidate()
        }
        status.text = when {
            connectionMessage.isNotEmpty() -> connectionMessage
            state == PlaybackState.STATE_ERROR -> "Station unavailable. Tap Retry or choose another."
            buffering -> "Connecting to the live broadcast…"
            playing -> "●  Playing live"
            state == PlaybackState.STATE_PAUSED -> "Paused · Ready when you are"
            else -> "Live radio · Tap Play to listen"
        }
        status.setTextColor(if (state == PlaybackState.STATE_ERROR) Color.parseColor(if (Palette.dark) "#F1BFAF" else "#AB4E40") else HIGHLIGHT)
        val retry = connectionMessage.isNotEmpty() && controller == null || state == PlaybackState.STATE_ERROR
        playIcon.kind = if ((playing || buffering) && !retry) "pause" else "play"; playIcon.invalidate()
        playLabel.text = if (retry) "Retry" else if (playing || buffering) "Pause" else "Play"
        playButton.contentDescription = "${playLabel.text} ${station?.name ?: "radio"}"
        favorite.text = if (selectedId in favorites) "★" else "☆"
        favorite.contentDescription = if (selectedId in favorites) "Remove from favorites" else "Add to favorites"
        cards.forEach { (entry, card, icon) ->
            val selected = entry.id == selectedId
            card.background = rounded(if (selected) SELECTED else PANEL, 18, if (selected) ACCENT else Color.TRANSPARENT)
            icon.kind = if (selected && (playing || buffering) && !retry) "pause" else "play"
            icon.tint = if (selected) HIGHLIGHT else MUTED; icon.invalidate()
            card.contentDescription = "${if (selected && playing) "Pause" else "Play"} ${entry.name}"
        }
    }
    private fun updateVolume() {
        if (::volume.isInitialized && !volume.isPressed) {
            val value = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            volume.progress = value; volumeLabel.text = "${if (volume.max > 0) value * 100 / volume.max else 0}%"
        }
    }
    private fun navButton(icon: String, text: String, primary: Boolean = false, click: () -> Unit) =
        label(text, 15f, if (primary) Color.WHITE else INK, true).apply {
            gravity = Gravity.CENTER; background = HubStyle.button(this@RadioActivity, primary); contentDescription = text
            maxLines = 1; setPadding(dp(16), dp(10), dp(16), dp(10))
            setCompoundDrawablesWithIntrinsicBounds(HubGlyph(this@RadioActivity, icon, if (primary) Color.WHITE else INK), null, null, null)
            compoundDrawablePadding = dp(9)
            setOnClickListener { click() }
        }
    private fun iconButton(icon: String, description: String, click: () -> Unit) = RadioIcon(this, icon, INK).apply {
        contentDescription = description; background = HubStyle.button(this@RadioActivity); setPadding(dp(15), dp(18), dp(15), dp(18)); setOnClickListener { click() }
    }
    private fun label(text: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color); includeFontPadding = false
        typeface = Typeface.create(if (bold) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
    }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun lp(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(top); bottomMargin = dp(bottom) }
    private fun rounded(color: Int, radius: Int, stroke: Int = Color.TRANSPARENT) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
    }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    companion object {
        private const val MATCH = -1; private const val WRAP = -2
        private val PANEL get() = Palette.CARD
        private val EDGE get() = Palette.PILL
        private val SELECTED get() = Palette.TODAY_CARD
        private val INK get() = Palette.INK
        private val MUTED get() = Palette.MUTED
        private val ACCENT get() = Palette.ACCENT
        private val HIGHLIGHT get() = if (Palette.dark) 0xFFB2CDFF.toInt() else Palette.ACCENT
    }
}

private class RadioIcon(context: Context, var kind: String, var tint: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val available = minOf(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom).toFloat()
        canvas.save(); canvas.translate((width - available) / 2f, (height - available) / 2f); canvas.scale(available / 24f, available / 24f)
        paint.color = tint; paint.strokeWidth = 1.8f; paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
        val p = Path()
        when (kind) {
            "pause" -> { paint.style = Paint.Style.FILL; canvas.drawRoundRect(6f,4f,10f,20f,1f,1f,paint); canvas.drawRoundRect(14f,4f,18f,20f,1f,1f,paint) }
            "play" -> { paint.style = Paint.Style.FILL; p.moveTo(7f,4f); p.lineTo(20f,12f); p.lineTo(7f,20f); p.close(); canvas.drawPath(p,paint) }
            "back" -> { paint.style = Paint.Style.STROKE; p.moveTo(13f,5f); p.lineTo(6f,12f); p.lineTo(13f,19f); canvas.drawPath(p,paint); canvas.drawLine(6f,12f,21f,12f,paint) }
            "home" -> { paint.style = Paint.Style.STROKE; p.moveTo(3f,11f); p.lineTo(12f,3f); p.lineTo(21f,11f); p.moveTo(5f,10f); p.lineTo(5f,21f); p.lineTo(10f,21f); p.lineTo(10f,15f); p.lineTo(14f,15f); p.lineTo(14f,21f); p.lineTo(19f,21f); p.lineTo(19f,10f); canvas.drawPath(p,paint) }
            "previous", "next" -> { if (kind == "previous") { canvas.translate(24f,0f); canvas.scale(-1f,1f) }; paint.style = Paint.Style.FILL; p.moveTo(4f,5f); p.lineTo(16f,12f); p.lineTo(4f,19f); p.close(); canvas.drawPath(p,paint); canvas.drawRoundRect(18f,5f,21f,19f,1f,1f,paint) }
            "volume" -> { paint.style = Paint.Style.STROKE; p.moveTo(3f,9f); p.lineTo(7f,9f); p.lineTo(12f,5f); p.lineTo(12f,19f); p.lineTo(7f,15f); p.lineTo(3f,15f); p.close(); canvas.drawPath(p,paint); canvas.drawArc(9f,3f,23f,21f,-55f,110f,false,paint); canvas.drawArc(12f,7f,19f,17f,-55f,110f,false,paint) }
        }
        canvas.restore()
    }
}

private class RadioArtwork(context: Context) : View(context) {
    var word = "বাংলা"
    var accent = Color.parseColor("#B2CDFF")
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val clip = Path().apply { addRoundRect(RectF(0f,0f,w,h),22f,22f,Path.Direction.CW) }
        canvas.save(); canvas.clipPath(clip)
        paint.style = Paint.Style.FILL; paint.shader = LinearGradient(0f,0f,w,h,Color.parseColor("#365F9B"),Color.parseColor("#54426F"),Shader.TileMode.CLAMP)
        canvas.drawRect(0f,0f,w,h,paint); paint.shader = null
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.4f; paint.color = Color.argb(42,255,255,255)
        for (i in 0..6) canvas.drawCircle(w*.84f,h*.88f,h*.25f+i*h*.18f,paint)
        paint.style = Paint.Style.FILL; paint.color = Color.WHITE; paint.typeface = Typeface.create("sans-serif-medium",Typeface.NORMAL)
        paint.textAlign = Paint.Align.CENTER; paint.textSize = if (w < 160) 23f else if (word.length > 5) 32f else 46f
        canvas.drawText(word,w/2f,h*.53f,paint)
        paint.textSize = if (w < 160) 8f else 10f
        canvas.drawText("AKASHVANI",w/2f,h*.73f,paint)
        canvas.restore()
    }
}
