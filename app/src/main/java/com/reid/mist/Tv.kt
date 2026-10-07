package com.reid.mist

import android.graphics.Path
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

private val OR = k("#e8571f")
private val CR = k("#e9d5bd")
private val MUT = k("#a8794f")
private val BR = k("#6b3a1f")
private val PAN = k("#e60d0805")
private val PARCH = k("#d8c3a0")
private val INK = k("#2b1a0e")

private fun panel(d: D, rd: Float = 12f) = dRR(d.c, 1f, 1f, d.w - 1, d.h - 1, rd, PAN, BR, 1.2f)

fun tvHeader(d: D) {
    panel(d, 10f)
    val c = d.c
    val cy = d.h / 2
    dTx(c, "T V A", 16f, cy + 1f, 16f, OR, MONO, LFT, .2f)
    dTx(c, "MOBILE TERMINAL", 16f, cy + 16f, 10f, MUT, MONO, LFT, .08f)
    dTx(c, "TIME VARIANCE AUTHORITY", d.w - 16f, cy - 2f, 10f, MUT, MONO, RGT, .06f)
    dTx(c, "FOR ALL TIME. ALWAYS.", d.w - 16f, cy + 14f, 10f, MUT, MONO, RGT, .06f)
}

fun tvClock(d: D) {
    panel(d)
    val c = d.c
    val p = 14f
    dTx(c, SimpleDateFormat("EEE, d MMM", Locale.ENGLISH).format(d.now.time).uppercase(), p, 26f, 11f, MUT, MONO, LFT, .14f)
    val sz = min(d.h * .3f, (d.w - 2 * p - 34f) / 3.9f)
    val y = 30f + sz + 4f
    val t = String.format(Locale.US, "%02d:%02d", d.now.get(Calendar.HOUR_OF_DAY), d.now.get(Calendar.MINUTE))
    dTx(c, t, p, y, sz, OR, MONO)
    val hx = p + dTw(t, sz, MONO) + 16f
    val hy = y - sz * .36f
    val hs = sz * .36f
    val hp = Path()
    hp.moveTo(hx - hs, hy - hs); hp.lineTo(hx + hs, hy - hs); hp.lineTo(hx, hy); hp.close()
    hp.moveTo(hx, hy); hp.lineTo(hx + hs, hy + hs); hp.lineTo(hx - hs, hy + hs); hp.close()
    dPath(c, hp, OR, false, 2f)
    val ly = d.h - 30f
    dLine(c, p, ly, d.w - p, ly, BR, 1f)
    dCirc(c, p + (d.w - 2 * p) * .46f, ly, 3.5f, OR)
    dTx(c, "PAST", p, ly + 16f, 10f, MUT, MONO, LFT, .06f)
    dTx(c, "NOW", d.w / 2, ly + 16f, 10f, MUT, MONO, CTR, .06f)
    dTx(c, "FUTURE", d.w - p, ly + 16f, 10f, MUT, MONO, RGT, .06f)
}

fun tvWeather(d: D) {
    panel(d)
    val c = d.c
    val w = Store.wx(d.x)
    val code = w?.code ?: 3
    dWx(c, code, 34f, 38f, 34f, CR)
    dTx(c, if (w != null) "${w.temp.roundToInt()}°" else "--", 62f, 48f, 26f, CR, MONO)
    dTx(c, Weather.label(code), 14f, 78f, 12f, CR, MONO)
    dTx(c, Store.cityName(d.x).uppercase(), 14f, 96f, 10f, MUT, MONO, LFT, .12f)
    if (w != null) dTx(c, "H ${w.hi.roundToInt()}° / L ${w.lo.roundToInt()}°", 14f, d.h - 14f, 11f, MUT, MONO)
}

fun tvTimeline(d: D) {
    panel(d)
    val c = d.c
    val p = 14f
    val all = Store.tasks(d.x)
    val open = min(all.count { !it.d }, 7)
    val done = all.count { it.d }
    dTx(c, "SACRED TIMELINE", p, 24f, 10f, MUT, MONO, LFT, .12f)
    dTx(c, "STABLE", p, 44f, 15f, CR, MONO, LFT, .12f)
    dTx(c, "BRANCHES", d.w - p, 24f, 10f, MUT, MONO, RGT, .12f)
    dTx(c, if (open == 0) "CLEAN" else "$open OPEN", d.w - p, 44f, 15f, OR, MONO, RGT, .12f)
    val top = 66f
    val bot = d.h - 30f
    val my = (top + bot) / 2
    val bx = d.w * .34f
    val ex = d.w - p - 4f
    val alpha = k("#bfe8571f")
    for (i in 0 until open) {
        val ey = if (open == 1) top + 6f else top + 6f + (bot - top - 12f) * i / (open - 1f)
        val br = Path()
        br.moveTo(bx, my)
        br.cubicTo(bx + (ex - bx) * .45f, my, bx + (ex - bx) * .5f, ey, ex, ey)
        dPath(c, br, alpha, false, 1.4f)
        dCirc(c, ex, ey, 2.5f, OR)
    }
    for (j in 0 until min(done, 2)) {
        val ey = my + 12f + j * 11f
        val ex2 = bx + (ex - bx) * (.5f + j * .12f)
        val br = Path()
        br.moveTo(bx, my)
        br.cubicTo(bx + (ex2 - bx) * .4f, my, bx + (ex2 - bx) * .5f, ey, ex2, ey)
        dPath(c, br, BR, false, 1.2f, floatArrayOf(3f, 4f))
        dLine(c, ex2 - 4f, ey - 4f, ex2 + 4f, ey + 4f, MUT, 1.4f)
        dLine(c, ex2 - 4f, ey + 4f, ex2 + 4f, ey - 4f, MUT, 1.4f)
    }
    dLine(c, p / 2, my, d.w - p / 2, my, OR, 2.5f)
    dCirc(c, 24f, my, 6f, PAN, OR, 2f)
    dCirc(c, 24f, my, 2.5f, OR)
    dTx(c, "NOW", 24f, my + 20f, 10f, MUT, MONO, CTR)
    dTx(c, "Finish a task and its branch is pruned.", p, d.h - 10f, 10f, MUT, MONO)
}

fun tvLog(d: D) {
    val c = d.c
    dRR(c, 1f, 1f, d.w - 1, d.h - 1, 10f, PARCH)
    val rh = d.h / 6
    val v = Store.view(d.x)
    dTx(c, "VARIANCE LOG", 14f, rh * .64f, 12f, INK, MONO, LFT, .14f)
    for (i in 0 until 5) {
        val t = v.getOrNull(i)?.second ?: break
        val cy = rh * (i + 1.5f)
        dLine(c, 14f, rh * (i + 1), d.w - 14f, rh * (i + 1), k("#c9ae86"), 1f)
        dTx(c, String.format(Locale.US, "%02d", i + 1), 14f, cy + 4f, 11f, k("#7a4a25"), MONO)
        val tx = dFit(t.t, d.w - 90f, 12f, MONO)
        val tc = if (t.d) k("#8a7355") else INK
        dTx(c, tx, 42f, cy + 4f, 12f, tc, MONO)
        if (t.d) dLine(c, 42f, cy + 1f, 42f + dTw(tx, 12f, MONO), cy + 1f, tc, 1f)
        val cx = d.w - 22f
        if (t.d) {
            dCirc(c, cx, cy, 7f, k("#c4501b"))
            dIco(c, "check", cx, cy, 9f, PARCH)
        } else dCirc(c, cx, cy, 7f, null, k("#c4501b"), 1.5f)
    }
}

// Miss Minutes = shortcut to the phone's default assistant
fun tvMiss(d: D) {
    panel(d)
    val c = d.c
    val sz = min(d.h - 58f, d.w - 40f).coerceAtLeast(30f)
    dFitImg(c, Img.get(d.x, R.drawable.mm, 400), d.w / 2, 12f + sz / 2, sz)
    dTx(c, "MISS MINUTES", d.w / 2, d.h - 26f, 11f, CR, MONO, CTR, .12f)
    dTx(c, "Assistant", d.w / 2, d.h - 11f, 11f, MUT, MONO, CTR)
}

fun tvMusic(d: D) {
    panel(d)
    val c = d.c
    val cy = d.h / 2
    val r = min(d.h / 2 - 12f, 30f)
    val cx = 16f + r
    dCirc(c, cx, cy, r, k("#1a0f08"), BR, 1.2f)
    dCirc(c, cx, cy, r * .7f, null, k("#4d6b3a1f"), 1f)
    dCirc(c, cx, cy, r * .3f, OR)
    val x0 = 32f + 2 * r
    val mw = d.w * .58f - x0
    dTx(c, "NOW PLAYING", x0, cy - 14f, 10f, MUT, MONO, LFT, .1f)
    dTx(c, dFit(musTitle(), mw, 13f, MONO), x0, cy + 4f, 13f, CR, MONO)
    dBar(c, x0, x0 + mw, cy + 20f, musFrac(), OR, k("#3a2314"), 2f)
    val u = d.w / 7
    val ic = arrayOf("prev", if (PS.playing) "pause" else "play", "next")
    for (i in 0 until 3) dIco(c, ic[i], u * (4.5f + i), cy, 20f, OR)
}

fun tvQuote(d: D) {
    val q = Store.quote(d.x, "tv").uppercase()
    dTx(d.c, dFit(q, d.w - 16f, 11f, MONO, .16f), d.w / 2, d.h / 2 + 4f, 11f, MUT, MONO, CTR, .16f)
}
