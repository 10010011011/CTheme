package com.reid.mist

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val TL = k("#39b8b0")
private val TL2 = k("#7fd1c9")
private val PK = k("#f4a6b7")
private val PK2 = k("#e0788f")
private val TXT = k("#1f4e52")
private val M2 = k("#4c8a8c")

private fun glass(d: D) = dRR(d.c, 2f, 2f, d.w - 2, d.h - 2, 20f, k("#99ffffff"), k("#d9ffffff"), 1.2f)

private fun hand(c: Canvas, cx: Float, cy: Float, deg: Float, l: Float, w: Float) {
    val a = rad(deg)
    dLine(c, cx, cy, cx + sin(a) * l, cy - cos(a) * l, TXT, w, null, true)
}

fun vcClock(d: D) {
    val c = d.c
    val r = min(d.h / 2 - 4f, d.w * .24f)
    val cx = 6f + r
    val cy = d.h / 2
    dCirc(c, cx, cy, r, k("#66ffffff"), WHITE, 2f)
    for (i in 0 until 3) dCirc(c, cx, cy, r * (.86f - i * .12f), null, k("#4d39b8b0"), 1f)
    dCirc(c, cx, cy, r * .43f, k("#bf7fd1c9"), WHITE, 1.5f)
    for (i in 0 until 4) {
        val a = i * PI.toFloat() / 2
        dLine(c, cx + sin(a) * r * .9f, cy - cos(a) * r * .9f, cx + sin(a) * r * .8f, cy - cos(a) * r * .8f, TXT, 2f, null, true)
    }
    val mi = d.now.get(Calendar.MINUTE)
    val hh = d.now.get(Calendar.HOUR) + mi / 60f
    hand(c, cx, cy, hh * 30f, r * .38f, 4f)
    hand(c, cx, cy, mi * 6f, r * .58f, 2.5f)
    dCirc(c, cx, cy, 4f, PK, TXT, 1.5f)
    val px = cx + r * .9f
    val py = cy - r * .9f
    val hx = cx + r * .4f
    val hy = cy - r * .12f
    dLine(c, px, py, hx, hy, TXT, 3f, null, true)
    dCirc(c, px, py, 6f, WHITE, TL, 2f)
    dRR(c, hx - 6f, hy - 4f, hx + 6f, hy + 5f, 3f, TXT)

    val x0 = 2 * r + 24f
    val rw = d.w - x0 - 10f
    val u = (2 * r) / 160f
    val top = cy - r
    dTx(c, String.format(Locale.US, "%02d", d.now.get(Calendar.DAY_OF_MONTH)), x0, top + 42f * u, 46f * u, TXT, LIGHT)
    dTx(c, SimpleDateFormat("MMM", Locale.ENGLISH).format(d.now.time).uppercase(), x0, top + 62f * u, 14f * u, TXT, SANS, LFT, .2f)
    dTx(c, SimpleDateFormat("EEEE", Locale.ENGLISH).format(d.now.time), x0, top + 82f * u, 16f * u, PK2, SERIFI)
    val mon = weekStart(d.now)
    val cw = rw / 7
    for (i in 0 until 7) {
        val x = x0 + cw * (i + .5f)
        dTx(c, "MTWTFSS"[i].toString(), x, top + 104f * u, 11f, M2, SANS, CTR)
        if (sameDay(mon, d.now)) {
            dRR(c, x - cw * .45f, top + 109f * u, x + cw * .45f, top + 109f * u + 18f, 9f, PK)
            dTx(c, "${mon.get(Calendar.DAY_OF_MONTH)}", x, top + 109f * u + 13f, 11f, WHITE, SANS, CTR)
        } else dTx(c, "${mon.get(Calendar.DAY_OF_MONTH)}", x, top + 109f * u + 13f, 11f, TXT, SANS, CTR)
        mon.add(Calendar.DAY_OF_MONTH, 1)
    }
    val wx = Store.wx(d.x)
    val pt0 = top + 138f * u
    val pr = min(x0 + 160f, d.w - 8f)
    dRR(c, x0, pt0, pr, pt0 + 24f, 12f, k("#99ffffff"), k("#d9ffffff"), 1f)
    dWx(c, wx?.code ?: 3, x0 + 14f, pt0 + 12f, 16f, TXT)
    dTx(c, dFit((if (wx != null) "${wx.temp.roundToInt()}°C  " else "") + Store.cityName(d.x), pr - x0 - 34f, 12f), x0 + 28f, pt0 + 16.5f, 12f, TXT)
}

private fun art(c: Canvas, l: Float, t: Float, s: Float) {
    c.save()
    c.translate(l, t)
    c.scale(s / 70f, s / 70f)
    val clip = Path()
    clip.addRoundRect(RectF(0f, 0f, 70f, 70f), 14f, 14f, Path.Direction.CW)
    c.clipPath(clip)
    dRR(c, 0f, 0f, 70f, 70f, 0f, TL2)
    val p1 = Path()
    p1.moveTo(-5f, 52f); p1.cubicTo(20f, 32f, 40f, 62f, 75f, 27f)
    dPath(c, p1, k("#d9ffffff"), false, 7f)
    val p2 = Path()
    p2.moveTo(-5f, 64f); p2.cubicTo(25f, 47f, 45f, 72f, 75f, 44f)
    dPath(c, p2, PK, false, 5f)
    dCirc(c, 48f, 18f, 8f, k("#b3ffffff"))
    c.restore()
}

fun vcMusic(d: D) {
    val c = d.c
    glass(d)
    val rh = d.h / 3
    val a = min(rh * 2 - 16f, 78f)
    art(c, 14f, 14f, a)
    val x0 = 28f + a
    val mw = d.w - x0 - 60f
    dTx(c, dFit(musTitle(), mw, 15f, MED), x0, 32f, 15f, TXT, MED)
    dTx(c, dFit(musArtist(), mw, 12f), x0, 50f, 12f, M2)
    val hs = floatArrayOf(.55f, 1f, .45f, .85f, .6f)
    for (i in 0 until 5) {
        val bh = 20f * hs[i]
        dRR(c, d.w - 50f + i * 7f, 38f - bh, d.w - 46f + i * 7f, 38f, 2f, if (i % 2 == 0) TL else PK)
    }
    val by = 14f + a - 14f
    dBar(c, x0, d.w - 14f, by, musFrac(), PK, k("#26" + "1f4e52"), 4f)
    dTx(c, mmss(PS.pos), x0, by + 14f, 10f, M2)
    dTx(c, mmss(PS.dur), d.w - 14f, by + 14f, 10f, M2, SANS, RGT)
    val cy = rh * 2.5f
    val u = d.w / 7
    dIco(c, "prev", u * 2.5f, cy, 18f, TXT)
    dCirc(c, u * 3.5f, cy, 20f, TL)
    dIco(c, if (PS.playing) "pause" else "play", u * 3.5f, cy, 17f, WHITE)
    dIco(c, "next", u * 4.5f, cy, 18f, TXT)
}

fun vcTasks(d: D) {
    glass(d)
    val c = d.c
    val rh = d.h / 5
    val v = Store.view(d.x)
    dTx(c, "TODAY", 16f, rh * .66f, 11f, M2, SANS, LFT, .12f)
    for (i in 0 until 4) {
        val t = v.getOrNull(i)?.second ?: break
        val cy = rh * (i + 1.5f)
        if (t.d) {
            dCirc(c, 24f, cy, 8f, TL)
            dIco(c, "check", 24f, cy, 10f, WHITE)
        } else dCirc(c, 24f, cy, 8f, null, TL, 1.5f)
        val tx = dFit(t.t, d.w - 60f, 13f)
        val tc = if (t.d) k("#6f9a9b") else TXT
        dTx(c, tx, 40f, cy + 4.5f, 13f, tc)
        if (t.d) dLine(c, 40f, cy + 1.5f, 40f + dTw(tx, 13f), cy + 1.5f, tc, 1f)
    }
}

fun vcRing(d: D) {
    glass(d)
    val c = d.c
    val l = Store.tasks(d.x)
    val tot = l.size
    val dn = l.count { it.d }
    val r = min(d.w, d.h) * .3f
    val cx = d.w / 2
    val cy = d.h * .45f
    val sw = r * .28f
    dCirc(c, cx, cy, r, null, k("#3339b8b0"), sw)
    if (tot > 0 && dn > 0) {
        val q = Paint(Paint.ANTI_ALIAS_FLAG)
        q.style = Paint.Style.STROKE
        q.strokeWidth = sw
        q.strokeCap = Paint.Cap.ROUND
        q.color = PK
        c.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, 360f * dn / tot, false, q)
    }
    dTx(c, "$dn/$tot", cx, cy + r * .2f, r * .55f, TXT, MED, CTR)
    dTx(c, "Keep going", cx, d.h - 14f, 11f, M2, SANS, CTR, .06f)
}

fun vcBanner(d: D) {
    val c = d.c
    glass(d)
    c.save()
    val clip = Path()
    clip.addRoundRect(RectF(2f, 2f, d.w - 2, d.h - 2), 20f, 20f, Path.Direction.CW)
    c.clipPath(clip)
    val s = d.w / 352f
    c.translate(0f, (d.h - 110f * s) / 2)
    c.scale(s, s)
    val p1 = Path()
    p1.moveTo(-10f, 70f); p1.cubicTo(60f, 20f, 120f, 110f, 200f, 60f); p1.cubicTo(250f, 30f, 320f, 20f, 370f, 50f)
    dPath(c, p1, k("#4d39b8b0"), false, 12f)
    val p2 = Path()
    p2.moveTo(-10f, 92f); p2.cubicTo(70f, 52f, 130f, 122f, 210f, 82f); p2.cubicTo(260f, 60f, 330f, 52f, 370f, 77f)
    dPath(c, p2, k("#8c7fd1c9"), false, 8f)
    val p3 = Path()
    p3.moveTo(-10f, 50f); p3.cubicTo(50f, 5f, 130f, 75f, 220f, 35f); p3.cubicTo(260f, 15f, 330f, 0f, 370f, 25f)
    dPath(c, p3, k("#99f4a6b7"), false, 5f)
    val pc = k("#ccf4a6b7")
    dOval(c, 250f, 22f, 5f, 3f, 30f, pc, 1f, true)
    dOval(c, 300f, 88f, 5f, 3f, -20f, pc, 1f, true)
    dOval(c, 330f, 40f, 4f, 2.5f, 50f, pc, 1f, true)
    dOval(c, 190f, 98f, 4f, 2.5f, 10f, pc, 1f, true)
    // music note
    dOval(c, 322f, 70f, 5f, 3.6f, -20f, k("#b31f4e52"), 1f, true)
    dLine(c, 326.5f, 69f, 326.5f, 50f, k("#b31f4e52"), 1.8f)
    dLine(c, 326.5f, 50f, 334f, 56f, k("#b31f4e52"), 1.8f, null, true)
    c.restore()
    dPara(c, Store.quote(d.x, "vc"), 20f, d.h / 2, d.w * .58f, min(d.h * .17f, 19f), TXT, SERIFI, min(d.h * .24f, 24f), 3)
}
