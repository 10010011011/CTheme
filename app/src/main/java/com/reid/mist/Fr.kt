package com.reid.mist

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

private val FG = k("#2f3b45")
private val MU = k("#566673")
private val CARD = k("#b8f1f5f8")
private val EDGE = k("#d0ffffff")

private fun card(d: D) = dRR(d.c, 2f, 2f, d.w - 2, d.h - 2, 18f, CARD, EDGE, 1.2f)

fun frClock(d: D) {
    val t = String.format(Locale.US, "%02d:%02d", d.now.get(Calendar.HOUR_OF_DAY), d.now.get(Calendar.MINUTE))
    val sz = min(d.h * .5f, 58f)
    val y = d.h * .5f + sz * .15f
    dTx(d.c, t, d.w / 2, y, sz, FG, SERIF, CTR)
    val wx = Store.wx(d.x)
    val s = SimpleDateFormat("EEE, d MMM", Locale.ENGLISH).format(d.now.time) + (if (wx != null) "   |   ${wx.temp.roundToInt()}°C" else "")
    dTx(d.c, s, d.w / 2, y + 22f, 14f, MU, SANS, CTR)
}

fun frCal(d: D) {
    val c = d.c
    card(d)
    val pad = 12f
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_MONTH)
    dTx(c, SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(cal.time).uppercase(), pad, pad + 12f, 11f, MU, SANS, LFT, .08f)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val off = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val n = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cw = (d.w - 2 * pad) / 7
    val top = pad + 22f
    val ch = min(cw, (d.h - top - pad) / 7)
    for (i in 0 until 7) dTx(c, "MTWTFSS"[i].toString(), pad + cw * (i + .5f), top + ch * .7f, 10f, MU, SANS, CTR)
    for (day in 1..n) {
        val i = off + day - 1
        val cx = pad + cw * (i % 7 + .5f)
        val cy = top + ch * (i / 7 + 1.5f)
        if (day == today) {
            dCirc(c, cx, cy, ch * .44f, FG)
            dTx(c, "$day", cx, cy + 4f, 11f, WHITE, SANS, CTR)
        } else dTx(c, "$day", cx, cy + 4f, 11f, FG, SANS, CTR)
    }
}

private fun frImg(d: D, id: Int, fx: Float, fy: Float) {
    dImg(d.c, Img.get(d.x, id), 2f, 2f, d.w - 2, d.h - 2, 18f, fx, fy)
    dRR(d.c, 2f, 2f, d.w - 2, d.h - 2, 18f, null, EDGE, 2f)
}

fun frPhotoA(d: D) = frImg(d, R.drawable.fr_a, .5f, .35f)
fun frPhotoB(d: D) = frImg(d, R.drawable.fr_b, .5f, .5f)

fun frQuote(d: D) {
    val c = d.c
    dImg(c, Img.get(d.x, R.drawable.wx_bg), 2f, 2f, d.w - 2, d.h - 2, 18f, .08f, .5f)
    dRR(c, 2f, 2f, d.w - 2, d.h - 2, 18f, k("#b3f3f6f9"), EDGE, 1.2f)
    val sx = 24f
    val sy = d.h * .24f
    dLine(c, sx - 6f, sy, sx + 6f, sy, FG, 1f, null, true)
    dLine(c, sx, sy - 6f, sx, sy + 6f, FG, 1f, null, true)
    dPara(c, Store.quote(d.x, "fr"), 40f, d.h * .56f, d.w - 70f, min(d.h * .16f, 16f), FG, SERIFI, min(d.h * .22f, 22f), 4)
}

fun frWeather(d: D) {
    val c = d.c
    val w = Store.wx(d.x)
    dImg(c, Img.get(d.x, R.drawable.wx_bg), 2f, 2f, d.w - 2, d.h - 2, 18f, .85f, .5f)
    dRR(c, 2f, 2f, d.w - 2, d.h - 2, 18f, k("#80f4f7fa"), EDGE, 1.2f)
    val code = w?.code ?: 3
    dWx(c, code, 42f, d.h * .36f, 40f, FG)
    dTx(c, Weather.label(code), 20f, d.h - 16f, 13f, FG, SANS)
    dTx(c, if (w != null) "${w.temp.roundToInt()}°C" else "--", d.w - 16f, d.h * .42f, 32f, FG, SERIF, RGT)
    dTx(c, Store.cityName(d.x), d.w - 16f, d.h * .42f + 20f, 13f, FG, SANS, RGT)
    if (w != null) dTx(c, "H ${w.hi.roundToInt()}°  L ${w.lo.roundToInt()}°", d.w - 16f, d.h - 16f, 12f, MU, SANS, RGT)
}

fun frTasks(d: D) {
    val c = d.c
    card(d)
    val v = Store.view(d.x)
    val rh = d.h / 5
    dTx(c, "Tasks", 16f, rh * .68f, 15f, FG, MED)
    for (i in 0 until 4) {
        val t = v.getOrNull(i)?.second ?: break
        val cy = rh * (i + 1.5f)
        if (t.d) {
            dRR(c, 16f, cy - 7f, 30f, cy + 7f, 3f, FG)
            dIco(c, "check", 23f, cy, 11f, WHITE)
        } else dRR(c, 16f, cy - 7f, 30f, cy + 7f, 3f, null, MU, 1.5f)
        val tx = dFit(t.t, d.w - 62f, 13f)
        dTx(c, tx, 40f, cy + 4.5f, 13f, if (t.d) MU else FG)
        if (t.d) dLine(c, 40f, cy + 1.5f, 40f + dTw(tx, 13f), cy + 1.5f, MU, 1f)
    }
}

fun frMusic(d: D) {
    val c = d.c
    card(d)
    val rh = d.h / 3
    val a = min(rh * 2 - 18f, 84f)
    dImg(c, Img.get(d.x, R.drawable.fr_b), 14f, 14f, 14f + a, 14f + a, 14f, .5f, .3f)
    val x0 = 28f + a
    val mw = d.w - x0 - 14f
    dTx(c, dFit(musTitle(), mw, 15f, MED), x0, 31f, 15f, FG, MED)
    dTx(c, dFit(musArtist(), mw, 12f), x0, 49f, 12f, MU)
    val by = 14f + a - 14f
    dBar(c, x0, d.w - 14f, by, musFrac(), FG, k("#33566673"), 4f)
    dTx(c, mmss(PS.pos), x0, by + 14f, 10f, MU)
    dTx(c, mmss(PS.dur), d.w - 14f, by + 14f, 10f, MU, SANS, RGT)
    val cy = rh * 2.5f
    val u = d.w / 7
    dIco(c, "prev", u * 2.5f, cy, 18f, FG)
    dCirc(c, u * 3.5f, cy, 19f, FG)
    dIco(c, if (PS.playing) "pause" else "play", u * 3.5f, cy, 16f, WHITE)
    dIco(c, "next", u * 4.5f, cy, 18f, FG)
}
