package com.ff6locke.gplayer

import `is`.xyz.mpv.BaseMPVView
import `is`.xyz.mpv.MPVLib
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

private class GPlayerMpvView(context: android.content.Context) :
    BaseMPVView(context, null) {
    override fun initOptions() {
        MPVLib.setOptionString("profile", "fast")
        MPVLib.setOptionString("vo", "gpu")
        MPVLib.setOptionString("gpu-context", "android")
        MPVLib.setOptionString("opengl-es", "yes")
        MPVLib.setOptionString("hwdec", "auto-safe")
        MPVLib.setOptionString("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1")
        MPVLib.setOptionString("ao", "audiotrack,opensles")
        MPVLib.setOptionString("audio-exclusive", "no")
        MPVLib.setOptionString("input-default-bindings", "yes")
        MPVLib.setOptionString("osc", "no")
        MPVLib.setOptionString("terminal", "no")
        MPVLib.setOptionString("config", "no")
        MPVLib.setOptionString("cache", "yes")
        MPVLib.setOptionString("demuxer-max-bytes", "67108864")
        MPVLib.setOptionString("demuxer-max-back-bytes", "67108864")
        MPVLib.setOptionString("sub-auto", "fuzzy")
        MPVLib.setOptionString("audio-file-auto", "fuzzy")
    }

    override fun postInitOptions() {
        MPVLib.setOptionString("save-position-on-quit", "no")
    }

    override fun observeProperties() {
        MPVLib.observeProperty("time-pos", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
        MPVLib.observeProperty("duration", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
        MPVLib.observeProperty("pause", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
        MPVLib.observeProperty("track-list", MPVLib.MpvFormat.MPV_FORMAT_NONE)
    }
}

class MainActivity : Activity() {
    private var playerView: GPlayerMpvView? = null
    private var initialized = false
    private var isCopyingVideo = false
    private val openVideoRequest = 4101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        buildHome()
    }

    private fun buildHome() {
        val root = LinearLayout(this).apply {
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
        val status = TextView(this).apply {
            text = "mpv playback engine • 32-bit ARM"
            textSize = 16f
            setTextColor(0xFFB8C0CC.toInt())
            gravity = Gravity.CENTER
        }
        val open = Button(this).apply {
            text = "Open video"
            isFocusable = true
            setOnClickListener { openVideo() }
        }
        root.addView(title, matchWrap())
        root.addView(status, matchWrap())
        root.addView(open, LinearLayout.LayoutParams(320, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 28 })
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

    @Deprecated("Legacy result API retained for Android TV compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == openVideoRequest && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) {}
                copyAndStartPlayback(uri)
            }
        }
    }

    private fun copyAndStartPlayback(uri: Uri) {
        if (isCopyingVideo) return
        isCopyingVideo = true
        Toast.makeText(this, "Preparing video…", Toast.LENGTH_SHORT).show()

        Thread {
            var copiedFile: File? = null
            try {
                val displayName = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    } ?: "selected-video"
                val safeName = displayName.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_")
                val extension = safeName.substringAfterLast('.', "")
                val target = File(cacheDir, "playback_input" + if (extension.isNotBlank()) ".$extension" else ".video")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Cannot read selected video")
                if (!target.exists() || target.length() == 0L) throw IllegalStateException("Selected video is empty")
                copiedFile = target
                runOnUiThread {
                    isCopyingVideo = false
                    startPlayback(target.absolutePath)
                }
            } catch (e: Exception) {
                copiedFile?.delete()
                runOnUiThread {
                    isCopyingVideo = false
                    Toast.makeText(this, "Cannot open video: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun startPlayback(filePath: String) {
        try {
            stopPlayback()
            val view = GPlayerMpvView(this).apply {
                setBackgroundColor(0xFF000000.toInt())
                keepScreenOn = true
            }
            playerView = view
            view.playFile(filePath)
            view.initialize(filesDir.absolutePath, cacheDir.absolutePath)
            initialized = true
            setContentView(view)
            view.requestFocus()
        } catch (e: Exception) {
            stopPlayback()
            Toast.makeText(this, "mpv could not start: ${e.message}", Toast.LENGTH_LONG).show()
            buildHome()
        }
    }

    private fun stopPlayback() {
        val view = playerView
        playerView = null
        if (initialized && view != null) {
            try { view.destroy() } catch (_: Exception) {}
        }
        initialized = false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (playerView != null) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> { stopPlayback(); buildHome(); return true }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_DPAD_CENTER -> {
                    MPVLib.command(arrayOf("cycle", "pause")); return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    MPVLib.command(arrayOf("seek", "-10", "relative")); return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    MPVLib.command(arrayOf("seek", "10", "relative")); return true
                }
                KeyEvent.KEYCODE_MEDIA_REWIND -> {
                    MPVLib.command(arrayOf("seek", "-30", "relative")); return true
                }
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                    MPVLib.command(arrayOf("seek", "30", "relative")); return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }
}
