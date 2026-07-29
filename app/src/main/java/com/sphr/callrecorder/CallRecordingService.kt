package com.sphr.callrecorder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CallRecordingService : Service() {

    companion object {
        const val ACTION_START = "com.sphr.callrecorder.action.START"
        const val ACTION_STOP = "com.sphr.callrecorder.action.STOP"
        const val EXTRA_NUMBER = "extra_number"
        private const val CHANNEL_ID = "call_recording_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private var recorder: MediaRecorder? = null
    private var audioManager: AudioManager? = null
    private var usingFallback = false
    private var outputFile: File? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val number = intent.getStringExtra(EXTRA_NUMBER) ?: "unknown"
                startForeground(NOTIFICATION_ID, buildNotification())
                startRecording(number)
            }
            ACTION_STOP -> {
                stopRecording()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording(number: String) {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val dir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "CallRecordings")
        if (!dir.exists()) dir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeNumber = number.replace(Regex("[^0-9+]"), "")
        outputFile = File(dir, "${safeNumber}_$timestamp.m4a")

        if (!tryStart(MediaRecorder.AudioSource.VOICE_COMMUNICATION)) {
            usingFallback = true
            audioManager?.apply {
                mode = AudioManager.MODE_IN_COMMUNICATION
                isSpeakerphoneOn = true
            }
            tryStart(MediaRecorder.AudioSource.MIC)
        }
    }

    private fun tryStart(source: Int): Boolean {
        return try {
            recorder = MediaRecorder().apply {
                setAudioSource(source)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            false
        }
    }

    private fun stopRecording() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
        } finally {
            recorder?.release()
            recorder = null
        }

        if (usingFallback) {
            audioManager?.apply {
                isSpeakerphoneOn = false
                mode = AudioManager.MODE_NORMAL
            }
            usingFallback = false
        }

        outputFile?.let {
            if (it.exists() && it.length() < 1024) it.delete()
        }
    }

    private fun buildNotification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Call Recording", NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NotificationManager::class.java)).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("در حال ضبط تماس")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
