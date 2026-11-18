package com.ff6locke.gplayer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var videoLayout: VLCVideoLayout
    private lateinit var status: TextView
    private var libVlc: LibVLC? = null
    private var player: MediaPlayer? = null
    private var playing = false
    private val openVideoRequest = 4101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildHome()
    }

    private fun buildHome() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 28, 48, 28)
            setBackgroundColor(0xFF101318.toInt())
        }
        val title = TextView(this).apply {
            text = "GPLAYER"
            textSize = 32f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "Media player for Android TV"
            textSize = 16f
            setTextColor(0xFFB8C0CC.toInt())
            gravity = Gravity.CENTER
        }
        val open = Button(this).apply {
            text = "Open video"
            isFocusable = true
            setOnClickListener { openVideo() }
        }
        val resume = Button(this).apply {
            text = "Playback controls"
            setOnClickListener {
                if (playing) togglePlayback()
                else Toast.makeText(this@MainActivity, "Open a video to start playback", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(title, matchWrap())
        root.addView(status, matchWrap())
        root.addView(open, LinearLayout.LayoutParams(320, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 28 })
        root.addView(resume, LinearLayout.LayoutParams(320, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 12 })
        setContentView(root)
        open.requestFocus()
    }

    private fun openVideo() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        startActivityForResult(intent, openVideoRequest)
    }

    @Deprecated("Legacy result API retained for broad Android TV compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == openVideoRequest && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startPlayback(uri)
            }
        }
    }

    private fun startPlayback(uri: Uri) {
        try {
            releasePlayer()
            val vlc = LibVLC(this, arrayListOf("--no-video-title-show", "--audio-time-stretch"))
            libVlc = vlc
            player = MediaPlayer(vlc)
            videoLayout = VLCVideoLayout(this)
            videoLayout.setBackgroundColor(0xFF000000.toInt())
            setContentView(videoLayout)
            player?.attachViews(videoLayout, null, false, false)
            val media = Media(vlc, uri)
            player?.media = media
            media.release()
            player?.play()
            playing = true
            status.text = "Playing: $uri"
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open video: ${e.message}", Toast.LENGTH_LONG).show()
            buildHome()
        }
    }

    private fun togglePlayback() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (player != null) {
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_DPAD_CENTER -> { togglePlayback(); return true }
                KeyEvent.KEYCODE_BACK -> { releasePlayer(); buildHome(); return true }
                KeyEvent.KEYCODE_DPAD_LEFT -> { player?.time = (player?.time ?: 0L) - 10_000L; return true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { player?.time = (player?.time ?: 0L) + 10_000L; return true }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun releasePlayer() {
        playing = false
        try { player?.stop(); player?.detachViews(); player?.release() } catch (_: Exception) {}
        try { libVlc?.release() } catch (_: Exception) {}
        player = null
        libVlc = null
    }

    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }
}
