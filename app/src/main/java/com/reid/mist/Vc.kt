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

private val VC_T = TStyle(
    card = { glass(it) }, title = "TODAY", tfH = SANS, hSize = 11f, hLs = .12f, head = M2,
    tf = SANS, txt = TXT, doneCol = k("#6f9a9b"), sub = M2,
    box = TL, boxFill = TL, boxDone = TL, check = WHITE,
    circle = true, rule = null, num = false, right = false, count = false
)

private val VC_M = MStyle(
    card = { glass(it) },
    art = { d, l, t, s -> dArt(d.c, d.x, l, t, s, 14f, TL2, WHITE) },
    tf = SANS, tfT = MED, title = TXT, sub = M2, bar = PK, barBg = k("#261f4e52"), ico = TXT,
    playBg = TL, playFg = WHITE, box = null, ls = 0f
)

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
    val narrow = rw < 150f
    val u = (2 * r) / 160f
    val top = cy - r
    dTx(c, String.format(Locale.US, "%02d", d.now.get(Calendar.DAY_OF_MONTH)), x0, top + 42f * u, 46f * u, TXT, LIGHT)
    dTx(c, SimpleDateFormat("MMM", Locale.ENGLISH).format(d.now.time).uppercase(), x0, top + 62f * u, 14f * u, TXT, SANS, LFT, .2f)
    dTx(c, SimpleDateFormat("EEEE", Locale.ENGLISH).format(d.now.time), x0, top + 82f * u, 16f * u, PK2, SERIFI)
    if (narrow) return
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

fun vcMusic(d: D) = drawMusic(d, VC_M)

fun vcTasks(d: D) = drawTasks(d, VC_T)

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
    dFitPara(c, Store.quote(d.x, "vc"), 20f, 8f, d.w * .58f, d.h - 16f, 11f, 28f, TXT, SERIFI, LFT, 0f, 1.25f)
}
