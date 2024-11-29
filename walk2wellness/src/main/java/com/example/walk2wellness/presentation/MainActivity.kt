package com.example.walk2wellness.presentation

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.walk2wellness.R
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var sensorManager: SensorManager
    private var stepCount: Int = 0
    private lateinit var tvStepCount: TextView
    private lateinit var timeText: TextView
    private lateinit var btnStartSimulation: Button
    private lateinit var btnStopSimulation: Button

    private val simulatedStepInterval: Long = 1000
    private val handler = Handler(Looper.getMainLooper()) // Updated constructor
    private var isSimulatingSteps: Boolean = false
    private var simulationRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Views
        tvStepCount = findViewById(R.id.tvStepCount)
        timeText = findViewById(R.id.timeText) // Reference to time display TextView
        btnStartSimulation = findViewById(R.id.btnStartSimulation)
        btnStopSimulation = findViewById(R.id.btnStopSimulation)

        // Initialize SensorManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Register for step counter sensor or simulate steps directly
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor != null) {
            sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            Log.d("MainActivity", "Step sensor not found. Using simulated steps.")
        }

        // Start button click listener
        btnStartSimulation.setOnClickListener {
            startSimulation()
        }

        // Stop button click listener
        btnStopSimulation.setOnClickListener {
            stopSimulation()
        }

        // Start a background task to update time every second
        startUpdatingTime()
    }

    override fun onResume() {
        super.onResume()
        // Check for step sensor and re-register if necessary
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor != null) {
            sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(stepListener)
    }

    // Step count listener for real sensor data
    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                stepCount = event.values[0].toInt()
                tvStepCount.text = "Steps: $stepCount"
                sendStepCountToPhone(stepCount)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    // Start the simulation
    private fun startSimulation() {
        if (!isSimulatingSteps) {
            isSimulatingSteps = true
            simulationRunnable = object : Runnable {
                override fun run() {
                    if (isSimulatingSteps) {
                        stepCount++ // Increment step count
                        tvStepCount.text = "Steps: $stepCount" // Update UI
                        sendStepCountToPhone(stepCount)
                        handler.postDelayed(this, simulatedStepInterval) // Keep simulating
                    }
                }
            }
            handler.post(simulationRunnable!!) // Start simulation
            Log.d("MainActivity", "Started step simulation")
        }
    }

    // Stop the simulation
    private fun stopSimulation() {
        if (isSimulatingSteps) {
            isSimulatingSteps = false
            simulationRunnable?.let {
                handler.removeCallbacks(it) // Stop the simulation
                Log.d("MainActivity", "Stopped step simulation")
            }
        }
    }

    // Send step count to the mobile app (simulated step counts)
    private fun sendStepCountToPhone(steps: Int) {
        val dataMap = PutDataMapRequest.create("/step_count").apply {
            dataMap.putInt("steps", steps)
        }
        val putDataRequest = dataMap.asPutDataRequest()
        Wearable.getDataClient(this).putDataItem(putDataRequest)
            .addOnSuccessListener {
                Log.d("WatchApp", "Sent step count to phone: $steps")
            }
            .addOnFailureListener {
                Log.d("WatchApp", "Failed to send step count to phone")
            }
    }

    // Start updating the time every second
    private fun startUpdatingTime() {
        val timeRunnable = object : Runnable {
            override fun run() {
                // Get current time
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                timeText.text = currentTime // Update time TextView
                handler.postDelayed(this, 1000) // Update every second
            }
        }
        handler.post(timeRunnable) // Start time updates
    }
}

