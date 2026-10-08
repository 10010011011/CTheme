package com.reid.mist

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// ---------- card styles used by the Frieren / Hail Mary / TVA / Vocaloid files ----------

// look of a task-list card (see drawTasks)
class TStyle(
    val card: (D) -> Unit,
    val title: String,
    val tfH: Typeface,
    val hSize: Float,
    val hLs: Float,
    val head: Int,
    val tf: Typeface,
    val txt: Int,
    val doneCol: Int,
    val sub: Int,
    val box: Int,
    val boxFill: Int,
    val boxDone: Int,
    val check: Int,
    val circle: Boolean,
    val rule: Int?,
    val num: Boolean,
    val right: Boolean,
    val count: Boolean
)

// look of a music-player card (see drawMusic)
class MStyle(
    val card: (D) -> Unit,
    val art: (D, Float, Float, Float) -> Unit,
    val tf: Typeface,
    val tfT: Typeface,
    val title: Int,
    val sub: Int,
    val bar: Int,
    val barBg: Int,
    val ico: Int,
    val playBg: Int?,
    val playFg: Int,
    val box: Int?,
    val ls: Float
)

// ---------- task-list widget card ----------
// every row is a tap zone "tg:<index>" which ActionReceiver turns into Store.toggle
fun drawTasks(d: D, s: TStyle) {
    val c = d.c
    s.card(d)
    val pad = 12f
    val items = Store.view(d.x)
    val total = items.size
    val done = items.count { it.second.d }

    val hy = pad + s.hSize
    dTx(c, s.title, pad, hy, s.hSize, s.head, s.tfH, LFT, s.hLs)
    if (s.count) dTx(c, "$done/$total", d.w - pad, hy, s.hSize, s.sub, s.tfH, RGT, s.hLs)

    val rl = s.rule
    val top = hy + 10f
    if (rl != null) dLine(c, pad, top, d.w - pad, top, rl, 1f)

    if (total == 0) {
        dTx(c, "No tasks", d.w / 2, d.h / 2 + 12f, 12f, s.sub, s.tf, CTR)
        return
    }

    val rowH = 24f
    val avail = d.h - top - pad
    val maxRows = max(1, (avail / rowH).toInt())
    val show = if (total <= maxRows) total else max(1, maxRows - 1)
    val fs = 12.5f
    val bs = 13f
    val numW = if (s.num) dTw("00", fs, s.tf) + 8f else 0f
    val sideW = bs + 10f
    val textX = pad + numW + (if (s.right) 0f else sideW)
    val textW = d.w - pad - textX - (if (s.right) sideW else 0f)

    for (i in 0 until show) {
        val idx = items[i].first
        val t = items[i].second
        val ry = top + i * rowH
        val cy = ry + rowH / 2
        val bx = if (s.right) d.w - pad - bs / 2 else pad + bs / 2
        val outline = if (t.d) s.boxDone else s.box
        val fill: Int? = if (t.d) s.boxFill else null
        if (s.circle) {
            dCirc(c, bx, cy, bs / 2, fill, outline, 1.4f)
        } else {
            dRR(c, bx - bs / 2, cy - bs / 2, bx + bs / 2, cy + bs / 2, 3f, fill, outline, 1.4f)
        }
        if (t.d) dIco(c, "check", bx, cy, bs * .8f, s.check)
        if (s.num) dTx(c, String.format(Locale.US, "%02d", i + 1), pad, cy + fs * .35f, fs * .85f, s.sub, s.tf)
        val col = if (t.d) s.doneCol else s.txt
        val shown = dFit(t.t, textW, fs, s.tf)
        dTx(c, shown, textX, cy + fs * .35f, fs, col, s.tf)
        if (t.d) dLine(c, textX, cy, textX + dTw(shown, fs, s.tf), cy, col, 1f)
        if (rl != null) dLine(c, pad, ry + rowH, d.w - pad, ry + rowH, rl, 1f)
        d.zone(pad, ry, d.w - pad, ry + rowH, "tg:$idx")
    }

    val more = total - show
    if (more > 0 && show < maxRows) {
        val my = top + show * rowH
        dTx(c, "+$more more", pad, my + rowH / 2 + fs * .35f, fs * .9f, s.sub, s.tf)
    }
}

// ---------- music widget card ----------
// prev / play-pause / next are tap zones ("pv", "pp", "nx"); the rest of the card opens the Music page
fun drawMusic(d: D, s: MStyle) {
    val c = d.c
    s.card(d)
    val pad = 12f
    val showArt = d.w >= 220f
    val asz = min(d.h - 2 * pad, 110f).coerceAtLeast(40f)
    var x0 = pad
    if (showArt) {
        s.art(d, pad, (d.h - asz) / 2, asz)
        x0 = pad + asz + 14f
    }
    val rx = d.w - pad
    val maxW = rx - x0
    val blockH = min(92f, d.h - 12f)
    val y0 = (d.h - blockH) / 2

    dTx(c, dFit(musTitle(), maxW, 15f, s.tfT, s.ls), x0, y0 + 15f, 15f, s.title, s.tfT, LFT, s.ls)
    dTx(c, dFit(musArtist(), maxW, 12f, s.tf, s.ls), x0, y0 + 33f, 12f, s.sub, s.tf, LFT, s.ls)

    val by = y0 + 49f
    dBar(c, x0, rx, by, musFrac(), s.bar, s.barBg, 4f)
    if (d.h >= 120f) {
        dTx(c, mmss(PS.pos), x0, by + 14f, 9.5f, s.sub, s.tf, LFT, s.ls)
        dTx(c, mmss(PS.dur), rx, by + 14f, 9.5f, s.sub, s.tf, RGT, s.ls)
    }

    val cy = y0 + blockH - 13f
    val cx = (x0 + rx) / 2
    val gap = min(52f, maxW / 3.2f)
    val pb = s.playBg
    val bb = s.box
    for (j in 0 until 3) {
        val bx = cx + (j - 1) * gap
        val mid = j == 1
        val r = if (mid) 15f else 12f
        val name = if (j == 0) "prev" else if (j == 1) (if (PS.playing) "pause" else "play") else "next"
        if (mid && pb != null) {
            dCirc(c, bx, cy, r + 2f, pb)
        } else if (bb != null) {
            dRR(c, bx - r - 2f, cy - r, bx + r + 2f, cy + r, 7f, null, bb, 1f)
        }
        dIco(c, name, bx, cy, if (mid) 20f else 16f, if (mid) s.playFg else s.ico)
        val act = if (j == 0) "pv" else if (j == 1) "pp" else "nx"
        d.zone(bx - gap / 2, cy - 20f, bx + gap / 2, cy + 20f, act)
    }
}

// ---------- Weather details page ----------
class WeatherActivity : Activity() {
    private lateinit var box: LinearLayout
    private val cText = Color.parseColor("#e8eef2")
    private val cMuted = Color.parseColor("#8a98a3")
    private val cAccent = Color.parseColor("#7fd1c9")

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val sv = ScrollView(this)
        sv.setBackgroundColor(Color.parseColor("#0f1418"))
        box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(18), dp(28), dp(18), dp(28))
        sv.addView(box)
        setContentView(sv)
        build()
        if (Weather.stale(this)) refresh()
    }

    private fun refresh() {
        Thread {
            Weather.fetch(this)
            Render.refreshAll(this)
            runOnUiThread { build() }
        }.start()
    }

    private fun tx(s: String, size: Float, col: Int, bold: Boolean): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = size
        t.setTextColor(col)
        if (bold) t.setTypeface(null, Typeface.BOLD)
        return t
    }

    private fun head(s: String) {
        val t = tx(s, 13f, cAccent, true)
        t.setPadding(0, dp(22), 0, dp(6))
        box.addView(t)
    }

    private fun kv(a: String, b: String) {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.setPadding(0, dp(5), 0, dp(5))
        r.addView(tx(a, 14f, cMuted, false), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        r.addView(tx(b, 14f, cText, false))
        box.addView(r)
    }

    private fun row(a: String, b: String, c: String) {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.setPadding(0, dp(5), 0, dp(5))
        r.addView(tx(a, 14f, cMuted, false), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.1f))
        r.addView(tx(b, 14f, cText, false), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.6f))
        r.addView(tx(c, 14f, cText, false), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(r)
    }

    private fun num(o: JSONObject, key: String, unit: String): String =
        if (o.has(key) && !o.isNull(key)) o.getDouble(key).roundToInt().toString() + unit else "--"

    private fun arrNum(a: JSONArray, i: Int, unit: String): String =
        if (i < a.length() && !a.isNull(i)) a.getDouble(i).roundToInt().toString() + unit else "--"

    private fun compass(deg: Double): String {
        val names = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val a = ((deg % 360.0) + 360.0) % 360.0
        return names[((a + 22.5) / 45.0).toInt() % 8]
    }

    private fun aqiLabel(v: Int): String = when {
        v <= 50 -> "Good"
        v <= 100 -> "Moderate"
        v <= 150 -> "Unhealthy for sensitive groups"
        v <= 200 -> "Unhealthy"
        v <= 300 -> "Very unhealthy"
        else -> "Hazardous"
    }

    private fun dayName(s: String): String {
        return try {
            val dt = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s)
            if (dt == null) s else SimpleDateFormat("EEE d MMM", Locale.ENGLISH).format(dt)
        } catch (e: Exception) { s }
    }

    private fun addRefresh() {
        val b = Button(this)
        b.text = "Refresh now"
        b.setOnClickListener { refresh(); toast("Refreshing...") }
        box.addView(b)
    }

    private fun build() {
        box.removeAllViews()
        box.addView(tx(Store.cityName(this), 26f, cText, true))
        val raw = Store.wjson(this)
        if (raw == null) {
            box.addView(tx("No weather data yet. Check your internet connection and tap Refresh.", 14f, cMuted, false))
            addRefresh()
            return
        }
        try {
            val j = JSONObject(raw)
            val cur = j.getJSONObject("current")
            val code = cur.getInt("weather_code")
            box.addView(tx(num(cur, "temperature_2m", "°C"), 54f, cText, false))
            box.addView(tx(Weather.label(code), 18f, cAccent, false))

            kv("Feels like", num(cur, "apparent_temperature", "°C"))
            kv("Humidity", num(cur, "relative_humidity_2m", "%"))
            kv("Wind", num(cur, "wind_speed_10m", " km/h") + " " + compass(cur.optDouble("wind_direction_10m", 0.0)))
            kv("Pressure", num(cur, "surface_pressure", " hPa"))
            kv("Cloud cover", num(cur, "cloud_cover", "%"))
            kv("Rain now", String.format(Locale.US, "%.1f mm", cur.optDouble("precipitation", 0.0)))

            val dd = j.getJSONObject("daily")
            kv("UV index (max today)", arrNum(dd.getJSONArray("uv_index_max"), 0, ""))
            kv("Sunrise", dd.getJSONArray("sunrise").getString(0).substringAfter('T'))
            kv("Sunset", dd.getJSONArray("sunset").getString(0).substringAfter('T'))

            val aj = Store.ajson(this)
            if (aj != null) {
                try {
                    val ac = JSONObject(aj).getJSONObject("current")
                    val aq = ac.optInt("us_aqi", -1)
                    if (aq >= 0) kv("Air quality (US AQI)", "$aq  " + aqiLabel(aq))
                    kv("PM2.5", num(ac, "pm2_5", " µg/m³"))
                } catch (e: Exception) { }
            }

            val hr = j.getJSONObject("hourly")
            val times = hr.getJSONArray("time")
            val ht = hr.getJSONArray("temperature_2m")
            val hpr = hr.getJSONArray("precipitation_probability")
            val hco = hr.getJSONArray("weather_code")
            val ct = cur.optString("time", "")
            val prefix = if (ct.length >= 13) ct.substring(0, 13) else ""
            var start = 0
            for (i in 0 until times.length()) {
                if (times.getString(i).startsWith(prefix)) { start = i; break }
            }
            head("NEXT HOURS")
            val endH = min(times.length(), start + 12)
            for (i in start until endH) {
                val tm = times.getString(i)
                row(
                    if (tm.length >= 16) tm.substring(11, 16) else tm,
                    arrNum(ht, i, "°") + "  " + Weather.label(hco.optInt(i, 3)),
                    arrNum(hpr, i, "%")
                )
            }

            head("7 DAYS")
            val dtm = dd.getJSONArray("time")
            val dco = dd.getJSONArray("weather_code")
            val dhi = dd.getJSONArray("temperature_2m_max")
            val dlo = dd.getJSONArray("temperature_2m_min")
            val dpr = dd.getJSONArray("precipitation_probability_max")
            for (i in 0 until dtm.length()) {
                row(
                    dayName(dtm.getString(i)),
                    Weather.label(dco.optInt(i, 3)) + "  " + arrNum(dhi, i, "°") + "/" + arrNum(dlo, i, "°"),
                    arrNum(dpr, i, "%")
                )
            }
        } catch (e: Exception) {
            box.addView(tx("Could not read the saved weather data. Tap Refresh.", 14f, cMuted, false))
        }
        addRefresh()
        val foot = tx("Data: Open-Meteo.com", 11f, cMuted, false)
        foot.setPadding(0, dp(16), 0, 0)
        box.addView(foot)
    }
}

// ---------- Music page: now playing, song list, per-song cover images ----------
class MusicActivity : Activity() {
    private lateinit var nowTitle: TextView
    private lateinit var nowArtist: TextView
    private lateinit var nowTime: TextView
    private lateinit var nowBar: SeekBar
    private lateinit var playBtn: Button
    private lateinit var permBtn: Button
    private lateinit var filter: EditText
    private lateinit var listBox: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private val songs = ArrayList<Triple<Long, String, String>>()
    private var pickId = -1L
    private var seeking = false

    private val tick = object : Runnable {
        override fun run() {
            showNow()
            handler.postDelayed(this, 1000)
        }
    }

    private fun ctlBtn(s: String, f: () -> Unit): Button {
        val b = Button(this)
        b.text = s
        b.setOnClickListener { f() }
        return b
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(12), dp(16), dp(8))

        val title = TextView(this)
        title.text = "Music"
        title.textSize = 24f
        title.setTypeface(null, Typeface.BOLD)
        root.addView(title)

        nowTitle = TextView(this)
        nowTitle.textSize = 18f
        nowTitle.setTypeface(null, Typeface.BOLD)
        nowTitle.setSingleLine()
        root.addView(nowTitle)
        nowArtist = TextView(this)
        nowArtist.textSize = 14f
        root.addView(nowArtist)

        nowBar = SeekBar(this)
        nowBar.max = 1000
        nowBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, u: Boolean) {}
            override fun onStartTrackingTouch(sb: SeekBar) { seeking = true }
            override fun onStopTrackingTouch(sb: SeekBar) {
                seeking = false
                if (PS.dur > 0) Music.send(this@MusicActivity, "sk", -1L, (sb.progress / 1000f * PS.dur).toInt())
            }
        })
        root.addView(nowBar)
        nowTime = TextView(this)
        nowTime.textSize = 12f
        root.addView(nowTime)

        val ctl = LinearLayout(this)
        ctl.orientation = LinearLayout.HORIZONTAL
        ctl.addView(ctlBtn("Prev") { Music.cmd(this, "pv") }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        playBtn = ctlBtn("Play") { Music.cmd(this, "pp") }
        ctl.addView(playBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        ctl.addView(ctlBtn("Next") { Music.cmd(this, "nx") }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        ctl.addView(ctlBtn("Stop") { Music.cmd(this, "st") }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(ctl)

        permBtn = Button(this)
        permBtn.text = "Allow music access"
        permBtn.setOnClickListener { requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1) }
        root.addView(permBtn)

        filter = EditText(this)
        filter.hint = "Search songs"
        filter.setSingleLine()
        filter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, aft: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, bef: Int, cnt: Int) {}
            override fun afterTextChanged(s: Editable?) { drawList() }
        })
        root.addView(filter)

        val sv = ScrollView(this)
        listBox = LinearLayout(this)
        listBox.orientation = LinearLayout.VERTICAL
        sv.addView(listBox)
        root.addView(sv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        reload()
    }

    private fun reload() {
        val ok = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        permBtn.visibility = if (ok) View.GONE else View.VISIBLE
        loadSongs()
        drawList()
    }

    private fun loadSongs() {
        songs.clear()
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) return
        try {
            val cur = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST),
                MediaStore.Audio.Media.IS_MUSIC + " != 0", null,
                MediaStore.Audio.Media.TITLE + " COLLATE NOCASE")
            cur?.use {
                while (it.moveToNext()) songs.add(Triple(it.getLong(0), it.getString(1) ?: "Unknown", it.getString(2) ?: ""))
            }
        } catch (e: Exception) { }
    }

    private fun drawList() {
        listBox.removeAllViews()
        val q = filter.text.toString().trim().lowercase()
        var shown = 0
        for (sg in songs) {
            if (q.isNotEmpty() && !sg.second.lowercase().contains(q) && !sg.third.lowercase().contains(q)) continue
            if (shown >= 400) break
            shown++
            val id = sg.first
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(0, dp(6), 0, dp(6))
            val label = TextView(this)
            label.text = sg.second + (if (sg.third.isEmpty()) "" else "\n" + sg.third)
            label.textSize = 15f
            label.setOnClickListener { Music.send(this, "pl", id) }
            row.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val cb = Button(this)
            cb.text = if (Cover.has(this, id)) "Change cover" else "Add cover"
            cb.textSize = 11f
            cb.setOnClickListener { coverMenu(id) }
            row.addView(cb)
            listBox.addView(row)
        }
        if (shown == 0) {
            val n = TextView(this)
            n.text = if (songs.isEmpty()) "No songs to show. Allow music access and make sure the phone has audio files." else "No matches."
            listBox.addView(n)
        }
    }

    private fun coversChanged() {
        drawList()
        Render.refreshAll(this) { it.endsWith("music") }
    }

    private fun coverMenu(id: Long) {
        if (!Cover.has(this, id)) { pickCover(id); return }
        AlertDialog.Builder(this).setTitle("Cover").setItems(arrayOf("Choose another image", "Remove cover")) { _, w ->
            if (w == 0) pickCover(id) else { Cover.remove(this, id); coversChanged() }
        }.show()
    }

    private fun pickCover(id: Long) {
        pickId = id
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "image/*"
        try {
            startActivityForResult(Intent.createChooser(i, "Choose cover image"), 7)
        } catch (e: Exception) { toast("No image picker found") }
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == 7 && res == RESULT_OK) {
            val u = data?.data
            if (u != null && pickId >= 0) {
                if (Cover.save(this, pickId, u)) { toast("Cover saved"); coversChanged() } else toast("Could not read that image")
            }
        }
    }

    private fun showNow() {
        nowTitle.text = musTitle()
        nowArtist.text = musArtist()
        nowTime.text = mmss(PS.pos) + " / " + mmss(PS.dur)
        if (!seeking) nowBar.progress = (musFrac() * 1000f).toInt()
        playBtn.text = if (PS.playing) "Pause" else "Play"
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        reload()
        Render.refreshAll(this)
    }

    override fun onResume() {
        super.onResume()
        handler.post(tick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }
}
