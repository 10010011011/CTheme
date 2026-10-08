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

private val FR_T = TStyle(
    card = { card(it) }, title = "Tasks", tfH = MED, hSize = 15f, hLs = 0f, head = FG,
    tf = SANS, txt = FG, doneCol = MU, sub = MU,
    box = MU, boxFill = FG, boxDone = FG, check = WHITE,
    circle = false, rule = null, num = false, right = false, count = false
)

private val FR_M = MStyle(
    card = { card(it) },
    art = { d, l, t, s -> dArt(d.c, d.x, l, t, s, 14f, k("#cce6edf2"), FG) },
    tf = SANS, tfT = MED, title = FG, sub = MU, bar = FG, barBg = k("#33566673"), ico = FG,
    playBg = FG, playFg = WHITE, box = null, ls = 0f
)

fun frClock(d: D) {
    val t = String.format(Locale.US, "%02d:%02d", d.now.get(Calendar.HOUR_OF_DAY), d.now.get(Calendar.MINUTE))
    val sz = min(d.h * .5f, d.w * .24f)
    val y = d.h * .5f + sz * .18f
    dTx(d.c, t, d.w / 2, y, sz, FG, SERIF, CTR)
    val wx = Store.wx(d.x)
    val s = SimpleDateFormat("EEE, d MMM", Locale.ENGLISH).format(d.now.time) + (if (wx != null) "   |   ${wx.temp.roundToInt()}°C" else "")
    val fs = (sz * .27f).coerceIn(12f, 22f)
    dTx(d.c, s, d.w / 2, y + fs * 1.7f, fs, MU, SANS, CTR)
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
    val ch = min(cw * 1.15f, (d.h - top - pad) / 7)
    val fs = min(cw * .42f, ch * .5f).coerceIn(9f, 17f)
    for (i in 0 until 7) dTx(c, "MTWTFSS"[i].toString(), pad + cw * (i + .5f), top + ch * .7f, fs * .85f, MU, SANS, CTR)
    for (day in 1..n) {
        val i = off + day - 1
        val cx = pad + cw * (i % 7 + .5f)
        val cy = top + ch * (i / 7 + 1.5f)
        if (day == today) {
            dCirc(c, cx, cy, min(ch, cw) * .46f, FG)
            dTx(c, "$day", cx, cy + fs * .36f, fs, WHITE, SANS, CTR)
        } else dTx(c, "$day", cx, cy + fs * .36f, fs, FG, SANS, CTR)
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
    dLine(c, 18f, 22f, 30f, 22f, FG, 1f, null, true)
    dLine(c, 24f, 16f, 24f, 28f, FG, 1f, null, true)
    dFitPara(c, Store.quote(d.x, "fr"), 40f, 12f, d.w - 58f, d.h - 24f, 11f, 30f, FG, SERIFI, LFT, 0f, 1.3f)
}

fun frWeather(d: D) {
    val c = d.c
    val w = Store.wx(d.x)
    dImg(c, Img.get(d.x, R.drawable.wx_bg), 2f, 2f, d.w - 2, d.h - 2, 18f, .85f, .5f)
    dRR(c, 2f, 2f, d.w - 2, d.h - 2, 18f, k("#80f4f7fa"), EDGE, 1.2f)
    val code = w?.code ?: 3
    val temp = if (w != null) "${w.temp.roundToInt()}°C" else "--"
    val hl = if (w != null) "H ${w.hi.roundToInt()}°  L ${w.lo.roundToInt()}°" else ""
    if (d.w >= 230f) {
        val ts = min(d.h * .28f, 44f).coerceAtLeast(28f)
        dWx(c, code, 42f, d.h * .38f, min(d.h * .3f, 56f).coerceAtLeast(34f), FG)
        dTx(c, temp, d.w - 16f, d.h * .44f, ts, FG, SERIF, RGT)
        dTx(c, Store.cityName(d.x), d.w - 16f, d.h * .44f + ts * .55f, 13f, FG, SANS, RGT)
        dTx(c, Weather.label(code), 20f, d.h - 16f, 13f, FG, SANS)
        dTx(c, hl, d.w - 16f, d.h - 16f, 12f, MU, SANS, RGT)
    } else {
        dWx(c, code, 40f, d.h * .22f, 36f, FG)
        dTx(c, temp, 16f, d.h * .5f, 32f, FG, SERIF)
        dTx(c, Store.cityName(d.x), 16f, d.h * .5f + 20f, 13f, FG, SANS)
        dTx(c, Weather.label(code), 16f, d.h - 34f, 13f, FG, SANS)
        dTx(c, hl, 16f, d.h - 14f, 12f, MU, SANS)
    }
}

fun frTasks(d: D) = drawTasks(d, FR_T)

fun frMusic(d: D) = drawMusic(d, FR_M)
