package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.sin

class SosService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_SOS

        if (action == ACTION_START_SOS) {
            val notification = createNotification()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            
            isServiceRunning = true

            // Start playing siren
            if (sirenPlayer == null) {
                sirenPlayer = SirenPlayer()
            }
            sirenPlayer?.start()

            // Start recording audio
            if (audioRecorder == null) {
                audioRecorder = SafeAudioRecorder(this)
            }
            currentAudioPath = audioRecorder?.startRecording()
        } else if (action == ACTION_START_SIREN) {
            if (sirenPlayer == null) {
                sirenPlayer = SirenPlayer()
            }
            sirenPlayer?.start()
        } else if (action == ACTION_STOP_SIREN) {
            sirenPlayer?.stop()
        } else if (action == ACTION_STOP_SOS) {
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        sirenPlayer?.stop()
        sirenPlayer = null

        audioRecorder?.stopRecording()
        audioRecorder = null

        isServiceRunning = false
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val channelId = "sos_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SOS Emergency Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Running SOS emergency protocols"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // Standard fallback or application launcher icon
        val iconRes = if (applicationInfo.icon != 0) applicationInfo.icon else android.R.drawable.ic_menu_compass

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Abhaya SOS Active")
            .setContentText("Recording audio and playing siren for your safety.")
            .setSmallIcon(iconRes)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 9119

        const val ACTION_START_SOS = "com.example.service.ACTION_START_SOS"
        const val ACTION_STOP_SOS = "com.example.service.ACTION_STOP_SOS"
        const val ACTION_START_SIREN = "com.example.service.ACTION_START_SIREN"
        const val ACTION_STOP_SIREN = "com.example.service.ACTION_STOP_SIREN"

        @Volatile
        var isServiceRunning = false
            private set

        @Volatile
        var currentAudioPath: String? = null
            private set

        private var sirenPlayer: SirenPlayer? = null
        private var audioRecorder: SafeAudioRecorder? = null
    }
}

class SafeAudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false

    fun startRecording(): String? {
        if (isRecording) return outputFile?.absolutePath
        try {
            val dir = context.getExternalFilesDir("EmergencyRecordings") ?: File(context.filesDir, "EmergencyRecordings")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            outputFile = File(dir, "emergency_record_${System.currentTimeMillis()}.mp4")
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            return outputFile?.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            // Gracefully fallback to files directory
            isRecording = true
            try {
                val dir = File(context.filesDir, "EmergencyRecordings")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                outputFile = File(dir, "emergency_record_fallback.mp4")
                outputFile?.createNewFile()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return outputFile?.absolutePath
        }
    }

    fun stopRecording() {
        if (!isRecording) return
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecording = false
        }
    }
}

class SirenPlayer {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    fun start() {
        if (isPlaying) return
        isPlaying = true
        thread(start = true) {
            val sampleRate = 8000
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = if (minBufferSize > 0) minBufferSize else 4000
            
            try {
                @Suppress("DEPRECATION")
                audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
                audioTrack?.play()
            } catch (e: Exception) {
                e.printStackTrace()
                return@thread
            }

            val samples = ShortArray(bufferSize)
            var angle = 0.0
            var time = 0.0

            while (isPlaying) {
                val track = audioTrack ?: break
                for (i in samples.indices) {
                    val lfo = sin(2.0 * Math.PI * 1.0 * time)
                    val frequency = 900.0 + lfo * 250.0
                    
                    samples[i] = (sin(angle) * Short.MAX_VALUE * 0.7).toInt().toShort()
                    angle += 2.0 * Math.PI * frequency / sampleRate
                    if (angle > 2.0 * Math.PI) {
                        angle -= 2.0 * Math.PI
                    }
                    time += 1.0 / sampleRate
                }
                try {
                    track.write(samples, 0, samples.size)
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    fun stop() {
        isPlaying = false
        try {
            audioTrack?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioTrack = null
        }
    }
}
