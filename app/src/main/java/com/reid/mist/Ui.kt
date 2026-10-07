package com.reid.mist

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var tl: LinearLayout

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show() }

    private fun head(s: String): TextView {
        val t = TextView(this); t.text = s; t.textSize = 17f
        t.setTypeface(null, Typeface.BOLD); t.setPadding(0, dp(20), 0, dp(6))
        return t
    }

    private fun note(s: String): TextView { val t = TextView(this); t.text = s; return t }

    private fun btn(s: String, f: () -> Unit): Button {
        val b = Button(this); b.text = s; b.setOnClickListener { f() }
        return b
    }

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

        ll.addView(head("Weather city"))
        val ce = EditText(this); ce.setText(Store.cityName(this)); ce.setSingleLine()
        ll.addView(ce)
        ll.addView(btn("Set city") {
            val n = ce.text.toString().trim()
            if (n.isNotEmpty()) Thread {
                val r = Weather.search(n)
                runOnUiThread {
                    if (r == null) toast("City not found") else {
                        Store.setCity(this, r.first, r.second, r.third)
                        toast("City set to " + r.first)
                        Thread { Weather.fetch(this); Render.refreshAll(this) }.start()
                    }
                }
            }.start()
        })

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

        ll.addView(head("Music"))
        ll.addView(note("The music widgets play audio files stored on this phone. Allow access once."))
        ll.addView(btn("Allow music access") { requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1) })

        ll.addView(head("Widgets"))
        ll.addView(btn("Refresh all widgets") { Render.refreshAll(this); toast("Refreshed") })

        Tick.schedule(this)
        Thread { if (Weather.stale(this)) { Weather.fetch(this); Render.refreshAll(this) } }.start()
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
        Render.refreshAll(this)
    }

    override fun onResume() {
        super.onResume()
        Render.refreshAll(this)
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
