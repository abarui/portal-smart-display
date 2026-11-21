package com.portal.calendar

import android.content.Context
import biweekly.Biweekly
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.Semaphore

/** Calendar pairing and separately authorized shared lists. Neither exposes calendar secrets. */
class PhoneSetupServer(private val ctx: Context, private val store: ConfigStore,
    val pairing: PhonePairing = PhonePairing()) : NanoHTTPD(PORT) {
    private val connecting = Semaphore(1)
    val photos = PhotoSharing(java.io.File(ctx.filesDir, "portal-camera"))
    private val shoppingPrefs = ctx.getSharedPreferences("shopping_access", Context.MODE_PRIVATE)
    val shopping = ShoppingAccess(
        { shoppingPrefs.getString("token", "") ?: "" },
        { shoppingPrefs.edit().putString("token", it).commit(); Unit })

    override fun serve(s: IHTTPSession): Response = try {
        route(s)
    } catch (_: PhonePairing.Expired) {
        json(401, JSONObject().put("error", "This setup code has expired. Tap New code on the Portal, then scan again."))
    } catch (e: SecurityException) {
        json(401, JSONObject().put("error", e.message))
    } catch (e: IllegalArgumentException) {
        json(400, JSONObject().put("error", e.message ?: "Check the calendar link and try again."))
    } catch (_: Exception) {
        json(502, JSONObject().put("error", "Could not read this calendar. Check the subscription link and try again."))
    }

    private fun route(s: IHTTPSession): Response {
        if (s.method == Method.GET && s.uri in listOf("/", "/phone.css", "/phone.js", "/shopping", "/shopping.css", "/shopping.js", "/photo", "/photo.css", "/photo.js")) {
            val file = when (s.uri) { "/" -> "phone.html"; "/shopping" -> "shopping.html"; "/photo" -> "photo.html"; else -> s.uri.removePrefix("/") }
            val mime = when { file.endsWith(".css") -> "text/css"; file.endsWith(".js") -> "application/javascript"; else -> "text/html" }
            return headers(newFixedLengthResponse(Response.Status.OK, "$mime; charset=utf-8",
                ctx.assets.open(file).bufferedReader(Charsets.UTF_8).use { it.readText() }))
        }
        if (s.uri == "/api/photo") {
            if (s.method != Method.GET) return json(405, JSONObject().put("error", "Method not allowed"))
            return try {
                val bytes = photos.read(s.headers["x-portal-photo-key"])
                headers(newFixedLengthResponse(Response.Status.OK, "image/jpeg", bytes.inputStream(), bytes.size.toLong()))
            } catch (_: PhonePairing.Expired) {
                json(401, JSONObject().put("error", "This photo code has expired. Open Export photo on the Portal and scan again."))
            } catch (e: IllegalArgumentException) {
                json(404, JSONObject().put("error", e.message ?: "This photo is no longer available."))
            }
        }
        if (s.uri == "/api/shopping") return shoppingRoute(s)
        val token = s.headers["x-portal-token"]
        if (!pairing.valid(token)) throw PhonePairing.Expired()
        if (s.uri == "/api/session" && s.method == Method.GET) {
            val names = JSONArray()
            store.feeds().filter { it.kind == "calendar" }.forEach { names.put(it.name) }
            return json(200, JSONObject().put("seconds", pairing.remainingSeconds()).put("calendars", names))
        }
        if (s.uri != "/api/subscribe" || s.method != Method.POST)
            return json(404, JSONObject().put("error", "Not found"))
        val origin = s.headers["origin"]
        require(origin == null || origin == "http://${s.headers["host"]}") { "Open setup using the QR code on the Portal." }
        require(s.headers["content-type"]?.startsWith("application/json") == true) { "Send the calendar using the setup form." }
        val length = s.headers["content-length"]?.toIntOrNull() ?: 0
        require(length in 1..16_384) { "The calendar link is too long or missing." }
        val body = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = s.inputStream.read(body, read, length - read)
            require(n > 0) { "The connection was interrupted. Try again." }
            read += n
        }
        val data = try { JSONObject(String(body, Charsets.UTF_8)) } catch (_: Exception) {
            throw IllegalArgumentException("Check the calendar details and try again.")
        }
        val name = data.optString("name").trim()
        require(name.isNotEmpty() && name.length <= 80 && name.none { it.isISOControl() }) { "Give your calendar a name (up to 80 characters)." }
        val provider = data.optString("provider")
        val url = SubscriptionLink.normalize(data.optString("url"), provider)
        require(data.optBoolean("sharingAccepted")) { "Confirm the calendar-link sharing note before connecting." }
        if (!connecting.tryAcquire()) return json(429, JSONObject().put("error", "Another calendar is connecting. Try again in a moment."))
        try {
            // A repeat tap is idempotent and does not rename an existing calendar.
            var connectedName = store.feeds().firstOrNull { it.url == url }?.name
            val duplicate = connectedName != null
            if (!duplicate) validateCalendar(url, provider)
            pairing.authorized(token) {
                synchronized(store) {
                    val feeds = JSONArray(store.feedsJson())
                    connectedName = store.feeds().firstOrNull { it.url == url }?.name
                    if (connectedName == null) {
                        val color = when (provider) { "google" -> "#4FA3E3"; "outlook" -> "#8A69C8"; else -> "#2BB3A3" }
                        feeds.put(JSONObject().put("name", name).put("url", url).put("color", color)
                            .put("kind", "calendar").put("text", "white"))
                        store.saveJson(feeds.toString())
                        connectedName = name
                    }
                    pairing.connected(connectedName!!)
                }
            }
            App.instance.notifyConfigChanged()
            return json(200, JSONObject().put("ok", true).put("name", connectedName).put("alreadyConnected", duplicate))
        } finally { connecting.release() }
    }

    private fun shoppingRoute(s: IHTTPSession): Response = shopping.authorized(s.headers["x-portal-token"]) {
        require(store.featureEnabled("lists")) { "Shared lists are switched off on the Portal." }
        if (s.method == Method.POST) {
            require(s.headers["origin"] == null || s.headers["origin"] == "http://${s.headers["host"]}") { "Open the shared list using its Portal link." }
            require(s.headers["content-type"]?.substringBefore(';')?.trim() == "application/json") { "Use the shopping list form." }
            val length = s.headers["content-length"]?.toIntOrNull() ?: 0
            require(length in 1..8192) { "The list update is too large or missing." }
            val body = ByteArray(length)
            var offset = 0
            while (offset < length) {
                val n = s.inputStream.read(body, offset, length - offset)
                require(n > 0) { "The update was interrupted. Try again." }
                offset += n
            }
            val input = try { JSONObject(String(body, Charsets.UTF_8)) }
                catch (_: Exception) { throw IllegalArgumentException("Check the list update and try again.") }
            val action = input.optString("action")
            require(action in listOf("addList", "addItem", "setItemDone", "deleteItem")) { "This action is not available from the phone page." }
            val safe = JSONObject().put("action", action)
            fun field(key: String, max: Int): String {
                val value = input.optString(key).trim()
                require(value.isNotEmpty() && value.length <= max && value.none { it.isISOControl() }) { "Enter a valid $key (up to $max characters)." }
                return value
            }
            if (action == "addList") safe.put("name", field("name", 80))
            else safe.put("listId", field("listId", 80))
            if (action == "addItem") safe.put("text", field("text", 240))
            if (action == "setItemDone" || action == "deleteItem") safe.put("itemId", field("itemId", 80))
            if (action == "setItemDone") {
                require(input.opt("done") is Boolean) { "Choose whether the item is checked." }
                safe.put("done", input.getBoolean("done"))
            }
            FamilyLists.mutate(ctx, safe)
        } else if (s.method != Method.GET) return@authorized json(405, JSONObject().put("error", "Method not allowed"))
        val lists = JSONArray(FamilyLists.json(ctx))
        val publicLists = JSONArray()
        for (i in 0 until lists.length()) {
            val list = lists.getJSONObject(i)
            val items = list.getJSONArray("items")
            val publicItems = JSONArray()
            for (j in 0 until items.length()) {
                val item = items.getJSONObject(j)
                publicItems.put(JSONObject().put("id", item.getString("id"))
                    .put("text", item.getString("text")).put("done", item.optBoolean("done")))
            }
            publicLists.put(JSONObject().put("id", list.getString("id"))
                .put("name", list.getString("name")).put("items", publicItems))
        }
        json(200, JSONObject().put("lists", publicLists))
    }

    /** Bounded download, trusted provider redirects only, before touching stored feeds. */
    private fun validateCalendar(initialUrl: String, provider: String) {
        var url = initialUrl
        repeat(4) {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "PortalFamilyCalendar/1.0")
            try {
                val status = connection.responseCode
                if (status in 300..399) {
                    val target = connection.getHeaderField("Location") ?: throw IllegalArgumentException("The calendar link has moved. Copy a fresh link from your calendar settings.")
                    url = SubscriptionLink.normalize(URI(url).resolve(target).toString(), provider)
                } else {
                    require(status in 200..299) { "The calendar could not be opened. Check that calendar sharing is enabled and copy its subscription link again." }
                    require(connection.contentLengthLong <= MAX_FEED) { "This calendar is too large to connect from the phone page." }
                    val bytes = ByteArrayOutputStream()
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            require(bytes.size() + n <= MAX_FEED) { "This calendar is too large to connect from the phone page." }
                            bytes.write(buffer, 0, n)
                        }
                    }
                    val text = bytes.toString("UTF-8")
                    require(text.contains("BEGIN:VCALENDAR") && Biweekly.parse(text).first() != null) {
                        "This is not a calendar subscription. Copy the ICS or webcal link from your calendar settings."
                    }
                    return
                }
            } finally { connection.disconnect() }
        }
        throw IllegalArgumentException("The calendar link redirects too many times. Copy a fresh subscription link.")
    }

    private fun json(status: Int, value: JSONObject): Response = headers(newFixedLengthResponse(
        Response.Status.lookup(status), "application/json; charset=utf-8", value.toString()))

    private fun headers(response: Response): Response = response.apply {
        // Rejected requests can still have an unread body; never reuse that socket.
        addHeader("Connection", "close")
        addHeader("Cache-Control", "no-store")
        addHeader("Referrer-Policy", "no-referrer")
        addHeader("X-Content-Type-Options", "nosniff")
        addHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data: blob:; connect-src 'self'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'")
    }

    companion object { const val PORT = 8091; private const val MAX_FEED = 8 * 1024 * 1024 }
}
