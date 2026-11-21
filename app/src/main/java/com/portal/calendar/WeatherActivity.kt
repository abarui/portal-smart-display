package com.portal.calendar

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class WeatherActivity : HubAppActivity() {
    private val worker=Executors.newSingleThreadExecutor()
    private var loading=false
    private var message=""
    override fun onCreate(state: Bundle?) { super.onCreate(state);refresh(false) }
    override fun onDestroy() { worker.shutdownNow();super.onDestroy() }
    private fun refresh(force:Boolean) {
        if(loading || !Weather.isConfigured(this)) return
        loading=true;message="Updating forecast…";renderScreen()
        worker.execute {
            val old=Weather.snapshot(this)
            val ok=Weather.maybeRefresh(this,force || old?.optJSONObject("hourly")?.has("is_day")!=true)
            ui.post {
                if(isFinishing || isDestroyed) return@post
                loading=false
                message=if(!ok && (force || Weather.snapshot(this)==null)) "Couldn’t update. Check Wi-Fi and try Refresh." else ""
                renderScreen()
            }
        }
    }
    private fun number(o:JSONObject?, key:String, suffix:String=""):String {
        val n=o?.optDouble(key,Double.NaN)?:Double.NaN
        return if(n.isFinite()) "${n.roundToInt()}$suffix" else "—"
    }
    override fun renderScreen() {
        val root=shell("Weather",listOf(
            button("Location") { chooseLocation() }.apply { isEnabled=!loading },
            button(Weather.unitSuffix(this)) {
                if(!loading) { Weather.set(this,null,null,null,if(Weather.unitSuffix(this)=="°C") "f" else "c");refresh(true) }
            }.apply { isEnabled=!loading },
            button(if(loading) "Updating…" else "Refresh") { refresh(true) }.apply { isEnabled=!loading }
        ))
        val data=Weather.snapshot(this)
        if(data==null) {
            val empty=panel().apply { gravity=Gravity.CENTER }
            empty.addView(label(if(Weather.isConfigured(this)) "Your forecast is on its way" else "Choose your weather location",28f,true))
            empty.addView(label(message.ifEmpty { "Search for a city to get started." },16f,false,Palette.MUTED),lp(top=18))
            empty.addView(button(if(Weather.isConfigured(this)) "Try again" else "Choose location",primary=true) {
                if(Weather.isConfigured(this)) refresh(true) else chooseLocation()
            },lp(top=22))
            root.addView(empty,LinearLayout.LayoutParams(MATCH,0,1f));setContentView(root);return
        }
        val zone=TimeZone.getTimeZone(data.optString("timezone","America/Indiana/Indianapolis"))
        fun fmt(pattern:String)=SimpleDateFormat(pattern,Locale.getDefault()).apply { timeZone=zone }
        val now=Date();val dayKey=fmt("yyyy-MM-dd").format(now)
        val daily=data.optJSONObject("daily")?:JSONObject()
        val dates=daily.optJSONArray("time")
        val todayIndex=(0 until (dates?.length()?:0)).firstOrNull { dates?.optString(it)==dayKey } ?: 0
        val cur=data.optJSONObject("current")
        val suffix=Weather.unitSuffix(this)
        fun dailyTemp(key:String,index:Int):String {
            val n=daily.optJSONArray(key)?.optDouble(index,Double.NaN)?:Double.NaN
            return if(n.isFinite()) "${n.roundToInt()}$suffix" else "—"
        }
        val body=LinearLayout(this).apply { orientation=if(wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL }
        val hero=panel()
        hero.addView(label(Weather.label(this).ifEmpty { "Your location" },22f,true).apply { maxLines=2 })
        val tempRow=row()
        tempRow.addView(label(number(cur,"temperature_2m",suffix),if(wide) 83f else 88f).apply {
            typeface=Typeface.create("sans-serif-light",Typeface.NORMAL);letterSpacing=-.045f
        },LinearLayout.LayoutParams(0,WRAP,1f))
        tempRow.addView(label(if(cur?.optInt("is_day",1)==0 && cur.optInt("weather_code") in 0..1) "☾" else Weather.emoji(cur?.optInt("weather_code")?:-1),45f))
        hero.addView(tempRow,lp(top=14))
        hero.addView(label(Weather.condition(cur?.optInt("weather_code")?:-1,cur?.optInt("is_day",1)==0),24f,true),lp(top=4))
        hero.addView(label("High ${dailyTemp("temperature_2m_max",todayIndex)}  ·  Low ${dailyTemp("temperature_2m_min",todayIndex)}",16f,false,Palette.MUTED),lp(top=10))
        val metrics=LinearLayout(this).apply { orientation=if(wide) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL }
        val units=data.optJSONObject("current_units")
        listOf("Feels like" to number(cur,"apparent_temperature",suffix),"Humidity" to number(cur,"relative_humidity_2m","%"),
            "Wind" to number(cur,"wind_speed_10m"," "+(units?.optString("wind_speed_10m","km/h")?:"km/h"))).forEach { (title,value) ->
            val m=column();m.addView(label(title,13f,false,Palette.MUTED));m.addView(label(value,23f,true),lp(top=6))
            metrics.addView(m,if(wide) lp(bottom=20) else LinearLayout.LayoutParams(0,WRAP,1f))
        }
        hero.addView(metrics,lp(top=26))
        val stamp=data.optLong("fetchedAt",0)
        hero.addView(label(message.ifEmpty { "Updated ${fmt("h:mm a · z").format(Date(stamp))}" },12f,false,Palette.MUTED),lp(top=18))
        hero.addView(label("Forecast by Open-Meteo",12f,false,Palette.FAINT),lp(top=8))
        body.addView(hero,if(wide) LinearLayout.LayoutParams(dp(340),MATCH).apply { rightMargin=dp(20) } else lp(bottom=18))
        val forecasts=column()
        val hoursPanel=panel();hoursPanel.addView(label("Next 24 hours",20f,true),lp(bottom=14))
        val hourRow=row();val hourly=data.optJSONObject("hourly");val times=hourly?.optJSONArray("time")
        val hourKey=fmt("yyyy-MM-dd'T'HH:00").format(now)
        val start=(0 until (times?.length()?:0)).firstOrNull { times?.optString(it).orEmpty()>=hourKey } ?: 0
        for(i in start until minOf(start+24,times?.length()?:0)) {
            val tile=column().apply { gravity=Gravity.CENTER_HORIZONTAL }
            val instant=runCatching { fmt("yyyy-MM-dd'T'HH:mm").parse(times?.optString(i).orEmpty()) }.getOrNull()
            val hour=instant?.let { fmt("h a").format(it) }?:"—"
            tile.addView(label(if(i==start) "Now" else hour,13f,false,Palette.MUTED))
            val code=hourly?.optJSONArray("weather_code")?.optInt(i)?:-1
            val night=hourly?.optJSONArray("is_day")?.optInt(i,1)==0
            tile.addView(label(if(night && code in 0..1) "☾" else Weather.emoji(code),24f),lp(top=8).apply { gravity=Gravity.CENTER_HORIZONTAL;width=WRAP })
            val v=hourly?.optJSONArray("temperature_2m")?.optDouble(i,Double.NaN)?:Double.NaN
            tile.addView(label(if(v.isFinite()) "${v.roundToInt()}°" else "—",19f,true),lp(top=6).apply { width=WRAP })
            val chance=hourly?.optJSONArray("precipitation_probability")?.optInt(i,-1)?:-1
            tile.addView(label(if(chance>=0) "$chance%" else "—",11f,false,Palette.MUTED),lp(top=6).apply { width=WRAP })
            hourRow.addView(tile,LinearLayout.LayoutParams(dp(82),WRAP))
        }
        hoursPanel.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled=false;addView(hourRow) })
        forecasts.addView(hoursPanel,lp(bottom=18))
        val daysPanel=panel();daysPanel.addView(label("7-day forecast",20f,true),lp(bottom=6))
        daysPanel.addView(label("Percentages show the chance of rain or snow.",12f,false,Palette.MUTED),lp(bottom=10))
        val days=column()
        for(i in todayIndex until minOf(todayIndex+7,dates?.length()?:0)) {
            val item=row().apply { minimumHeight=dp(62) }
            val date=runCatching { fmt("yyyy-MM-dd").parse(dates?.optString(i).orEmpty()) }.getOrNull()
            item.addView(label(if(i==todayIndex) "Today" else date?.let { fmt("EEE").format(it) }?:"—",15f,true),LinearLayout.LayoutParams(dp(64),WRAP))
            val code=daily.optJSONArray("weather_code")?.optInt(i)?:-1
            item.addView(label(Weather.emoji(code),23f),LinearLayout.LayoutParams(dp(46),WRAP))
            item.addView(label(Weather.condition(code),14f,false,Palette.MUTED).apply { maxLines=1;ellipsize=TextUtils.TruncateAt.END },LinearLayout.LayoutParams(0,WRAP,1f))
            val temperatures=column();temperatures.addView(label("High ${dailyTemp("temperature_2m_max",i)}",14f,true));temperatures.addView(label("Low ${dailyTemp("temperature_2m_min",i)}",13f,false,Palette.MUTED),lp(top=5))
            item.addView(temperatures,LinearLayout.LayoutParams(dp(99),WRAP).apply { leftMargin=dp(12) })
            val rain=daily.optJSONArray("precipitation_probability_max")?.optInt(i,-1)?:-1
            item.addView(label(if(rain>=0) "$rain%" else "—",12f,false,Palette.MUTED).apply { gravity=Gravity.END },LinearLayout.LayoutParams(dp(46),WRAP))
            days.addView(item)
        }
        daysPanel.addView(ScrollView(this).apply { isVerticalScrollBarEnabled=false;addView(days) },LinearLayout.LayoutParams(MATCH,0,1f))
        forecasts.addView(daysPanel,LinearLayout.LayoutParams(MATCH,0,1f))
        body.addView(forecasts,if(wide) LinearLayout.LayoutParams(0,MATCH,1f) else LinearLayout.LayoutParams(MATCH,0,1f))
        root.addView(body,LinearLayout.LayoutParams(MATCH,0,1f));setContentView(root)
    }
    private fun chooseLocation() {
        val content=column().apply { setPadding(dp(24),dp(16),dp(24),dp(10)) }
        val query=EditText(this).apply { hint="City or postal code";setSingleLine(true);setTextColor(Palette.INK);setHintTextColor(Palette.MUTED) }
        val results=column();content.addView(query);content.addView(results,lp(top=16))
        val dialog=AlertDialog.Builder(this).setTitle("Weather location").setView(content).setPositiveButton("Search",null).setNegativeButton("Cancel",null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val q=query.text.toString().trim();if(q.isEmpty()) return@setOnClickListener
                (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(query.windowToken,0)
                results.removeAllViews();results.addView(label("Searching…",15f))
                worker.execute {
                    val found=runCatching { Weather.search(q) }
                    ui.post {
                        if(!dialog.isShowing || isFinishing) return@post
                        results.removeAllViews()
                        found.onSuccess { cities ->
                            if(cities.length()==0) results.addView(label("No locations found. Try a nearby city.",15f))
                            for(i in 0 until cities.length()) {
                                val c=cities.getJSONObject(i)
                                results.addView(button(c.getString("label")) {
                                    Weather.set(this,c.getDouble("lat"),c.getDouble("lon"),c.getString("label"),null)
                                    dialog.dismiss();refresh(true)
                                }.apply { maxLines=2 },lp(bottom=8))
                            }
                        }.onFailure { results.addView(label("Couldn’t search. Check Wi-Fi and try again.",15f)) }
                    }
                }
            }
        }
        dialog.show();styleDialog(dialog)
    }
}
