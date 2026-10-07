package com.reid.mist

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.max

// ---------- widget definition ----------
class Def(
    val w: Int, val h: Int, val draw: (D) -> Unit,
    val rows: Int = 0, val ticks: Boolean = false, val tap: String = "app",
    val act: (Int, Int, Context) -> String? = { _, _, _ -> null }
)

// row 0 = header, rows 1.. = tasks (open tasks first)
val taskAct: (Int, Int, Context) -> String? = { r, _, x ->
    if (r >= 1) Store.view(x).getOrNull(r - 1)?.let { "tg:${it.first}" } else null
}

// prev / play / next sit in three consecutive columns (of 7) of one row
fun musAct(row: Int, c0: Int): (Int, Int, Context) -> String? = { r, c, _ ->
    if (r != row) null else when (c - c0) {
        0 -> "pv"
        1 -> "pp"
        2 -> "nx"
        else -> null
    }
}

// ---------- storage ----------
class Task(var t: String, var d: Boolean)

object Store {
    private fun sp(c: Context) = c.getSharedPreferences("mist", Context.MODE_PRIVATE)

    fun tasks(c: Context): MutableList<Task> {
        val s = sp(c).getString("tasks", null) ?: "1|Maths PYQs\n0|Chemistry notes\n0|Revision - Redox\n0|Project work"
        return s.split("\n").filter { it.length > 2 }.map { Task(it.substring(2), it[0] == '1') }.toMutableList()
    }

    fun saveTasks(c: Context, l: List<Task>) {
        sp(c).edit().putString("tasks", l.joinToString("\n") { (if (it.d) "1|" else "0|") + it.t.replace("\n", " ") }).apply()
    }

    // open tasks first, paired with their real index
    fun view(c: Context): List<Pair<Int, Task>> {
        val l = tasks(c).withIndex().map { Pair(it.index, it.value) }
        return l.filter { !it.second.d } + l.filter { it.second.d }
    }

    fun toggle(c: Context, i: Int) {
        val l = tasks(c)
        if (i in l.indices) { l[i].d = !l[i].d; saveTasks(c, l) }
    }

    val QD = mapOf(
        "fr" to "Even in the fog, there are roads worth walking.",
        "hm" to "Still, the world moves forward.",
        "tv" to "Some paths should not be taken.",
        "vc" to "A song for a brighter tomorrow."
    )

    fun quote(c: Context, th: String): String = sp(c).getString("q_$th", null) ?: QD[th]!!
    fun setQuote(c: Context, th: String, s: String) { sp(c).edit().putString("q_$th", s).apply() }

    fun cityName(c: Context): String = sp(c).getString("cn", "Bhagalpur") ?: "Bhagalpur"
    fun lat(c: Context): Float = sp(c).getFloat("lat", 25.2425f)
    fun lon(c: Context): Float = sp(c).getFloat("lon", 86.9842f)
    fun setCity(c: Context, n: String, la: Float, lo: Float) {
        sp(c).edit().putString("cn", n).putFloat("lat", la).putFloat("lon", lo).putLong("wt", 0).apply()
    }

    class Wx(val temp: Float, val code: Int, val hi: Float, val lo: Float, val rise: Int, val set: Int, val ts: Long)

    fun wx(c: Context): Wx? {
        val s = sp(c)
        if (!s.contains("w_t")) return null
        return Wx(s.getFloat("w_t", 0f), s.getInt("w_c", 3), s.getFloat("w_h", 0f), s.getFloat("w_l", 0f),
            s.getInt("w_r", 360), s.getInt("w_s", 1080), s.getLong("wt", 0))
    }

    fun saveWx(c: Context, w: Wx) {
        sp(c).edit().putFloat("w_t", w.temp).putInt("w_c", w.code).putFloat("w_h", w.hi).putFloat("w_l", w.lo)
            .putInt("w_r", w.rise).putInt("w_s", w.set).putLong("wt", w.ts).apply()
    }
}

// ---------- weather (Open-Meteo, no key needed) ----------
object Weather {
    fun stale(c: Context): Boolean {
        val w = Store.wx(c)
        return w == null || System.currentTimeMillis() - w.ts > 30 * 60 * 1000
    }

    private fun get(u: String): String {
        val cn = URL(u).openConnection() as HttpURLConnection
        cn.connectTimeout = 7000; cn.readTimeout = 7000
        try { return cn.inputStream.bufferedReader().readText() } finally { cn.disconnect() }
    }

    private fun mins(s: String): Int {
        val t = s.substringAfter('T')
        return t.substring(0, 2).toInt() * 60 + t.substring(3, 5).toInt()
    }

    fun fetch(c: Context): Boolean {
        return try {
            val u = "https://api.open-meteo.com/v1/forecast?latitude=${Store.lat(c)}&longitude=${Store.lon(c)}" +
                "&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset&timezone=auto&forecast_days=1"
            val j = JSONObject(get(u))
            val cur = j.getJSONObject("current"); val d = j.getJSONObject("daily")
            Store.saveWx(c, Store.Wx(
                cur.getDouble("temperature_2m").toFloat(), cur.getInt("weather_code"),
                d.getJSONArray("temperature_2m_max").getDouble(0).toFloat(), d.getJSONArray("temperature_2m_min").getDouble(0).toFloat(),
                mins(d.getJSONArray("sunrise").getString(0)), mins(d.getJSONArray("sunset").getString(0)), System.currentTimeMillis()))
            true
        } catch (e: Exception) { false }
    }

    fun search(name: String): Triple<String, Float, Float>? {
        return try {
            val j = JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?count=1&name=" + URLEncoder.encode(name, "UTF-8")))
            val r = j.getJSONArray("results").getJSONObject(0)
            Triple(r.getString("name"), r.getDouble("latitude").toFloat(), r.getDouble("longitude").toFloat())
        } catch (e: Exception) { null }
    }

    fun label(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Cloudy"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Showers"
        in 95..99 -> "Storm"
        else -> "Cloudy"
    }
}

// ---------- rendering ----------
object Render {
    fun pi(c: Context, a: String): PendingIntent {
        val f = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val nt = Intent.FLAG_ACTIVITY_NEW_TASK
        return when (a) {
            "app" -> PendingIntent.getActivity(c, 1, Intent(c, MainActivity::class.java).addFlags(nt), f)
            "ast" -> PendingIntent.getActivity(c, 2, Intent(c, AssistActivity::class.java).addFlags(nt), f)
            "cal" -> PendingIntent.getActivity(c, 3,
                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR).addFlags(nt), f)
            else -> PendingIntent.getBroadcast(c, 0, Intent(c, ActionReceiver::class.java).setAction("mist:$a"), f)
        }
    }

    fun update(c: Context, m: AppWidgetManager, id: Int, key: String) {
        val def = Reg.m[key] ?: return
        val o = m.getAppWidgetOptions(id)
        val dn = c.resources.displayMetrics.density
        var wd = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).toFloat()
        var hd = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).toFloat()
        if (wd < 40f) wd = def.w.toFloat()
        if (hd < 30f) hd = def.h.toFloat()
        var s = dn
        if (wd * s > 1000f) s = 1000f / wd
        if (hd * s > 1500f) s = 1500f / hd
        val bmp = Bitmap.createBitmap(max(1, (wd * s).toInt()), max(1, (hd * s).toInt()), Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        cv.scale(s, s)
        try { def.draw(D(cv, wd, hd, c)) } catch (e: Throwable) { }
        val rv = RemoteViews(c.packageName, R.layout.widget)
        rv.setImageViewBitmap(R.id.img, bmp)
        rv.setOnClickPendingIntent(R.id.root, pi(c, def.tap))
        if (def.rows > 0) {
            val perm = c.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            rv.setViewVisibility(R.id.rows, View.VISIBLE)
            for (r in 0 until 7) {
                rv.setViewVisibility(ROW[r], if (r < def.rows) View.VISIBLE else View.GONE)
                if (r >= def.rows) continue
                for (cc in 0 until 7) {
                    var a = def.act(r, cc, c)
                    if (a != null && !perm && (a == "pp" || a == "nx" || a == "pv")) a = "app"
                    if (a != null) rv.setOnClickPendingIntent(CELL[r][cc], pi(c, a))
                }
            }
        } else rv.setViewVisibility(R.id.rows, View.GONE)
        m.updateAppWidget(id, rv)
    }

    fun refreshAll(c: Context, only: ((String) -> Boolean)? = null): Boolean {
        val m = AppWidgetManager.getInstance(c)
        var any = false
        for ((key, cls) in CLS) {
            if (only != null && !only(key)) continue
            for (id in m.getAppWidgetIds(ComponentName(c, cls))) { update(c, m, id, key); any = true }
        }
        return any
    }

    fun refreshTicking(c: Context): Boolean = refreshAll(c) { Reg.m[it]?.ticks == true }
}

// ---------- providers & receivers ----------
abstract class BaseW(private val key: String) : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) Render.update(context, appWidgetManager, id, key)
        Tick.schedule(context)
        if (Weather.stale(context)) {
            val pr = goAsync()
            Thread {
                try { if (Weather.fetch(context)) Render.refreshAll(context) } finally { pr.finish() }
            }.start()
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        Render.update(context, appWidgetManager, appWidgetId, key)
    }

    override fun onEnabled(context: Context) { Tick.schedule(context) }
}

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val a = (i.action ?: return).removePrefix("mist:")
        if (a.startsWith("tg:")) {
            Store.toggle(c, a.substring(3).toIntOrNull() ?: return)
            Render.refreshAll(c)
        } else if (a == "pp" || a == "nx" || a == "pv") Music.cmd(c, a)
    }
}

object Tick {
    private fun pi(c: Context): PendingIntent =
        PendingIntent.getBroadcast(c, 9, Intent(c, TickReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun schedule(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val t = (System.currentTimeMillis() / 60000 + 1) * 60000
        am.setExactAndAllowWhileIdle(AlarmManager.RTC, t, pi(c))
    }
}

class TickReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (Render.refreshTicking(c)) Tick.schedule(c)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Render.refreshAll(c)
        Tick.schedule(c)
    }
}
