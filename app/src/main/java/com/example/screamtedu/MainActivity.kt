package com.example.screamtedu

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.log10

class MainActivity : AppCompatActivity() {

    private var mediaRecorder: MediaRecorder? = null
    private lateinit var tvProgress: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnStart: Button

    private var soundLevel = 0
    private var timer: CountDownTimer? = null
    private val REQUEST_AUDIO_PERMISSION = 100
    private var isMonitoring = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi views
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)
        btnStart = findViewById(R.id.btnStart)

        btnStart.setOnClickListener {
            if (hasAudioPermission()) {
                startGame()
            } else {
                requestPermission()
            }
        }

        requestPermission()
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        if (!hasAudioPermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO_PERMISSION)
        }
    }

    private fun startGame() {
        try {
            startRecording()
            startMonitoring()

            timer = object : CountDownTimer(10000, 1000) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    stopRecording()
                    stopMonitoring()
                    // Hitung skor berdasarkan soundLevel dan durasi
                    val score = calculateScore(soundLevel)
                    tvProgress.text = "Skor: $score"
                }
            }.start()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting game: ${e.message}")
        }
    }

    private fun startRecording() {
        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(createTempFile("temp_audio", ".3gp").absolutePath)
                prepare()
                start()
            }
            Log.d("MainActivity", "Recording started")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting recording: ${e.message}")
        }
    }

    private fun startMonitoring() {
        isMonitoring = true
        mediaRecorder?.let { recorder ->
            Thread {
                while (isMonitoring) {
                    try {
                        val maxAmplitude = recorder.maxAmplitude
                        if (maxAmplitude > 0) {
                            val db = 20 * log10(maxAmplitude.toDouble() / Short.MAX_VALUE)
                            soundLevel = (db + 96).toInt()
                            runOnUiThread {
                                progressBar.progress = soundLevel
                                tvProgress.text = "$soundLevel%"
                            }
                        }
                        Thread.sleep(100)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error monitoring sound level: ${e.message}")
                    }
                }
            }.start()
        }
    }

    private fun stopMonitoring() {
        isMonitoring = false
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            Log.d("MainActivity", "Recording stopped")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error stopping recording: ${e.message}")
        }
    }

    private fun calculateScore(soundLevel: Int): Int {
        return soundLevel * 10
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Audio permission granted")
            } else {
                Log.e("MainActivity", "Audio permission denied")
            }
        }
    }
}
