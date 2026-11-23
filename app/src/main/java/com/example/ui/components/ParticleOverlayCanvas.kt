package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.ParticleStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ParticleSeed(
    val x: Float,
    val y: Float,
    val radiusFactor: Float,
    val speed: Float,
    val phase: Float,
    val warm: Boolean
)

@Composable
fun ParticleOverlayCanvas(
    style: ParticleStyle,
    parallaxOffset: Offset = Offset.Zero,
    modifier: Modifier = Modifier
) {
    if (style == ParticleStyle.NONE) return

    val seeds = remember(style) {
        val rng = Random(style.ordinal * 77 + 19)
        List(26) { idx ->
            ParticleSeed(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                radiusFactor = 0.008f + rng.nextFloat() * 0.032f,
                speed = 0.6f + rng.nextFloat() * 1.4f,
                phase = rng.nextFloat() * (2f * PI.toFloat()),
                warm = idx % 2 == 0
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "particle_anim")
    val timePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time_phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)

        when (style) {
            ParticleStyle.DUST_BOKEH -> {
                seeds.forEach { p ->
                    val driftX = sin(timePhase * p.speed + p.phase) * w * 0.035f + parallaxOffset.x * 26f
                    val driftY = cos(timePhase * p.speed * 0.8f + p.phase) * h * 0.035f + parallaxOffset.y * 26f
                    val cx = ((p.x * w + driftX) + w) % w
                    val cy = ((p.y * h + driftY) + h) % h
                    val r = minDim * p.radiusFactor * 1.4f
                    val pulse = 0.45f + 0.45f * sin(timePhase * p.speed + p.phase)
                    val color = if (p.warm) Color(0xFFFFE19C) else Color(0xFFFFF5DB)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.42f * pulse), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = r
                        ),
                        radius = r,
                        center = Offset(cx, cy),
                        blendMode = BlendMode.Screen
                    )
                }
            }
            ParticleStyle.FILM_LEAK -> {
                val shift = sin(timePhase) * 0.16f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE95630).copy(alpha = 0.42f),
                            Color(0xFFF3AF48).copy(alpha = 0.24f),
                            Color.Transparent
                        ),
                        start = Offset(0f, h * (0.1f + shift)),
                        end = Offset(w * 0.65f, h * (0.55f - shift))
                    ),
                    blendMode = BlendMode.Screen
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF8844).copy(alpha = 0.30f), Color.Transparent),
                        center = Offset(w * (0.85f + shift * 0.3f), h * 0.78f),
                        radius = minDim * 0.6f
                    ),
                    blendMode = BlendMode.Screen
                )
            }
            ParticleStyle.NEON_SPARKS -> {
                seeds.forEach { p ->
                    val cx = (p.x * w + parallaxOffset.x * 32f + w) % w
                    val cy = (p.y * h + parallaxOffset.y * 32f + h) % h
                    val twinkle = (0.3f + 0.7f * ((sin(timePhase * p.speed * 2f + p.phase) + 1f) * 0.5f))
                    val arm = minDim * p.radiusFactor * 0.9f * twinkle
                    val sparkColor = if (p.warm) Color(0xFF7BF2FF) else Color(0xFFFFD875)
                    drawLine(
                        color = sparkColor.copy(alpha = 0.85f * twinkle),
                        start = Offset(cx - arm, cy),
                        end = Offset(cx + arm, cy),
                        strokeWidth = 2.5f
                    )
                    drawLine(
                        color = sparkColor.copy(alpha = 0.85f * twinkle),
                        start = Offset(cx, cy - arm),
                        end = Offset(cx, cy + arm),
                        strokeWidth = 2.5f
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f * twinkle),
                        radius = arm * 0.18f,
                        center = Offset(cx, cy)
                    )
                }
            }
            ParticleStyle.AURORA_PRISM -> {
                val sweep = (sin(timePhase) + 1f) * 0.5f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF70D6FF).copy(alpha = 0.24f),
                            Color(0xFFD4AF37).copy(alpha = 0.18f),
                            Color(0xFFFF70A6).copy(alpha = 0.24f)
                        ),
                        start = Offset(w * sweep, 0f),
                        end = Offset(w * (1f - sweep), h)
                    ),
                    blendMode = BlendMode.Screen
                )
            }
            ParticleStyle.NONE -> Unit
        }
    }
}
