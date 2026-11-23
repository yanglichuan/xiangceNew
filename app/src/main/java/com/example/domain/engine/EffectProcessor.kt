package com.example.domain.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.data.model.ArtFilter
import com.example.data.model.EffectConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

object EffectProcessor {

    suspend fun processBitmap(
        source: Bitmap,
        config: EffectConfig
    ): Bitmap = withContext(Dispatchers.Default) {
        val w = source.width
        val h = source.height
        var working = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(working)

        val cm = buildColorMatrix(config)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        when (config.filter) {
            ArtFilter.PIXEL_8BIT -> working = applyPixelateEffect(working, config.filterIntensity)
            ArtFilter.OIL_IMPASTO -> working = applyOilPaintEffect(working, config.filterIntensity)
            ArtFilter.CHROMATIC_GLITCH -> working = applyChromaticGlitch(working, config.filterIntensity)
            ArtFilter.CYBER_NEON -> working = applyCyberDuotone(working, config.filterIntensity)
            ArtFilter.HALFTONE_POP -> working = applyHalftonePop(working, config.filterIntensity)
            ArtFilter.NEON_SKETCH -> working = applyNeonEdgeSketch(working, config.filterIntensity)
            ArtFilter.THERMAL_INFRARED -> working = applyThermalInfrared(working, config.filterIntensity)
            ArtFilter.KALEIDOSCOPE -> working = applyKaleidoscope(working, config.filterIntensity)
            ArtFilter.WATER_RIPPLE -> working = applyWaterRipple(working, config.filterIntensity)
            ArtFilter.PRISM_SHATTER -> working = applyPrismShatter(working, config.filterIntensity)
            else -> Unit
        }

        if (config.grainAmount > 0.02f) {
            applyFilmGrain(working, config.grainAmount)
        }

        if (config.vignetteAmount > 0.02f) {
            applyVignette(working, config.vignetteAmount)
        }

        working
    }

    private fun buildColorMatrix(config: EffectConfig): ColorMatrix {
        val result = ColorMatrix()
        val intensity = config.filterIntensity.coerceIn(0f, 1f)

        val filterMatrix = when (config.filter) {
            ArtFilter.KODAK_FILM -> ColorMatrix(
                floatArrayOf(
                    1.08f, 0.04f, -0.02f, 0f, 10f,
                    0.02f, 1.04f, 0.01f, 0f, 6f,
                    -0.03f, 0.02f, 0.94f, 0f, 4f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ArtFilter.NOIR_SILVER -> ColorMatrix().apply {
                setSaturation(0f)
                val c = 1.28f
                val t = (-0.5f * c + 0.5f) * 255f
                postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            c, 0f, 0f, 0f, t,
                            0f, c, 0f, 0f, t,
                            0f, 0f, c, 0f, t,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            ArtFilter.WARM_SEPIA -> ColorMatrix().apply {
                setSaturation(0.25f)
                postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            1.18f, 0.14f, 0.02f, 0f, 14f,
                            0.06f, 1.02f, -0.04f, 0f, 6f,
                            -0.08f, 0.02f, 0.78f, 0f, -8f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            ArtFilter.CYBER_NEON, ArtFilter.HALFTONE_POP -> ColorMatrix().apply {
                setSaturation(1.45f)
            }
            else -> ColorMatrix()
        }

        val blended = FloatArray(20)
        val identity = ColorMatrix().array
        val target = filterMatrix.array
        for (i in 0 until 20) {
            blended[i] = identity[i] + (target[i] - identity[i]) * intensity
        }
        result.set(blended)

        if (abs(config.warmth) > 0.01f) {
            val w = config.warmth * 26f
            result.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, w,
                        0f, 1f, 0f, 0f, w * 0.35f,
                        0f, 0f, 1f, 0f, -w,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        if (abs(config.contrast - 1f) > 0.01f) {
            val c = config.contrast
            val t = (-0.5f * c + 0.5f) * 255f
            result.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        c, 0f, 0f, 0f, t,
                        0f, c, 0f, 0f, t,
                        0f, 0f, c, 0f, t,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        return result
    }

    private fun applyPixelateEffect(src: Bitmap, intensity: Float): Bitmap {
        val w = src.width
        val h = src.height
        val blockSize = max(4, (max(w, h) / (110f - intensity * 72f)).toInt())
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        var y = 0
        while (y < h) {
            val blockH = min(blockSize, h - y)
            var x = 0
            while (x < w) {
                val blockW = min(blockSize, w - x)
                val sampleColor = pixels[(y + blockH / 2) * w + (x + blockW / 2)]
                val r = (Color.red(sampleColor) / 36) * 36
                val g = (Color.green(sampleColor) / 36) * 36
                val b = (Color.blue(sampleColor) / 36) * 36
                val quantized = Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))

                for (dy in 0 until blockH) {
                    val rowOffset = (y + dy) * w + x
                    for (dx in 0 until blockW) {
                        pixels[rowOffset + dx] = quantized
                    }
                }
                x += blockSize
            }
            y += blockSize
        }
        src.setPixels(pixels, 0, w, 0, 0, w, h)
        return src
    }

    private fun applyOilPaintEffect(src: Bitmap, intensity: Float): Bitmap {
        val w = src.width
        val h = src.height
        val radius = if (intensity > 0.6f) 3 else 2
        val levels = 12
        val inPixels = IntArray(w * h)
        val outPixels = IntArray(w * h)
        src.getPixels(inPixels, 0, w, 0, 0, w, h)

        val intensityCount = IntArray(levels)
        val sumR = IntArray(levels)
        val sumG = IntArray(levels)
        val sumB = IntArray(levels)

        for (y in 0 until h) {
            val yMin = max(0, y - radius)
            val yMax = min(h - 1, y + radius)
            for (x in 0 until w) {
                val xMin = max(0, x - radius)
                val xMax = min(w - 1, x + radius)

                intensityCount.fill(0)
                sumR.fill(0)
                sumG.fill(0)
                sumB.fill(0)

                for (ny in yMin..yMax) {
                    val row = ny * w
                    for (nx in xMin..xMax) {
                        val c = inPixels[row + nx]
                        val r = (c shr 16) and 0xFF
                        val g = (c shr 8) and 0xFF
                        val b = c and 0xFF
                        val lum = ((r + g + b) / 3 * (levels - 1)) / 255
                        intensityCount[lum]++
                        sumR[lum] += r
                        sumG[lum] += g
                        sumB[lum] += b
                    }
                }

                var bestBin = 0
                var maxCount = -1
                for (i in 0 until levels) {
                    if (intensityCount[i] > maxCount) {
                        maxCount = intensityCount[i]
                        bestBin = i
                    }
                }

                val r = (sumR[bestBin] / max(1, maxCount)).coerceIn(0, 255)
                val g = (sumG[bestBin] / max(1, maxCount)).coerceIn(0, 255)
                val b = (sumB[bestBin] / max(1, maxCount)).coerceIn(0, 255)
                outPixels[y * w + x] = Color.rgb(r, g, b)
            }
        }
        src.setPixels(outPixels, 0, w, 0, 0, w, h)
        return src
    }

    private fun applyChromaticGlitch(src: Bitmap, intensity: Float): Bitmap {
        val w = src.width
        val h = src.height
        val shift = max(4, (w * 0.024f * intensity).toInt())
        val inPixels = IntArray(w * h)
        val outPixels = IntArray(w * h)
        src.getPixels(inPixels, 0, w, 0, 0, w, h)

        for (y in 0 until h) {
            val row = y * w
            val bandShift = if ((y / 22) % 7 == 0) (shift * 1.4f).toInt() else 0
            for (x in 0 until w) {
                val rX = (x - shift + bandShift).coerceIn(0, w - 1)
                val gX = (x + bandShift / 2).coerceIn(0, w - 1)
                val bX = (x + shift - bandShift).coerceIn(0, w - 1)

                val r = (inPixels[row + rX] shr 16) and 0xFF
                val g = (inPixels[row + gX] shr 8) and 0xFF
                val b = inPixels[row + bX] and 0xFF
                val scanline = if (y % 4 == 0) 0.90f else 1.0f
                outPixels[row + x] = Color.rgb(
                    (r * scanline).toInt(),
                    (g * scanline).toInt(),
                    (b * scanline).toInt()
                )
            }
        }
        src.setPixels(outPixels, 0, w, 0, 0, w, h)
        return src
    }

    private fun applyCyberDuotone(src: Bitmap, intensity: Float): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

            val targetR = (22 + lum * 230).toInt().coerceIn(0, 255)
            val targetG = (18 + (1f - abs(lum - 0.5f) * 2f) * 205).toInt().coerceIn(0, 255)
            val targetB = (65 + (1f - lum) * 190 + lum * 90).toInt().coerceIn(0, 255)

            val nr = (r + (targetR - r) * intensity * 0.72f).toInt().coerceIn(0, 255)
            val ng = (g + (targetG - g) * intensity * 0.72f).toInt().coerceIn(0, 255)
            val nb = (b + (targetB - b) * intensity * 0.72f).toInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(nr, ng, nb)
        }
        src.setPixels(pixels, 0, w, 0, 0, w, h)
        return src
    }

    private fun applyHalftonePop(src: Bitmap, intensity: Float): Bitmap {
        val w = src.width
        val h = src.height
        val cell = max(8, (min(w, h) / (58f - intensity * 24f)).toInt())
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(Color.parseColor("#F5EFE2"))
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val sx = min(w - 1, x + cell / 2)
                val sy = min(h - 1, y + cell / 2)
                val col = pixels[sy * w + sx]
                val r = Color.red(col)
                val g = Color.green(col)
                val b = Color.blue(col)
                val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                val radius = ((1f - lum * 0.85f) * cell * 0.54f).coerceAtLeast(1.2f)

                // Vibrant Pop Art quantization
                val pr = if (r > 130) 225 else 24
                val pg = if (g > 130) 170 else 32
                val pb = if (b > 130) 215 else 54
                dotPaint.color = Color.rgb(
                    (r + (pr - r) * 0.55f).toInt().coerceIn(0, 255),
                    (g + (pg - g) * 0.55f).toInt().coerceIn(0, 255),
                    (b + (pb - b) * 0.55f).toInt().coerceIn(0, 255)
                )
                c.drawCircle(x + cell * 0.5f, y + cell * 0.5f, radius, dotPaint)
                x += cell
            }
            y += cell
        }
        src.recycle()
        return out
    }

    private fun applyNeonEdgeSketch(src: Bitmap, intensity: Float): Bitmap {
        val w = src.width
        val h = src.height
        val inPixels = IntArray(w * h)
        val outPixels = IntArray(w * h)
        src.getPixels(inPixels, 0, w, 0, 0, w, h)

        val lum = IntArray(w * h)
        for (i in inPixels.indices) {
            val c = inPixels[i]
            lum[i] = (((c shr 16) and 0xFF) * 30 + ((c shr 8) and 0xFF) * 59 + (c and 0xFF) * 11) / 100
        }

        val gain = 1.6f + intensity * 2.2f
        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val gx = -lum[(y - 1) * w + (x - 1)] + lum[(y - 1) * w + (x + 1)] -
                    2 * lum[row + (x - 1)] + 2 * lum[row + (x + 1)] -
                    lum[(y + 1) * w + (x - 1)] + lum[(y + 1) * w + (x + 1)]
                val gy = -lum[(y - 1) * w + (x - 1)] - 2 * lum[(y - 1) * w + x] - lum[(y - 1) * w + (x + 1)] +
                    lum[(y + 1) * w + (x - 1)] + 2 * lum[(y + 1) * w + x] + lum[(y + 1) * w + (x + 1)]

                val edge = min(255, ((abs(gx) + abs(gy)) * gain * 0.45f).toInt())
                val t = edge / 255f
                val bg = inPixels[row + x]
                val bgR = ((bg shr 16) and 0xFF) * 0.14f
                val bgG = ((bg shr 8) and 0xFF) * 0.16f
                val bgB = (bg and 0xFF) * 0.22f

                val edgeR = 60f + t * 195f
                val edgeG = 215f * t + (t * t * 40f)
                val edgeB = 255f * (1f - t * 0.35f) * t

                outPixels[row + x] = Color.rgb(
                    (bgR + edgeR * t).toInt().coerceIn(10, 255),
                    (bgG + edgeG * t).toInt().coerceIn(12, 255),
                    (bgB + edgeB).toInt().coerceIn(18, 255)
                )
            }
        }
        src.setPixels(outPixels, 0, w, 0, 0, w, h)
        return src
    }

    private fun applyThermalInfrared(src: Bitmap, intensity: Float): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val t = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

            val tr: Float
            val tg: Float
            val tb: Float
            when {
                t < 0.25f -> {
                    val k = t / 0.25f
                    tr = 18f + k * 70f
                    tg = 12f + k * 14f
                    tb = 75f + k * 125f
                }
                t < 0.55f -> {
                    val k = (t - 0.25f) / 0.30f
                    tr = 88f + k * 142f
                    tg = 26f + k * 34f
                    tb = 200f - k * 120f
                }
                t < 0.82f -> {
                    val k = (t - 0.55f) / 0.27f
                    tr = 230f + k * 25f
                    tg = 60f + k * 140f
                    tb = 80f - k * 60f
                }
                else -> {
                    val k = (t - 0.82f) / 0.18f
                    tr = 255f
                    tg = 200f + k * 55f
                    tb = 20f + k * 210f
                }
            }

            pixels[i] = Color.rgb(
                (r + (tr - r) * intensity).toInt().coerceIn(0, 255),
                (g + (tg - g) * intensity).toInt().coerceIn(0, 255),
                (b + (tb - b) * intensity).toInt().coerceIn(0, 255)
            )
        }
        src.setPixels(pixels, 0, w, 0, 0, w, h)
        return src
    }

    private fun applyKaleidoscope(src: Bitmap, intensity: Float): Bitmap {
        val w = src.width
        val h = src.height
        val inPixels = IntArray(w * h)
        val outPixels = IntArray(w * h)
        src.getPixels(inPixels, 0, w, 0, 0, w, h)

        val cx = w * 0.5f
        val cy = h * 0.5f
        val sector = (2.0 * PI) / 8.0
        val spinOffset = intensity * 0.6f

        for (y in 0 until h) {
            val dy = y - cy
            val row = y * w
            for (x in 0 until w) {
                val dx = x - cx
                val r = hypot(dx, dy)
                var theta = atan2(dy.toDouble(), dx.toDouble()) + spinOffset
                if (theta < 0) theta += 2.0 * PI
                var folded = theta % sector
                if (folded > sector / 2.0) folded = sector - folded

                val sx = (cx + r * cos(folded)).toInt().coerceIn(0, w - 1)
                val sy = (cy + r * sin(folded)).toInt().coerceIn(0, h - 1)
                outPixels[row + x] = inPixels[sy * w + sx]
            }
        }
        src.setPixels(outPixels, 0, w, 0, 0, w, h)
        return src
    }

    private fun applyWaterRipple(src: Bitmap, intensity: Float): Bitmap {
        val w = src.width
        val h = src.height
        val inPixels = IntArray(w * h)
        val outPixels = IntArray(w * h)
        src.getPixels(inPixels, 0, w, 0, 0, w, h)

        val cx = w * 0.5f
        val cy = h * 0.5f
        val amp = 6f + intensity * 18f
        val freq = 0.085f

        for (y in 0 until h) {
            val dy = y - cy
            val row = y * w
            for (x in 0 until w) {
                val dx = x - cx
                val dist = hypot(dx, dy)
                val wave = sin(dist * freq) * amp * (1f / (1f + dist * 0.003f))
                val sx = (x + wave * (dx / max(1f, dist))).toInt().coerceIn(0, w - 1)
                val sy = (y + wave * (dy / max(1f, dist))).toInt().coerceIn(0, h - 1)
                outPixels[row + x] = inPixels[sy * w + sx]
            }
        }
        src.setPixels(outPixels, 0, w, 0, 0, w, h)
        return src
    }

    private fun applyPrismShatter(src: Bitmap, intensity: Float): Bitmap {
        val w = src.width
        val h = src.height
        val inPixels = IntArray(w * h)
        val outPixels = IntArray(w * h)
        src.getPixels(inPixels, 0, w, 0, 0, w, h)

        val facetSize = max(24, (min(w, h) / 7))
        val shiftScale = (8f + intensity * 26f).toInt()

        for (y in 0 until h) {
            val row = y * w
            val ty = y / facetSize
            for (x in 0 until w) {
                val tx = x / facetSize
                val diag = ((x % facetSize) + (y % facetSize) > facetSize)
                val shardId = tx * 31 + ty * 17 + if (diag) 7 else 0
                val offX = ((shardId % 5) - 2) * shiftScale / 2
                val offY = (((shardId / 3) % 5) - 2) * shiftScale / 2

                val sxR = (x + offX + 4).coerceIn(0, w - 1)
                val sxG = (x + offX).coerceIn(0, w - 1)
                val sxB = (x + offX - 4).coerceIn(0, w - 1)
                val sy = (y + offY).coerceIn(0, h - 1)
                val sRow = sy * w

                val r = (inPixels[sRow + sxR] shr 16) and 0xFF
                val g = (inPixels[sRow + sxG] shr 8) and 0xFF
                val b = inPixels[sRow + sxB] and 0xFF
                outPixels[row + x] = Color.rgb(r, g, b)
            }
        }
        src.setPixels(outPixels, 0, w, 0, 0, w, h)
        return src
    }

    private fun applyFilmGrain(src: Bitmap, amount: Float) {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val strength = (amount * 34f).toInt().coerceAtLeast(1)
        val rng = Random(1337)
        for (i in pixels.indices) {
            val c = pixels[i]
            val n = rng.nextInt(-strength, strength + 1)
            val r = (((c shr 16) and 0xFF) + n).coerceIn(0, 255)
            val g = (((c shr 8) and 0xFF) + n).coerceIn(0, 255)
            val b = ((c and 0xFF) + n).coerceIn(0, 255)
            pixels[i] = Color.rgb(r, g, b)
        }
        src.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun applyVignette(src: Bitmap, amount: Float) {
        val canvas = Canvas(src)
        val cx = src.width / 2f
        val cy = src.height / 2f
        val radius = max(cx, cy) * 1.18f
        val alpha = (amount * 190f).toInt().coerceIn(0, 220)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(alpha, 10, 10, 12)),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, src.width.toFloat(), src.height.toFloat(), paint)
    }
}
