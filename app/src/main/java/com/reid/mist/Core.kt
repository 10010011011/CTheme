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
import kotlin.math.min
import kotlin.math.pow

// ---------- widget definition ----------
class Def(val w: Int, val h: Int, val draw: (D) -> Unit, val ticks: Boolean = false, val tap: String = "app")

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

    fun wxTap(c: Context): String = sp(c).getString("wxtap", "auto") ?: "auto"
    fun setWxTap(c: Context, v: String) { sp(c).edit().putString("wxtap", v).apply() }
    fun size(c: Context, key: String): Int = sp(c).getInt("sz_$key", 100)
    fun setSize(c: Context, key: String, v: Int) { sp(c).edit().putInt("sz_$key", v).apply() }
    fun auto(c: Context): Boolean = sp(c).getBoolean("auto", true)
    fun setAuto(c: Context, v: Boolean) { sp(c).edit().putBoolean("auto", v).apply() }
    fun wjson(c: Context): String? = sp(c).getString("wjson", null)
    fun ajson(c: Context): String? = sp(c).getString("ajson", null)
    fun saveJson(c: Context, w: String) { sp(c).edit().putString("wjson", w).apply() }
    fun saveAir(c: Context, a: String) { sp(c).edit().putString("ajson", a).apply() }
    fun wtry(c: Context): Long = sp(c).getLong("wtry", 0)
    fun setWtry(c: Context, t: Long) { sp(c).edit().putLong("wtry", t).apply() }

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
        return w == null || System.currentTimeMillis() - w.ts > 15 * 60 * 1000
    }

    fun due(c: Context): Boolean = stale(c) && System.currentTimeMillis() - Store.wtry(c) > 5 * 60 * 1000

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
        Store.setWtry(c, System.currentTimeMillis())
        return try {
            val la = Store.lat(c)
            val lo = Store.lon(c)
            val raw = get("https://api.open-meteo.com/v1/forecast?latitude=$la&longitude=$lo" +
                "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m,surface_pressure,cloud_cover,precipitation,is_day" +
                "&hourly=temperature_2m,precipitation_probability,weather_code,visibility" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,sunrise,sunset,uv_index_max" +
                "&timezone=auto&forecast_days=7&wind_speed_unit=kmh")
            val j = JSONObject(raw)
            val cur = j.getJSONObject("current")
            val d = j.getJSONObject("daily")
            Store.saveWx(c, Store.Wx(
                cur.getDouble("temperature_2m").toFloat(), cur.getInt("weather_code"),
                d.getJSONArray("temperature_2m_max").getDouble(0).toFloat(), d.getJSONArray("temperature_2m_min").getDouble(0).toFloat(),
                mins(d.getJSONArray("sunrise").getString(0)), mins(d.getJSONArray("sunset").getString(0)), System.currentTimeMillis()))
            Store.saveJson(c, raw)
            try {
                Store.saveAir(c, get("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$la&longitude=$lo&current=us_aqi,pm2_5&timezone=auto"))
            } catch (e: Exception) { }
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
            "wx" -> PendingIntent.getActivity(c, 4, Intent(c, WeatherLaunchActivity::class.java).addFlags(nt), f)
            "mus" -> PendingIntent.getActivity(c, 6, Intent(c, MusicActivity::class.java).addFlags(nt), f)
            else -> PendingIntent.getBroadcast(c, 0, Intent(c, ActionReceiver::class.java).setAction("mist:$a"), f)
        }
    }

    private fun fix(a: String, perm: Boolean): String = if (!perm && (a == "pp" || a == "nx" || a == "pv")) "app" else a

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
        // content grows with the widget frame (gently, and never below 80%), then the per-widget slider applies
        val fit = min(wd / def.w, hd / def.h)
        val auto = if (Store.auto(c)) fit.pow(.7f).coerceIn(.8f, 2.5f) else 1f
        val u = auto * Store.size(c, key) / 100f
        val bmp = Bitmap.createBitmap(max(1, (wd * s).toInt()), max(1, (hd * s).toInt()), Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        cv.scale(s * u, s * u)
        val d = D(cv, wd / u, hd / u, c)
        try { def.draw(d) } catch (e: Throwable) { }
        val rv = RemoteViews(c.packageName, R.layout.widget)
        rv.setImageViewBitmap(R.id.img, bmp)
        rv.setOnClickPendingIntent(R.id.root, pi(c, def.tap))
        if (d.zones.isEmpty()) {
            rv.setViewVisibility(R.id.rows, View.GONE)
        } else {
            val perm = c.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            val cw = d.w / COLS
            val ch = d.h / ROWS
            val grid = Array(ROWS) { arrayOfNulls<String>(COLS) }
            for (r in 0 until ROWS) for (cc in 0 until COLS) {
                val px = (cc + .5f) * cw
                val py = (r + .5f) * ch
                for (z in d.zones) {
                    var l = z.l
                    var rr = z.r
                    var t = z.t
                    var b = z.b
                    if (rr - l < cw) { val mid = (l + rr) / 2; l = mid - cw / 2; rr = mid + cw / 2 }
                    if (b - t < ch) { val mid = (t + b) / 2; t = mid - ch / 2; b = mid + ch / 2 }
                    if (px >= l && px < rr && py >= t && py < b) { grid[r][cc] = z.a; break }
                }
            }
            rv.setViewVisibility(R.id.rows, View.VISIBLE)
            for (r in 0 until ROWS) {
                val first = grid[r][0]
                var same = first != null
                for (cc in 1 until COLS) if (grid[r][cc] != first) same = false
                if (same && first != null) rv.setOnClickPendingIntent(ROW[r], pi(c, fix(first, perm)))
                else for (cc in 0 until COLS) {
                    val a = grid[r][cc]
                    if (a != null) rv.setOnClickPendingIntent(CELL[r][cc], pi(c, fix(a, perm)))
                }
            }
        }
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
        val any = Render.refreshTicking(c)
        if (any) Tick.schedule(c)
        if (any && Weather.due(c)) {
            val pr = goAsync()
            Thread {
                try { if (Weather.fetch(c)) Render.refreshAll(c) } finally { pr.finish() }
            }.start()
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Render.refreshAll(c)
        Tick.schedule(c)
    }
}
