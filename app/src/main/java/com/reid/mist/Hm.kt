package com.reid.mist

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val BG = k("#f2080b0d")
private val ED = k("#252c31")
private val MU = k("#7d8a93")
private val TX = k("#cfd8dc")
private val TW = k("#e6edf0")
private val GR = k("#8fd19e")
private val RD = k("#e5322d")

private fun panel(d: D) = dRR(d.c, 1f, 1f, d.w - 1, d.h - 1, 12f, BG, ED, 1f)

// ---------- astronomy (all offline) ----------
object Astro {
    class E(val lam: Double, val r: Double, val v: Double, val pct: Double)

    private fun days(ms: Long): Double = ms / 86400000.0 + 2440587.5 - 2451545.0
    private fun mod(a: Double): Double = ((a % 360.0) + 360.0) % 360.0
    fun lon(l0: Double, n: Double, ms: Long): Double = mod(l0 + n * days(ms))

    fun earth(ms: Long): E {
        val l = lon(100.46457, 0.98564736, ms)
        val m = mod(l - 102.93735)
        val mr = Math.toRadians(m)
        val nu = m + 1.914602 * Math.sin(mr) + 0.019993 * Math.sin(2 * mr)
        val r = 1.00014 - 0.01671 * Math.cos(mr) - 0.00014 * Math.cos(2 * mr)
        return E(mod(nu + 102.93735), r, 29.7847 * Math.sqrt(2 / r - 1), m / 3.6)
    }
}

private const val SX = 92f
private const val SY = 160f
private const val KK = .7f
private const val TH = -16f

private fun pp(r: Float, a: Float): FloatArray {
    val ar = rad(a)
    val t = rad(TH)
    val lx = r * cos(ar)
    val ly = KK * r * sin(ar)
    return floatArrayOf(SX + lx * cos(t) - ly * sin(t), SY + lx * sin(t) + ly * cos(t))
}

private fun orbit(c: Canvas, e: Astro.E) {
    val rnd = kotlin.random.Random(11)
    val now = System.currentTimeMillis()
    val aE = 45f
    val lamE = e.lam.toFloat()
    for (i in 0 until 40) {
        val x = rnd.nextFloat() * 360f; val y = rnd.nextFloat() * 330f
        val s = .5f + rnd.nextFloat() * .8f; val al = (255 * (.3f + rnd.nextFloat() * .5f)).toInt()
        dCirc(c, x, y, s, Color.argb(al, 255, 255, 255))
    }
    for (i in 0 until 750) {
        val lf = rnd.nextFloat() * 360f
        val r = 226f + rnd.nextFloat() * 38f
        val s = .6f + rnd.nextFloat() * .9f
        val al = (255 * (.35f + rnd.nextFloat() * .55f)).toInt()
        val a = ((aE - lf + lamE) % 360f + 360f) % 360f
        val p = pp(r, a)
        if (p[0] > 0f && p[0] < 358f && p[1] > 2f && p[1] < 328f) dCirc(c, p[0], p[1], s, Color.argb(al, 255, 255, 255))
    }
    val rr = floatArrayOf(22f, 40f, 62f, 88f, 118f, 150f)
    for (i in rr.indices) dOval(c, SX, SY, rr[i], rr[i] * KK, TH, Color.argb((255 * (.7f - i * .08f)).toInt(), 255, 255, 255), .8f)
    val tp = Path()
    var first = true
    var a = aE - 125f
    while (a <= aE + 30f) {
        val p = pp(205f, a)
        if (first) { tp.moveTo(p[0], p[1]); first = false } else tp.lineTo(p[0], p[1])
        a += 3f
    }
    dPath(c, tp, RD, false, 2f, floatArrayOf(7f, 5f), false)
    val ep = pp(205f, aE)
    val mk = pp(205f, aE - 29.5f)
    dLine(c, SX, SY, ep[0], ep[1], k("#b3ffffff"), 1f, floatArrayOf(1f, 4f), true)
    dCirc(c, SX, SY, 34f, k("#14f5a623"))
    dCirc(c, SX, SY, 24f, k("#29f5a623"))
    dCirc(c, SX, SY, 15f, k("#66f5a623"))
    dCirc(c, SX, SY, 8f, k("#ffd27a"))
    val pr = floatArrayOf(40f, 88f, 150f)
    val pl = doubleArrayOf(Astro.lon(252.25, 4.0923344, now), Astro.lon(181.98, 1.6021302, now), Astro.lon(355.43, 0.5240208, now))
    val ps = floatArrayOf(2.5f, 3f, 3f)
    val pc = intArrayOf(k("#d9a35c"), k("#e8c88a"), k("#d9704a"))
    for (i in 0 until 3) {
        val p = pp(pr[i], aE - (pl[i].toFloat() - lamE))
        dCirc(c, p[0], p[1], ps[i] + 3f, null, k("#4dffffff"), 1f)
        dCirc(c, p[0], p[1], ps[i], pc[i])
    }
    dCirc(c, mk[0], mk[1], 9f, null, RD, 1.5f)
    dCirc(c, mk[0], mk[1], 3.5f, RD)
    dTx(c, "+30D", mk[0] - 13f, mk[1] - 8f, 11f, MU, MONO, RGT)
    dCirc(c, ep[0], ep[1], 12f, null, k("#4dffffff"), 1f)
    dCirc(c, ep[0], ep[1], 5.5f, k("#5aa0e8"))
    dCirc(c, ep[0] - 1.5f, ep[1] - 1.8f, 2f, k("#e6ffffff"))
    dTx(c, "EARTH", ep[0] + 8f, ep[1] + 24f, 11f, TW, MONO, LFT, .14f)
    dTx(c, "SOL", 138f, 164f, 12f, TW, MONO, LFT, .25f)
}

private fun kv(c: Canvas, label: String, v: String, rx: Float, y: Float) {
    dTx(c, v, rx, y, 11f, GR, MONO, RGT)
    dTx(c, label, rx - dTw(v, 11f, MONO) - 6f, y, 11f, MU, MONO, RGT, .06f)
}

fun hmOrbit(d: D) {
    val c = d.c
    val w = d.w
    val e = Astro.earth(System.currentTimeMillis())
    val fs = min(w * .17f, 62f)
    val y1 = 6f + fs * .85f
    val hh = String.format(Locale.US, "%02d", d.now.get(Calendar.HOUR_OF_DAY))
    val mm = String.format(Locale.US, "%02d", d.now.get(Calendar.MINUTE))
    val hw = dTw(hh, fs, MONO)
    val mw = dTw(mm, fs, MONO)
    val gap = fs * .32f
    val x0 = (w - hw - mw - gap) / 2
    dTx(c, hh, x0, y1, fs, TW, MONO)
    dTx(c, mm, x0 + hw + gap, y1, fs, TW, MONO)
    dCirc(c, x0 + hw + gap / 2, y1 - fs * .5f, 3f, RD)
    dCirc(c, x0 + hw + gap / 2, y1 - fs * .22f, 3f, RD)
    dTx(c, SimpleDateFormat("EEE, dd MMM yyyy", Locale.ENGLISH).format(d.now.time).uppercase(), w / 2, y1 + 24f, 13f, TX, MONO, CTR, .18f)
    val ty = y1 + 52f
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    dTx(c, "SOL SYS //", 6f, ty, 11f, MU, MONO, LFT, .06f)
    dTx(c, "LIVE", 6f + dTw("SOL SYS // ", 11f, MONO, .06f), ty, 11f, RD, MONO, LFT, .06f)
    dTx(c, SimpleDateFormat("dd.MM.yyyy", Locale.US).format(d.now.time), 6f, ty + 16f, 11f, TX, MONO)
    dTx(c, String.format(Locale.US, "%02dH %02dM UTC", utc.get(Calendar.HOUR_OF_DAY), utc.get(Calendar.MINUTE)), 6f, ty + 32f, 11f, TX, MONO)
    kv(c, "DIST", String.format(Locale.US, "%.3f AU", e.r), w - 6f, ty)
    kv(c, "VEL", String.format(Locale.US, "%.2f km/s", e.v), w - 6f, ty + 16f)
    kv(c, "ORBIT", String.format(Locale.US, "%.1f%%", e.pct), w - 6f, ty + 32f)
    val top = ty + 44f
    val oh = d.h - top - 2f
    if (oh > 80f) {
        val s = min(w / 360f, oh / 330f)
        c.save()
        c.translate((w - 360f * s) / 2, top)
        c.scale(s, s)
        orbit(c, e)
        c.restore()
    }
}

fun hmLink(d: D) {
    panel(d)
    val c = d.c
    val cy = d.h / 2
    dCirc(c, 22f, cy, 4.5f, GR)
    dTx(c, "LINK STABLE", 40f, cy - 2f, 12f, GR, MONO, LFT, .16f)
    dTx(c, dFit(Store.quote(d.x, "hm"), d.w - 80f, 12f, MONO), 40f, cy + 15f, 12f, MU, MONO)
    dIco(c, "chev", d.w - 20f, cy, 14f, MU)
}

fun hmCal(d: D) {
    panel(d)
    val c = d.c
    val pad = 12f
    dTx(c, SimpleDateFormat("EEE, dd MMM", Locale.ENGLISH).format(d.now.time).uppercase(), pad, 24f, 11f, TX, MONO, LFT, .12f)
    dCirc(c, d.w - pad - 3f, 20f, 3.5f, RD)
    val mon = weekStart(d.now)
    val cw = (d.w - 2 * pad) / 7
    val y0 = 54f
    for (i in 0 until 7) {
        val cx = pad + cw * (i + .5f)
        val dd = mon.get(Calendar.DAY_OF_MONTH)
        dTx(c, "MTWTFSS"[i].toString(), cx, y0, 11f, MU, MONO, CTR)
        if (sameDay(mon, d.now)) {
            dRR(c, cx - cw * .45f, y0 + 10f, cx + cw * .45f, y0 + 36f, 6f, k("#2a1210"), RD, 1f)
            dTx(c, "$dd", cx, y0 + 28f, 12f, WHITE, MONO, CTR)
        } else dTx(c, "$dd", cx, y0 + 28f, 12f, TX, MONO, CTR)
        mon.add(Calendar.DAY_OF_MONTH, 1)
    }
    val cal2 = d.now.clone() as Calendar
    cal2.firstDayOfWeek = Calendar.MONDAY
    cal2.minimalDaysInFirstWeek = 4
    dTx(c, "WEEK ${cal2.get(Calendar.WEEK_OF_YEAR)} // " + SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(d.now.time).uppercase(),
        pad, d.h - 14f, 10f, MU, MONO, LFT, .08f)
}

fun hmStatus(d: D) {
    panel(d)
    val c = d.c
    val pad = 12f
    dTx(c, "SYSTEM STATUS", pad, 24f, 11f, TX, MONO, LFT, .14f)
    val bi: Intent? = d.x.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val lv = bi?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
    val sc = bi?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val pct = lv * 100 / max(1, sc)
    val sf = StatFs(Environment.getDataDirectory().path)
    val tot = sf.totalBytes.toFloat()
    val used = tot - sf.availableBytes.toFloat()
    val gb = 1024f * 1024f * 1024f
    val cm = d.x.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val nc = cm.getNetworkCapabilities(cm.activeNetwork)
    val net = when {
        nc == null -> "OFFLINE"
        nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WI-FI"
        nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE"
        else -> "ONLINE"
    }
    val step = (d.h - 40f) / 3
    val rw = d.w - pad
    var y = 48f
    dTx(c, "BATT", pad, y + 12f, 11f, MU, MONO, LFT, .08f); dTx(c, "$pct%", rw, y + 12f, 11f, GR, MONO, RGT)
    dBar(c, pad, rw, y + 22f, pct / 100f, GR, k("#1b2227"), 3f)
    y += step
    dTx(c, "STOR", pad, y + 12f, 11f, MU, MONO, LFT, .08f)
    dTx(c, "${(used / gb).roundToInt()}/${(tot / gb).roundToInt()} GB", rw, y + 12f, 11f, GR, MONO, RGT)
    dBar(c, pad, rw, y + 22f, used / tot, GR, k("#1b2227"), 3f)
    y += step
    dTx(c, "NET", pad, y + 12f, 11f, MU, MONO, LFT, .08f); dTx(c, net, rw, y + 12f, 11f, GR, MONO, RGT)
}

private fun hm(m: Float): String = String.format(Locale.US, "%02d:%02d", (m / 60).toInt(), (m % 60).toInt())

fun hmWeather(d: D) {
    panel(d)
    val c = d.c
    val pad = 12f
    val wx = Store.wx(d.x)
    dTx(c, "ATMOS // " + Store.cityName(d.x).uppercase(), pad, 24f, 12f, TX, MONO, LFT, .12f)
    dTx(c, SimpleDateFormat("dd.MM.yyyy", Locale.US).format(d.now.time), d.w - pad, 24f, 11f, MU, MONO, RGT)
    val py = 36f
    val pw = d.w - 2 * pad
    val ph = max(110f, d.h - py - pad - 96f)
    dRR(c, pad, py, pad + pw, py + ph, 10f, k("#05080a"), ED, 1f)
    val rise = (wx?.rise ?: 360).toFloat()
    val sunset = (wx?.set ?: 1080).toFloat()
    val nm = d.now.get(Calendar.HOUR_OF_DAY) * 60f + d.now.get(Calendar.MINUTE)
    val f = ((nm - rise) / (sunset - rise)).coerceIn(-.3f, 1.3f)
    val hy = py + ph * .74f
    val x0 = pad + pw * .12f
    val x1 = pad + pw * .88f
    val amp = ph * .52f
    val pif = PI.toFloat()
    c.save()
    c.clipRect(pad + 1f, py + 1f, pad + pw - 1f, py + ph - 1f)
    dRR(c, pad, hy, pad + pw, py + ph, 0f, k("#030506"))
    dLine(c, pad, hy, pad + pw, hy, k("#3a444c"), 1f)
    dLine(c, (x0 + x1) / 2, py + 10f, (x0 + x1) / 2, hy, k("#2c353c"), 1f, floatArrayOf(1f, 3f))
    dRR(c, pad + pw * .45f, py + ph * .2f, pad + pw * .75f, py + ph * .2f + 14f, 7f, k("#12cfd8dc"))
    dRR(c, pad + pw * .6f, py + ph * .12f, pad + pw * .82f, py + ph * .12f + 12f, 6f, k("#12cfd8dc"))
    val past = Path()
    val fut = Path()
    var pf = true
    var ff = true
    for (i in 0..40) {
        val t = i / 40f
        val x = x0 + (x1 - x0) * t
        val y = hy - amp * sin(pif * t)
        if (t <= f + .0001f) { if (pf) { past.moveTo(x, y); pf = false } else past.lineTo(x, y) }
        if (t >= f - .0251f) { if (ff) { fut.moveTo(x, y); ff = false } else fut.lineTo(x, y) }
    }
    dPath(c, fut, RD, false, 1.6f, floatArrayOf(5f, 4f), false)
    dPath(c, past, RD, false, 2f, null, false)
    val sx = x0 + (x1 - x0) * f
    val sy = hy - amp * sin(pif * f)
    val up = f >= 0f && f <= 1f
    dCirc(c, sx, sy, 16f, if (up) k("#1fff9d3c") else k("#0fff9d3c"))
    dCirc(c, sx, sy, 9f, null, if (up) WHITE else k("#808a93"), 1f)
    dCirc(c, sx, sy, 5f, if (up) k("#ff9d3c") else k("#7a5a3a"))
    val lt = String.format(Locale.US, "%02d:%02d", d.now.get(Calendar.HOUR_OF_DAY), d.now.get(Calendar.MINUTE))
    if (sx < pad + pw * .6f) dTx(c, lt, sx + 14f, sy + 24f, 11f, TW, MONO) else dTx(c, lt, sx - 14f, sy + 24f, 11f, TW, MONO, RGT)
    dCirc(c, x0, hy, 3.5f, k("#05080a"), WHITE, 1f)
    dCirc(c, x1, hy, 3.5f, k("#05080a"), WHITE, 1f)
    dTx(c, hm(rise), x0, hy + 18f, 11f, MU, MONO, CTR)
    dTx(c, hm(sunset), x1, hy + 18f, 11f, MU, MONO, CTR)
    c.restore()
    val day = nm >= rise && nm < sunset
    val left = if (day) sunset - nm else if (nm < rise) rise - nm else 1440f - nm + rise
    val lv = String.format(Locale.US, "%dH %02dM", (left / 60).toInt(), (left % 60).toInt())
    val cw = (d.w - 2 * pad - 14f) / 2
    fun cell(col: Int, row: Int, l: String, v: String, vc: Int) {
        val x = pad + col * (cw + 14f)
        val y = py + ph + 10f + row * 28f
        dLine(c, x, y, x + cw, y, k("#1b2227"), 1f)
        dTx(c, l, x, y + 19f, 11f, MU, MONO, LFT, .06f)
        dTx(c, v, x + cw, y + 19f, 12f, vc, MONO, RGT)
    }
    cell(0, 0, "TEMP", if (wx != null) "${wx.temp.roundToInt()}°C" else "--", TW)
    cell(1, 0, "SKY", Weather.label(wx?.code ?: 3), TW)
    cell(0, 1, "HI/LO", if (wx != null) "${wx.hi.roundToInt()}°/${wx.lo.roundToInt()}°" else "--", TW)
    cell(1, 1, if (day) "LIGHT LEFT" else "RISE IN", lv, GR)
    cell(0, 2, "RISE", hm(rise), TW)
    cell(1, 2, "SET", hm(sunset), TW)
}

fun hmTasks(d: D) {
    panel(d)
    val c = d.c
    val rh = d.h / 6
    val v = Store.view(d.x)
    dTx(c, "LOG // TASKS", 14f, rh * .64f, 11f, TX, MONO, LFT, .14f)
    dTx(c, "${v.count { it.second.d }}/${v.size}", d.w - 14f, rh * .64f, 11f, GR, MONO, RGT)
    for (i in 0 until 5) {
        val t = v.getOrNull(i)?.second ?: break
        val cy = rh * (i + 1.5f)
        dLine(c, 14f, rh * (i + 1), d.w - 14f, rh * (i + 1), k("#1b2227"), 1f)
        if (t.d) {
            dRR(c, 14f, cy - 7f, 28f, cy + 7f, 3f, k("#228fd19e"), GR, 1f)
            dIco(c, "check", 21f, cy, 10f, GR)
        } else dRR(c, 14f, cy - 7f, 28f, cy + 7f, 3f, null, k("#5d6a73"), 1f)
        val tx = dFit(t.t, d.w - 54f, 12f, MONO)
        dTx(c, tx, 40f, cy + 4f, 12f, if (t.d) MU else TW, MONO)
        if (t.d) dLine(c, 40f, cy + 1f, 40f + dTw(tx, 12f, MONO), cy + 1f, MU, 1f)
    }
}

fun hmMusic(d: D) {
    panel(d)
    val c = d.c
    val cy = d.h / 2
    val a = min(d.h - 28f, 58f)
    val top = cy - a / 2
    dRR(c, 14f, top, 14f + a, top + a, 8f, k("#0e1418"), ED, 1f)
    val hs = floatArrayOf(.25f, .6f, .85f, .45f, .7f)
    for (i in 0 until 5) {
        val bh = a * .6f * hs[i]
        val bx = 14f + a * .18f + i * a * .14f
        dRR(c, bx, top + a * .8f - bh, bx + a * .08f, top + a * .8f, 1f, GR)
    }
    val x0 = 28f + a
    val mw = d.w * .58f - x0
    dTx(c, dFit(musTitle(), mw, 13f, MONO), x0, cy - 8f, 13f, TW, MONO)
    dTx(c, dFit(musArtist().uppercase(), mw, 11f, MONO, .06f), x0, cy + 8f, 11f, MU, MONO, LFT, .06f)
    dBar(c, x0, x0 + mw, cy + 24f, musFrac(), RD, k("#1b2227"), 3f)
    val u = d.w / 7
    val ic = arrayOf("prev", if (PS.playing) "pause" else "play", "next")
    for (i in 0 until 3) {
        val bx = u * (4.5f + i)
        dRR(c, bx - 16f, cy - 16f, bx + 16f, cy + 16f, 8f, null, ED, 1f)
        dIco(c, ic[i], bx, cy, 15f, TX)
    }
}

// Earth night side: glowing atmosphere + city lights (decorative; remove the widget to drop it)
fun hmEarth(d: D) {
    val c = d.c
    val s = d.w / 360f
    c.save()
    c.scale(s, s)
    val rim = LinearGradient(0f, 0f, 360f, 0f,
        intArrayOf(k("#402f6fd0"), k("#7fb8ff"), k("#ffb066"), k("#ff7a3a")), floatArrayOf(0f, .45f, .8f, 1f), Shader.TileMode.CLAMP)
    val q = Paint(Paint.ANTI_ALIAS_FLAG)
    q.shader = rim
    q.style = Paint.Style.STROKE
    q.strokeWidth = 12f; q.alpha = 26
    c.drawCircle(180f, 500f, 432f, q)
    q.strokeWidth = 5f; q.alpha = 72
    c.drawCircle(180f, 500f, 426f, q)
    dCirc(c, 180f, 500f, 421f, k("#04070b"))
    q.strokeWidth = 1.5f; q.alpha = 255
    c.drawCircle(180f, 500f, 421f, q)
    val rnd = kotlin.random.Random(5)
    val cc = intArrayOf(k("#ffb347"), k("#ffd27a"), k("#ff8a3d"))
    val cl = floatArrayOf(60f, 40f, 150f, 30f, 240f, 45f, 320f, 35f)
    for (j in 0 until 4) for (i in 0 until 45) {
        val x = cl[j * 2] + (rnd.nextFloat() - .5f) * cl[j * 2 + 1] * 2
        val y = 500f - sqrt(421f * 421f - (x - 180f) * (x - 180f)) + 3f + rnd.nextFloat() * rnd.nextFloat() * 85f
        val r = .6f + rnd.nextFloat() * .7f
        val al = (255 * (.4f + rnd.nextFloat() * .55f)).toInt()
        if (y < 168f) dCirc(c, x, y, r, (cc[i % 3] and 0x00ffffff) or (al shl 24))
    }
    for (i in 0 until 50) {
        val x = rnd.nextFloat() * 360f
        val y = 500f - sqrt(421f * 421f - (x - 180f) * (x - 180f)) + 3f + rnd.nextFloat() * rnd.nextFloat() * 85f
        val r = .5f + rnd.nextFloat() * .5f
        val al = (255 * (.3f + rnd.nextFloat() * .4f)).toInt()
        if (y < 168f) dCirc(c, x, y, r, (cc[i % 3] and 0x00ffffff) or (al shl 24))
    }
    val tp = Path()
    tp.moveTo(0f, 140f)
    tp.quadTo(150f, 85f, 360f, 40f)
    dPath(c, tp, RD, false, 1.2f, floatArrayOf(5f, 5f), false)
    dCirc(c, 165f, 87.5f, 3f, RD)
    c.restore()
}
