package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext

/**
 * Provides smoothed normalized device tilt (-1f..1f on X and Y) from the accelerometer
 * for naked-eye 3D parallax and physics gravity bias. Falls back cleanly to Offset.Zero
 * when hardware sensor is stationary (e.g. emulator).
 */
@Composable
fun rememberTiltOffset(): State<Offset> {
    val context = LocalContext.current
    val tiltState = remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensorManager == null || accel == null) {
            onDispose { }
        } else {
            var smoothX = 0f
            var smoothY = 0f
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    // Normalize tilt relative to natural ~45deg handheld angle
                    val rawX = (-event.values[0] / 6.5f).coerceIn(-1f, 1f)
                    val rawY = ((event.values[1] - 4.5f) / 6.5f).coerceIn(-1f, 1f)
                    smoothX += (rawX - smoothX) * 0.14f
                    smoothY += (rawY - smoothY) * 0.14f
                    tiltState.value = Offset(smoothX, smoothY)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }
    }
    return tiltState
}
