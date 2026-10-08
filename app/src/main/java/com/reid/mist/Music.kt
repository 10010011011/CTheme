package com.reid.mist

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object PS {
    var id = -1L
    var title = ""
    var artist = ""
    var pos = 0
    var dur = 0
    var playing = false
}

object Music {
    fun send(c: Context, a: String, id: Long = -1L, ms: Int = 0) {
        c.startForegroundService(Intent(c, PlayerService::class.java).setAction(a).putExtra("id", id).putExtra("ms", ms))
    }

    fun cmd(c: Context, a: String) = send(c, a)
}

// optional per-song cover images, stored privately inside the app
object Cover {
    private val cache = HashMap<Long, Bitmap>()
    private fun f(c: Context, id: Long) = File(c.filesDir, "cover_$id.jpg")

    fun has(c: Context, id: Long): Boolean = id >= 0 && f(c, id).exists()

    fun get(c: Context, id: Long): Bitmap? {
        if (id < 0) return null
        cache[id]?.let { return it }
        val file = f(c, id)
        if (!file.exists()) return null
        val b = BitmapFactory.decodeFile(file.path) ?: return null
        cache[id] = b
        return b
    }

    fun save(c: Context, id: Long, uri: Uri): Boolean {
        try {
            val o = BitmapFactory.Options()
            o.inJustDecodeBounds = true
            c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
            var s = 1
            while (o.outWidth / (s * 2) >= 600 && o.outHeight / (s * 2) >= 600) s *= 2
            val o2 = BitmapFactory.Options()
            o2.inSampleSize = s
            val b = c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o2) } ?: return false
            val side = min(b.width, b.height)
            val sq = Bitmap.createBitmap(b, (b.width - side) / 2, (b.height - side) / 2, side, side)
            val out = Bitmap.createScaledBitmap(sq, min(side, 480), min(side, 480), true)
            FileOutputStream(f(c, id)).use { out.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            cache.remove(id)
            return true
        } catch (e: Exception) { return false }
    }

    fun remove(c: Context, id: Long) { f(c, id).delete(); cache.remove(id) }
}

class PlayerService : Service() {
    private var mp: MediaPlayer? = null
    private val q = ArrayList<Long>()
    private var qi = -1
    private var n = 0
    private val h = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            n++
            mp?.let { try { PS.pos = it.currentPosition; PS.dur = it.duration } catch (e: Exception) { } }
            if (n % 5 == 0) sync()
            if (PS.playing) h.postDelayed(this, 1000)
        }
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        fg()
        val ex = i ?: Intent()
        when (ex.action) {
            "pp" -> toggle()
            "nx" -> play(qi + 1)
            "pv" -> play(if (qi < 0) 0 else qi - 1)
            "pl" -> {
                if (q.isEmpty()) load()
                val ix = q.indexOf(ex.getLongExtra("id", -1L))
                play(if (ix < 0) 0 else ix)
            }
            "sk" -> { mp?.seekTo(ex.getIntExtra("ms", 0)); sync() }
            "st" -> { stopAll(); return START_NOT_STICKY }
        }
        if (mp == null) stopSelf()
        return START_NOT_STICKY
    }

    private fun stopAll() {
        h.removeCallbacks(tick)
        mp?.release(); mp = null
        PS.playing = false; PS.pos = 0
        stopForeground(true)
        stopSelf()
        Render.refreshAll(this) { it.endsWith("music") }
    }

    private fun fg() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("mist", "Playback", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "mist")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(if (PS.title.isEmpty()) "Mist player" else PS.title)
            .setContentText(PS.artist)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop",
                PendingIntent.getService(this, 5, Intent(this, PlayerService::class.java).setAction("st"), PendingIntent.FLAG_IMMUTABLE))
            .build()
        startForeground(7, n)
    }

    private fun load() {
        q.clear()
        val cur = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Audio.Media._ID),
            MediaStore.Audio.Media.IS_MUSIC + " != 0", null, MediaStore.Audio.Media.TITLE + " COLLATE NOCASE")
        cur?.use { while (it.moveToNext()) q.add(it.getLong(0)) }
    }

    private fun play(i: Int) {
        if (q.isEmpty()) load()
        if (q.isEmpty()) {
            PS.title = "No songs found"; PS.artist = "Add audio files to the phone"; PS.playing = false
            sync(); return
        }
        qi = ((i % q.size) + q.size) % q.size
        PS.id = q[qi]
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, q[qi])
        try {
            mp?.release()
            val m = MediaPlayer()
            m.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            m.setDataSource(this, uri)
            m.setOnCompletionListener { play(qi + 1) }
            m.prepare(); m.start(); mp = m
            meta(uri)
            PS.dur = m.duration; PS.pos = 0; PS.playing = true
            focus(); fg()
        } catch (e: Exception) {
            PS.title = "Can't play this file"; PS.artist = ""; PS.playing = false
        }
        sync()
        h.removeCallbacks(tick); h.postDelayed(tick, 1000)
    }

    private fun meta(u: Uri) {
        try {
            val r = MediaMetadataRetriever()
            r.setDataSource(this, u)
            PS.title = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Track ${qi + 1}"
            PS.artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown artist"
            r.release()
        } catch (e: Exception) { PS.title = "Track ${qi + 1}"; PS.artist = "" }
    }

    private fun toggle() {
        val m = mp
        if (m == null) { play(if (qi < 0) 0 else qi); return }
        if (m.isPlaying) {
            m.pause(); PS.playing = false
        } else {
            m.start(); PS.playing = true; fg()
            h.removeCallbacks(tick); h.postDelayed(tick, 1000)
        }
        sync()
    }

    private fun focus() {
        (getSystemService(AUDIO_SERVICE) as AudioManager).requestAudioFocus({ f ->
            if (f == AudioManager.AUDIOFOCUS_LOSS || f == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                mp?.let { if (it.isPlaying) it.pause() }
                PS.playing = false; sync()
            }
        }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
    }

    private fun sync() {
        mp?.let { try { PS.pos = it.currentPosition; PS.dur = it.duration } catch (e: Exception) { } }
        Render.refreshAll(this) { it.endsWith("music") }
    }

    override fun onDestroy() {
        h.removeCallbacks(tick)
        mp?.release(); mp = null
        PS.playing = false
        super.onDestroy()
    }
}
