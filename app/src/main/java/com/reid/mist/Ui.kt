package com.reid.mist

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

fun Activity.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
fun Activity.toast(s: String) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show() }

class MainActivity : Activity() {
    private lateinit var tl: LinearLayout
    private lateinit var ce: EditText
    private lateinit var cityNote: TextView
    private lateinit var wxTapNote: TextView

    private fun head(s: String): TextView {
        val t = TextView(this)
        t.text = s; t.textSize = 17f
        t.setTypeface(null, Typeface.BOLD); t.setPadding(0, dp(20), 0, dp(6))
        return t
    }

    private fun note(s: String): TextView { val t = TextView(this); t.text = s; return t }

    private fun btn(s: String, f: () -> Unit): Button {
        val b = Button(this); b.text = s; b.setOnClickListener { f() }
        return b
    }

    private fun cityText(): String =
        "Using: " + Store.cityName(this) + String.format(Locale.US, "  (%.3f, %.3f)", Store.lat(this), Store.lon(this))

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val sv = ScrollView(this)
        val ll = LinearLayout(this)
        ll.orientation = LinearLayout.VERTICAL
        ll.setPadding(dp(16), dp(12), dp(16), dp(28))
        sv.addView(ll)
        setContentView(sv)

        val title = TextView(this); title.text = "Mist widgets"; title.textSize = 24f
        title.setTypeface(null, Typeface.BOLD)
        ll.addView(title)
        ll.addView(note("Long-press the home screen, open Widgets and look for Frieren, Hail Mary, TVA and Vocaloid."))

        ll.addView(head("Pages"))
        ll.addView(btn("Weather details") { startActivity(Intent(this, WeatherActivity::class.java)) })
        ll.addView(btn("Music (songs and covers)") { startActivity(Intent(this, MusicActivity::class.java)) })
        ll.addView(btn("Widget sizes") { startActivity(Intent(this, SizesActivity::class.java)) })

        ll.addView(head("Weather location"))
        cityNote = note(cityText())
        ll.addView(cityNote)
        ce = EditText(this); ce.setText(Store.cityName(this)); ce.setSingleLine()
        ll.addView(ce)
        ll.addView(btn("Set city") {
            val n = ce.text.toString().trim()
            if (n.isNotEmpty()) Thread {
                val r = Weather.search(n)
                runOnUiThread {
                    if (r == null) toast("City not found") else {
                        Store.setCity(this, r.first, r.second, r.third)
                        cityNote.text = cityText()
                        toast("City set to " + r.first)
                        Thread { Weather.fetch(this); Render.refreshAll(this) }.start()
                    }
                }
            }.start()
        })
        ll.addView(btn("Use my current location (more accurate)") { useLocation() })

        ll.addView(head("When you tap a weather widget"))
        wxTapNote = note(tapLabel())
        ll.addView(wxTapNote)
        ll.addView(btn("Change tap action") { chooseTap() })

        ll.addView(head("Quotes"))
        val names = listOf("fr" to "Frieren Mist", "hm" to "Hail Mary", "tv" to "TVA", "vc" to "Vocaloid")
        val qe = HashMap<String, EditText>()
        for ((id, nm) in names) {
            ll.addView(note(nm))
            val e = EditText(this); e.setText(Store.quote(this, id))
            qe[id] = e
            ll.addView(e)
        }
        ll.addView(btn("Save quotes") {
            for ((id, e) in qe) Store.setQuote(this, id, e.text.toString().trim().ifEmpty { Store.QD[id]!! })
            Render.refreshAll(this); toast("Saved")
        })

        ll.addView(head("Tasks"))
        tl = LinearLayout(this); tl.orientation = LinearLayout.VERTICAL
        ll.addView(tl)
        val ne = EditText(this); ne.hint = "New task"; ne.setSingleLine()
        ll.addView(ne)
        ll.addView(btn("Add task") {
            val t = ne.text.toString().trim()
            if (t.isNotEmpty()) {
                val l = Store.tasks(this); l.add(Task(t, false)); Store.saveTasks(this, l)
                ne.setText(""); drawTasks(); Render.refreshAll(this)
            }
        })
        drawTasks()

        ll.addView(head("Music access"))
        ll.addView(note("The music widgets play audio files stored on this phone. Allow access once."))
        ll.addView(btn("Allow music access") { requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1) })

        ll.addView(head("Widgets"))
        ll.addView(btn("Refresh all widgets") { Render.refreshAll(this); toast("Refreshed") })

        Tick.schedule(this)
        Thread { if (Weather.stale(this)) { Weather.fetch(this); Render.refreshAll(this) } }.start()
    }

    private fun appLabel(p: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(p, 0)).toString()
    } catch (e: Exception) { p }

    private fun tapLabel(): String {
        val m = Store.wxTap(this)
        val what = when {
            m == "mist" -> "Mist weather page"
            m == "google" -> "Google weather (web page)"
            m == "auto" -> {
                val p = findWeatherApp(this)
                if (p != null) "the phone's weather app (" + appLabel(p) + ")" else "Google weather (no weather app found)"
            }
            else -> appLabel(m.removePrefix("pkg:"))
        }
        return "Opens: $what"
    }

    private fun setTap(v: String) { Store.setWxTap(this, v); wxTapNote.text = tapLabel() }

    private fun chooseTap() {
        val opts = arrayOf("Auto: phone's weather app, else Google weather", "Google weather (web page)", "Mist weather page (this app)", "Choose an app...")
        AlertDialog.Builder(this).setTitle("Tapping a weather widget opens").setItems(opts) { _, w ->
            when (w) {
                0 -> setTap("auto")
                1 -> setTap("google")
                2 -> setTap("mist")
                else -> pickApp()
            }
        }.show()
    }

    private fun pickApp() {
        val apps = launcherApps(this)
        AlertDialog.Builder(this).setTitle("Choose your weather app")
            .setItems(Array(apps.size) { apps[it].first }) { _, w -> setTap("pkg:" + apps[w].second) }
            .show()
    }

    private fun useLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 2)
            return
        }
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var best: Location? = null
        for (p in lm.getProviders(true)) {
            val l: Location? = try { lm.getLastKnownLocation(p) } catch (e: SecurityException) { null }
            val b0 = best
            if (l != null && (b0 == null || l.time > b0.time)) best = l
        }
        val b = best
        if (b == null) { toast("No recent location found. Open Maps once, then try again."); return }
        Thread {
            var name = "My location"
            try {
                val a = Geocoder(this, Locale.ENGLISH).getFromLocation(b.latitude, b.longitude, 1)
                if (a != null && a.isNotEmpty()) name = a[0].locality ?: a[0].subAdminArea ?: name
            } catch (e: Exception) { }
            Store.setCity(this, name, b.latitude.toFloat(), b.longitude.toFloat())
            Weather.fetch(this)
            Render.refreshAll(this)
            runOnUiThread { ce.setText(name); cityNote.text = cityText(); toast("Location set: $name") }
        }.start()
    }

    private fun drawTasks() {
        tl.removeAllViews()
        val l = Store.tasks(this)
        for (i in l.indices) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL; row.gravity = Gravity.CENTER_VERTICAL
            val cb = CheckBox(this); cb.text = l[i].t; cb.isChecked = l[i].d
            cb.setOnCheckedChangeListener { _, v ->
                val x = Store.tasks(this)
                if (i < x.size) { x[i].d = v; Store.saveTasks(this, x); Render.refreshAll(this) }
            }
            cb.setOnLongClickListener { rename(i); true }
            row.addView(cb, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(btn("Delete") {
                val x = Store.tasks(this)
                if (i < x.size) { x.removeAt(i); Store.saveTasks(this, x); drawTasks(); Render.refreshAll(this) }
            })
            tl.addView(row)
        }
        tl.addView(note("Tip: long-press a task to rename it."))
    }

    private fun rename(i: Int) {
        val l = Store.tasks(this)
        if (i >= l.size) return
        val e = EditText(this); e.setText(l[i].t)
        AlertDialog.Builder(this).setTitle("Rename task").setView(e)
            .setPositiveButton("Save") { _, _ ->
                val t = e.text.toString().trim()
                if (t.isNotEmpty()) { l[i].t = t; Store.saveTasks(this, l); drawTasks(); Render.refreshAll(this) }
            }
            .setNegativeButton("Cancel", null).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val ok = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (requestCode == 2 && ok) useLocation()
        Render.refreshAll(this)
    }

    override fun onResume() {
        super.onResume()
        Render.refreshAll(this)
    }
}

// per-widget content size (50% - 200%) plus the auto-fit switch
class SizesActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val sv = ScrollView(this)
        val ll = LinearLayout(this)
        ll.orientation = LinearLayout.VERTICAL
        ll.setPadding(dp(16), dp(12), dp(16), dp(28))
        sv.addView(ll)
        setContentView(sv)

        val t = TextView(this); t.text = "Widget sizes"; t.textSize = 24f
        t.setTypeface(null, Typeface.BOLD)
        ll.addView(t)
        val n = TextView(this)
        n.text = "Android decides each widget's frame (long-press a widget on the home screen and drag its handles). " +
            "These sliders scale what is drawn inside it, per widget type. 100% is the default."
        ll.addView(n)
        val auto = CheckBox(this)
        auto.text = "Grow or shrink content with the widget frame"
        auto.isChecked = Store.auto(this)
        auto.setOnCheckedChangeListener { _, v -> Store.setAuto(this, v); Render.refreshAll(this) }
        ll.addView(auto)

        val names = mapOf("fr" to "Frieren Mist", "hm" to "Hail Mary", "tv" to "TVA", "vc" to "Vocaloid")
        var last = ""
        for ((key, label) in LABELS) {
            val th = key.substring(0, 2)
            if (th != last) {
                last = th
                val h = TextView(this); h.text = names[th]; h.textSize = 17f
                h.setTypeface(null, Typeface.BOLD); h.setPadding(0, dp(18), 0, dp(4))
                ll.addView(h)
            }
            val short = label.substringAfter(" - ")
            val tv = TextView(this)
            tv.text = short + "   " + Store.size(this, key) + "%"
            ll.addView(tv)
            val sb = SeekBar(this)
            sb.max = 150
            sb.progress = Store.size(this, key) - 50
            sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, u: Boolean) { tv.text = short + "   " + (p + 50) + "%" }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) {
                    Store.setSize(this@SizesActivity, key, s.progress + 50)
                    Render.refreshAll(this@SizesActivity) { it == key }
                }
            })
            ll.addView(sb)
        }
        val reset = Button(this)
        reset.text = "Reset all to 100%"
        reset.setOnClickListener {
            for ((key, _) in LABELS) Store.setSize(this, key, 100)
            Render.refreshAll(this); recreate()
        }
        ll.addView(reset)
    }
}

// Miss Minutes opens the phone's default assistant; falls back to voice search, then to this app
class AssistActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val tries = listOf(Intent(Intent.ACTION_ASSIST), Intent(Intent.ACTION_VOICE_COMMAND), Intent(Intent.ACTION_WEB_SEARCH))
        var ok = false
        for (i in tries) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try { startActivity(i); ok = true; break } catch (e: Exception) { }
        }
        if (!ok) startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

// launcher apps installed on the phone (needs the <queries> entry in the manifest on Android 11+)
fun launcherApps(c: Context): List<Pair<String, String>> {
    val pm = c.packageManager
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(i, 0)
        .map { Pair(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        .filter { it.second != c.packageName }
        .distinctBy { it.second }
        .sortedBy { it.first.lowercase() }
}

// Android 11 has no "default weather app", so look for any installed app with "weather" in its name
fun findWeatherApp(c: Context): String? =
    launcherApps(c).firstOrNull { it.first.contains("weather", true) || it.second.contains("weather", true) }?.second

// tapping a weather widget lands here and is forwarded to the target chosen in settings
class WeatherLaunchActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val mode = Store.wxTap(this)
        var ok = false
        if (mode.startsWith("pkg:")) ok = openPkg(mode.substring(4))
        else if (mode == "auto") {
            val p = findWeatherApp(this)
            ok = if (p != null) openPkg(p) else google()
        } else if (mode == "google") ok = google()
        if (!ok) go(Intent(this, WeatherActivity::class.java))
        finish()
    }

    private fun go(i: Intent): Boolean {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try { startActivity(i); true } catch (e: Exception) { false }
    }

    private fun openPkg(p: String): Boolean {
        val i = packageManager.getLaunchIntentForPackage(p) ?: return false
        return go(i)
    }

    private fun google(): Boolean =
        go(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=weather+" + Uri.encode(Store.cityName(this)))))
}
