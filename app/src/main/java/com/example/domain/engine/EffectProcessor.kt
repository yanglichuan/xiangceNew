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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
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

        // 1. Base ColorMatrix adjustments (filter + warmth + contrast)
        val cm = buildColorMatrix(config)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        // 2. Pixel-level artistic shaders
        when (config.filter) {
            ArtFilter.PIXEL_8BIT -> {
                working = applyPixelateEffect(working, config.filterIntensity)
            }
            ArtFilter.OIL_IMPASTO -> {
                working = applyOilPaintEffect(working, config.filterIntensity)
            }
            ArtFilter.CHROMATIC_GLITCH -> {
                working = applyChromaticGlitch(working, config.filterIntensity)
            }
            ArtFilter.CYBER_NEON -> {
                working = applyCyberDuotone(working, config.filterIntensity)
            }
            else -> Unit
        }

        // 3. Film Grain
        if (config.grainAmount > 0.02f) {
            applyFilmGrain(working, config.grainAmount)
        }

        // 4. Museum Vignette
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
            ArtFilter.CYBER_NEON -> ColorMatrix().apply {
                setSaturation(1.45f)
            }
            else -> ColorMatrix()
        }

        // Blend identity -> filterMatrix by intensity
        val blended = FloatArray(20)
        val identity = ColorMatrix().array
        val target = filterMatrix.array
        for (i in 0 until 20) {
            blended[i] = identity[i] + (target[i] - identity[i]) * intensity
        }
        result.set(blended)

        // Warmth adjustment (-1..1)
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

        // Contrast adjustment (0.6..1.6)
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
                // Quantize color channels for crisp 8-bit retro look
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

        val step = 1
        for (y in 0 until h step step) {
            val yMin = max(0, y - radius)
            val yMax = min(h - 1, y + radius)
            for (x in 0 until w step step) {
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
        val shift = max(4, (w * 0.022f * intensity).toInt())
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

            // Shadows -> Deep Electric Cyan/Indigo, Highlights -> Neon Magenta/Gold
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
