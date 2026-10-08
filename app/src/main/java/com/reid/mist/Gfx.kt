package com.reid.mist

import android.content.Context
import android.graphics.*
import java.util.Calendar
import java.util.Locale
import kotlin.math.*

class Zone(val l: Float, val t: Float, val r: Float, val b: Float, val a: String)

class D(val c: Canvas, val w: Float, val h: Float, val x: Context) {
    val now: Calendar = Calendar.getInstance()
    val zones = ArrayList<Zone>()
    // declare a tappable rectangle (logical units) and the action it triggers
    fun zone(l: Float, t: Float, r: Float, b: Float, a: String) { zones.add(Zone(l, t, r, b, a)) }
}

val LFT = Paint.Align.LEFT
val CTR = Paint.Align.CENTER
val RGT = Paint.Align.RIGHT
val WHITE: Int = Color.WHITE
val SANS: Typeface = Typeface.SANS_SERIF
val LIGHT: Typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
val MED: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
val MONO: Typeface = Typeface.MONOSPACE
val SERIF: Typeface = Typeface.SERIF
val SERIFI: Typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)

fun k(s: String): Int = Color.parseColor(s)
fun rad(a: Float): Float = (a * PI / 180.0).toFloat()

private val P = Paint(Paint.ANTI_ALIAS_FLAG)

private fun pt(col: Int, st: Paint.Style = Paint.Style.FILL, sw: Float = 0f): Paint {
    P.reset(); P.isAntiAlias = true; P.color = col; P.style = st; P.strokeWidth = sw
    return P
}

fun dRR(c: Canvas, l: Float, t: Float, r: Float, b: Float, rd: Float, fill: Int?, line: Int? = null, sw: Float = 1f) {
    if (fill != null) c.drawRoundRect(RectF(l, t, r, b), rd, rd, pt(fill))
    if (line != null) c.drawRoundRect(RectF(l + sw / 2, t + sw / 2, r - sw / 2, b - sw / 2), rd, rd, pt(line, Paint.Style.STROKE, sw))
}

fun dCirc(c: Canvas, x: Float, y: Float, r: Float, fill: Int?, line: Int? = null, sw: Float = 1f) {
    if (fill != null) c.drawCircle(x, y, r, pt(fill))
    if (line != null) c.drawCircle(x, y, r, pt(line, Paint.Style.STROKE, sw))
}

fun dOval(c: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, rot: Float, col: Int, sw: Float = 1f, fill: Boolean = false) {
    c.save(); c.rotate(rot, cx, cy)
    c.drawOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), pt(col, if (fill) Paint.Style.FILL else Paint.Style.STROKE, sw))
    c.restore()
}

fun dLine(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, col: Int, sw: Float = 1f, dash: FloatArray? = null, round: Boolean = false) {
    val q = pt(col, Paint.Style.STROKE, sw)
    if (round) q.strokeCap = Paint.Cap.ROUND
    if (dash != null) q.pathEffect = DashPathEffect(dash, 0f)
    c.drawLine(x1, y1, x2, y2, q)
}

fun dPath(c: Canvas, p: Path, col: Int, fill: Boolean = false, sw: Float = 1f, dash: FloatArray? = null, round: Boolean = true) {
    val q = pt(col, if (fill) Paint.Style.FILL else Paint.Style.STROKE, sw)
    if (!fill && round) { q.strokeCap = Paint.Cap.ROUND; q.strokeJoin = Paint.Join.ROUND }
    if (dash != null) q.pathEffect = DashPathEffect(dash, 0f)
    c.drawPath(p, q)
}

fun dTx(c: Canvas, s: String, x: Float, y: Float, size: Float, col: Int, tf: Typeface = SANS, al: Paint.Align = LFT, ls: Float = 0f) {
    val q = pt(col); q.textSize = size; q.typeface = tf; q.textAlign = al; q.letterSpacing = ls
    c.drawText(s, x, y, q)
}

fun dTw(s: String, size: Float, tf: Typeface = SANS, ls: Float = 0f): Float {
    val q = pt(0); q.textSize = size; q.typeface = tf; q.letterSpacing = ls
    return q.measureText(s)
}

fun dFit(s: String, mw: Float, size: Float, tf: Typeface = SANS, ls: Float = 0f): String {
    if (dTw(s, size, tf, ls) <= mw) return s
    var t = s
    while (t.length > 1 && dTw("$t..", size, tf, ls) > mw) t = t.dropLast(1)
    return "$t.."
}

fun dLines(s: String, mw: Float, size: Float, tf: Typeface, ls: Float = 0f): List<String> {
    val out = ArrayList<String>()
    var line = ""
    for (w in s.split(" ")) {
        val t = if (line.isEmpty()) w else "$line $w"
        if (line.isNotEmpty() && dTw(t, size, tf, ls) > mw) { out.add(line); line = w } else line = t
    }
    if (line.isNotEmpty()) out.add(line)
    return out
}

// wrapped paragraph, vertically centred on cy
fun dPara(c: Canvas, s: String, x: Float, cy: Float, mw: Float, size: Float, col: Int, tf: Typeface, lh: Float, max: Int, al: Paint.Align = LFT, ls: Float = 0f) {
    val l = dLines(s, mw, size, tf, ls).take(max)
    var y = cy - (l.size - 1) * lh / 2 + size * .35f
    for (t in l) { dTx(c, t, x, y, size, col, tf, al, ls); y += lh }
}

fun dBar(c: Canvas, l: Float, r: Float, y: Float, f: Float, col: Int, bg: Int, hgt: Float = 3f) {
    dRR(c, l, y - hgt / 2, r, y + hgt / 2, hgt / 2, bg)
    if (f > 0f) dRR(c, l, y - hgt / 2, l + (r - l) * f.coerceIn(0f, 1f), y + hgt / 2, hgt / 2, col)
}

object Img {
    private val cache = HashMap<Int, Bitmap>()
    fun get(c: Context, id: Int, maxW: Int = 700): Bitmap? {
        cache[id]?.let { return it }
        return try {
            val o = BitmapFactory.Options(); o.inJustDecodeBounds = true
            BitmapFactory.decodeResource(c.resources, id, o)
            var s = 1
            while (o.outWidth / (s * 2) >= maxW) s *= 2
            val o2 = BitmapFactory.Options(); o2.inSampleSize = s
            val b = BitmapFactory.decodeResource(c.resources, id, o2)
            if (b != null) cache[id] = b
            b
        } catch (e: Throwable) { null }
    }
}

// centre-cropped image in a rounded rect
fun dImg(c: Canvas, b: Bitmap?, l: Float, t: Float, r: Float, bt: Float, rd: Float, fx: Float = .5f, fy: Float = .5f) {
    if (b == null) { dRR(c, l, t, r, bt, rd, k("#8899aa")); return }
    val dw = r - l; val dh = bt - t
    val sc = max(dw / b.width, dh / b.height)
    val m = Matrix(); m.setScale(sc, sc)
    m.postTranslate(l + (dw - b.width * sc) * fx, t + (dh - b.height * sc) * fy)
    val sh = BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP); sh.setLocalMatrix(m)
    val q = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG); q.shader = sh
    c.drawRoundRect(RectF(l, t, r, bt), rd, rd, q)
}

// whole image fitted inside a square, keeps transparency
fun dFitImg(c: Canvas, b: Bitmap?, cx: Float, cy: Float, size: Float) {
    if (b == null) return
    val s = size / max(b.width, b.height)
    val w = b.width * s; val h = b.height * s
    c.drawBitmap(b, null, RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
}

fun dIco(c: Canvas, n: String, cx: Float, cy: Float, s: Float, col: Int) {
    val p = Path()
    when (n) {
        "play" -> { p.moveTo(cx - s * .3f, cy - s * .45f); p.lineTo(cx + s * .5f, cy); p.lineTo(cx - s * .3f, cy + s * .45f); p.close() }
        "pause" -> {
            p.addRect(cx - s * .4f, cy - s * .42f, cx - s * .1f, cy + s * .42f, Path.Direction.CW)
            p.addRect(cx + s * .1f, cy - s * .42f, cx + s * .4f, cy + s * .42f, Path.Direction.CW)
        }
        "next" -> {
            p.moveTo(cx - s * .45f, cy - s * .42f); p.lineTo(cx + s * .25f, cy); p.lineTo(cx - s * .45f, cy + s * .42f); p.close()
            p.addRect(cx + s * .3f, cy - s * .42f, cx + s * .46f, cy + s * .42f, Path.Direction.CW)
        }
        "prev" -> {
            p.moveTo(cx + s * .45f, cy - s * .42f); p.lineTo(cx - s * .25f, cy); p.lineTo(cx + s * .45f, cy + s * .42f); p.close()
            p.addRect(cx - s * .46f, cy - s * .42f, cx - s * .3f, cy + s * .42f, Path.Direction.CW)
        }
        "check" -> {
            p.moveTo(cx - s * .35f, cy); p.lineTo(cx - s * .08f, cy + s * .28f); p.lineTo(cx + s * .38f, cy - s * .3f)
            dPath(c, p, col, false, s * .16f); return
        }
        "chev" -> {
            p.moveTo(cx - s * .15f, cy - s * .35f); p.lineTo(cx + s * .2f, cy); p.lineTo(cx - s * .15f, cy + s * .35f)
            dPath(c, p, col, false, s * .12f); return
        }
    }
    dPath(c, p, col, true)
}

fun dWx(c: Canvas, code: Int, cx: Float, cy: Float, s: Float, col: Int, night: Boolean = false) {
    if (code <= 1) {
        if (night) { dMoon(c, cx, cy, s, col); return }
        dCirc(c, cx, cy, s * .24f, col)
        for (i in 0 until 8) {
            val a = i * PI.toFloat() / 4
            dLine(c, cx + cos(a) * s * .34f, cy + sin(a) * s * .34f, cx + cos(a) * s * .48f, cy + sin(a) * s * .48f, col, s * .06f, null, true)
        }
        return
    }
    val wet = code in 51..99
    val y = if (wet) cy - s * .1f else cy
    dCirc(c, cx - s * .2f, y + s * .05f, s * .2f, col)
    dCirc(c, cx + s * .04f, y - s * .08f, s * .27f, col)
    dCirc(c, cx + s * .28f, y + s * .08f, s * .18f, col)
    dRR(c, cx - s * .4f, y + s * .05f, cx + s * .46f, y + s * .26f, s * .1f, col)
    if (wet) for (i in 0 until 3) dLine(c, cx - s * .2f + i * s * .22f, y + s * .36f, cx - s * .26f + i * s * .22f, y + s * .5f, col, s * .06f, null, true)
}

fun weekStart(now: Calendar): Calendar {
    val m = now.clone() as Calendar
    m.add(Calendar.DAY_OF_MONTH, -((m.get(Calendar.DAY_OF_WEEK) + 5) % 7))
    return m
}

fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

fun mmss(ms: Int): String { val s = ms / 1000; return String.format(Locale.US, "%d:%02d", s / 60, s % 60) }
fun musTitle(): String = if (PS.title.isEmpty()) "Nothing playing" else PS.title
fun musArtist(): String = if (PS.title.isEmpty()) "Tap play" else PS.artist
fun musFrac(): Float = if (PS.dur > 0) PS.pos.toFloat() / PS.dur else 0f

fun dMoon(c: Canvas, cx: Float, cy: Float, s: Float, col: Int) {
    val a = Path(); a.addCircle(cx, cy, s * .3f, Path.Direction.CW)
    val b = Path(); b.addCircle(cx + s * .14f, cy - s * .08f, s * .27f, Path.Direction.CW)
    a.op(b, Path.Op.DIFFERENCE)
    dPath(c, a, col, true)
}

// music logo (two beamed notes)
fun dNote(c: Canvas, cx: Float, cy: Float, s: Float, col: Int) {
    val x1 = cx - s * .22f
    val x2 = cx + s * .26f
    val y1 = cy + s * .3f
    val y2 = cy + s * .22f
    dOval(c, x1, y1, s * .17f, s * .12f, -20f, col, 1f, true)
    dOval(c, x2, y2, s * .17f, s * .12f, -20f, col, 1f, true)
    val sw = s * .06f
    val s1 = x1 + s * .15f
    val s2 = x2 + s * .15f
    dLine(c, s1, y1 - s * .02f, s1, cy - s * .38f, col, sw)
    dLine(c, s2, y2 - s * .02f, s2, cy - s * .46f, col, sw)
    val p = Path()
    p.moveTo(s1, cy - s * .38f); p.lineTo(s2, cy - s * .46f); p.lineTo(s2, cy - s * .32f); p.lineTo(s1, cy - s * .24f); p.close()
    dPath(c, p, col, true)
}

// song cover if the user added one for the current song, otherwise a music logo tile
fun dArt(c: Canvas, x: Context, l: Float, t: Float, s: Float, rd: Float, bg: Int, fg: Int, line: Int? = null) {
    val cv = Cover.get(x, PS.id)
    if (cv != null) { dImg(c, cv, l, t, l + s, t + s, rd); return }
    dRR(c, l, t, l + s, t + s, rd, bg, line, 1f)
    dNote(c, l + s / 2, t + s / 2, s * .62f, fg)
}

fun dLinesMax(s: String, mw: Float, size: Float, tf: Typeface, ls: Float, max: Int): List<String> {
    val l = dLines(s, mw, size, tf, ls)
    if (l.size <= max) return l
    val out = ArrayList<String>(l.take(max - 1))
    out.add(dFit(l.drop(max - 1).joinToString(" "), mw, size, tf, ls))
    return out
}

// largest font (between minS and maxS) at which the wrapped text fills the box without overflowing
fun dFitPara(c: Canvas, s: String, l: Float, t: Float, w: Float, h: Float, minS: Float, maxS: Float, col: Int, tf: Typeface,
             al: Paint.Align = LFT, ls: Float = 0f, lhm: Float = 1.3f) {
    var sz = maxS
    var lines = dLines(s, w, sz, tf, ls)
    while (sz > minS && lines.size * sz * lhm > h) { sz -= .5f; lines = dLines(s, w, sz, tf, ls) }
    val maxL = max(1, (h / (sz * lhm)).toInt())
    if (lines.size > maxL) lines = dLinesMax(s, w, sz, tf, ls, maxL)
    val lh = sz * lhm
    val x = when (al) {
        Paint.Align.CENTER -> l + w / 2
        Paint.Align.RIGHT -> l + w
        else -> l
    }
    var y = t + (h - lines.size * lh) / 2 + lh / 2 + sz * .35f
    for (ln in lines) { dTx(c, ln, x, y, sz, col, tf, al, ls); y += lh }
}
