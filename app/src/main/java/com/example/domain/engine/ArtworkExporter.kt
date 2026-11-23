package com.example.domain.engine

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.data.model.EffectConfig
import com.example.data.model.FrameStyle
import com.example.data.model.ParticleStyle
import com.example.data.model.PlacedSticker
import com.example.data.model.StickerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

object ArtworkExporter {

    suspend fun renderMasterpiece(
        sourceBitmap: Bitmap,
        config: EffectConfig,
        title: String
    ): Bitmap = withContext(Dispatchers.Default) {
        val filtered = EffectProcessor.processBitmap(sourceBitmap, config)
        val imgW = filtered.width
        val imgH = filtered.height

        // Apply static particle light/bokeh glow baked onto the photo surface
        bakeParticlesOnPhoto(filtered, config.particleStyle)

        // Draw stickers and luminous text onto the photo layer
        val photoCanvas = Canvas(filtered)
        config.stickers.forEach { sticker ->
            drawSticker(photoCanvas, sticker, imgW.toFloat(), imgH.toFloat())
        }
        if (config.luminousText.isNotBlank()) {
            drawLuminousText(
                canvas = photoCanvas,
                text = config.luminousText,
                w = imgW.toFloat(),
                h = imgH.toFloat(),
                normY = config.luminousTextNormY,
                glow = config.luminousTextGlow
            )
        }

        // Wrap in Museum Frame Matting if selected
        val framed = applyFrameMatting(filtered, config.frameStyle, title, config.filter.tagCode)
        if (framed != filtered) {
            filtered.recycle()
        }
        framed
    }

    private fun bakeParticlesOnPhoto(bitmap: Bitmap, style: ParticleStyle) {
        if (style == ParticleStyle.NONE) return
        val canvas = Canvas(bitmap)
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val rng = Random(2026)

        when (style) {
            ParticleStyle.DUST_BOKEH -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                repeat(24) {
                    val cx = rng.nextFloat() * w
                    val cy = rng.nextFloat() * h
                    val r = (min(w, h) * (0.02f + rng.nextFloat() * 0.06f))
                    paint.color = Color.argb(rng.nextInt(32, 95), 255, 224, 155)
                    canvas.drawCircle(cx, cy, r, paint)
                }
            }
            ParticleStyle.FILM_LEAK -> {
                val leakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, w * 0.55f, h * 0.45f,
                        intArrayOf(Color.argb(145, 235, 92, 48), Color.argb(70, 245, 178, 72), Color.TRANSPARENT),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, leakPaint)
            }
            ParticleStyle.NEON_SPARKS -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                repeat(20) {
                    val cx = rng.nextFloat() * w
                    val cy = rng.nextFloat() * h
                    val r = min(w, h) * (0.014f + rng.nextFloat() * 0.024f)
                    paint.color = if (it % 2 == 0) Color.argb(175, 120, 240, 255) else Color.argb(175, 255, 210, 110)
                    paint.strokeWidth = max(2f, r * 0.16f)
                    canvas.drawLine(cx - r, cy, cx + r, cy, paint)
                    canvas.drawLine(cx, cy - r, cx, cy + r, paint)
                }
            }
            ParticleStyle.AURORA_PRISM -> {
                val prismPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, h * 0.1f, w, h * 0.9f,
                        intArrayOf(
                            Color.argb(80, 125, 215, 255),
                            Color.argb(55, 212, 175, 55),
                            Color.argb(80, 225, 110, 175)
                        ),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, prismPaint)
            }
            ParticleStyle.NONE -> Unit
        }
    }

    private fun drawSticker(canvas: Canvas, sticker: PlacedSticker, w: Float, h: Float) {
        val cx = sticker.normX.coerceIn(0.08f, 0.92f) * w
        val cy = sticker.normY.coerceIn(0.08f, 0.92f) * h
        val baseSize = min(w, h) * 0.14f * sticker.scale

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(sticker.rotationDeg)

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(85, 0, 0, 0)
        }

        when (sticker.type) {
            StickerType.SEAL_EXHIBITION -> {
                canvas.drawCircle(4f, 6f, baseSize * 0.56f, shadowPaint)
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#C84B31")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(0f, 0f, baseSize * 0.55f, ringPaint)
                val innerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#F8F6F1")
                    style = Paint.Style.STROKE
                    strokeWidth = baseSize * 0.05f
                }
                canvas.drawCircle(0f, 0f, baseSize * 0.43f, innerStroke)
                val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#F8F6F1")
                    textSize = baseSize * 0.22f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                }
                canvas.drawText("馆藏", 0f, baseSize * 0.08f, txtPaint)
            }
            StickerType.PRISM_STAR -> {
                val starPath = createStarPath(4, baseSize * 0.62f, baseSize * 0.22f)
                canvas.save()
                canvas.translate(4f, 6f)
                canvas.drawPath(starPath, shadowPaint)
                canvas.restore()
                val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        -baseSize * 0.5f, -baseSize * 0.5f, baseSize * 0.5f, baseSize * 0.5f,
                        Color.parseColor("#FFF2B2"), Color.parseColor("#D4AF37"),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawPath(starPath, starPaint)
            }
            StickerType.FILM_STAMP -> {
                val rect = RectF(-baseSize * 0.85f, -baseSize * 0.34f, baseSize * 0.85f, baseSize * 0.34f)
                canvas.drawRoundRect(
                    RectF(rect.left + 4f, rect.top + 6f, rect.right + 4f, rect.bottom + 6f),
                    baseSize * 0.14f, baseSize * 0.14f, shadowPaint
                )
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#141413")
                }
                canvas.drawRoundRect(rect, baseSize * 0.14f, baseSize * 0.14f, bgPaint)
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#D4AF37")
                    style = Paint.Style.STROKE
                    strokeWidth = baseSize * 0.04f
                }
                canvas.drawRoundRect(rect, baseSize * 0.14f, baseSize * 0.14f, borderPaint)
                val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#F8F6F1")
                    textSize = baseSize * 0.24f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                }
                canvas.drawText("35MM FILM", 0f, baseSize * 0.09f, txtPaint)
            }
            StickerType.HEART_3D -> {
                val heart = createHeartPath(baseSize * 0.58f)
                canvas.save()
                canvas.translate(4f, 6f)
                canvas.drawPath(heart, shadowPaint)
                canvas.restore()
                val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, -baseSize * 0.5f, 0f, baseSize * 0.5f,
                        Color.parseColor("#FF7A6B"), Color.parseColor("#B8292F"),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawPath(heart, heartPaint)
            }
            StickerType.CROWN_GOLD -> {
                val crown = Path().apply {
                    val r = baseSize * 0.55f
                    moveTo(-r, r * 0.5f)
                    lineTo(-r, -r * 0.4f)
                    lineTo(-r * 0.4f, r * 0.05f)
                    lineTo(0f, -r * 0.65f)
                    lineTo(r * 0.4f, r * 0.05f)
                    lineTo(r, -r * 0.4f)
                    lineTo(r, r * 0.5f)
                    close()
                }
                canvas.save()
                canvas.translate(4f, 6f)
                canvas.drawPath(crown, shadowPaint)
                canvas.restore()
                val crownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        -baseSize * 0.5f, -baseSize * 0.5f, baseSize * 0.5f, baseSize * 0.5f,
                        Color.parseColor("#FCE38A"), Color.parseColor("#B68D40"),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawPath(crown, crownPaint)
            }
            StickerType.FRAME_CORNER -> {
                val rect = RectF(-baseSize * 0.9f, -baseSize * 0.34f, baseSize * 0.9f, baseSize * 0.34f)
                val brassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#D4AF37")
                }
                canvas.drawRoundRect(rect, baseSize * 0.08f, baseSize * 0.08f, brassPaint)
                val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#141413")
                    textSize = baseSize * 0.23f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                }
                canvas.drawText("ATELIER", 0f, baseSize * 0.08f, txtPaint)
            }
        }
        canvas.restore()
    }

    private fun drawLuminousText(
        canvas: Canvas,
        text: String,
        w: Float,
        h: Float,
        normY: Float,
        glow: Boolean
    ) {
        val textSize = min(w, h) * 0.068f
        val yPos = normY.coerceIn(0.15f, 0.90f) * h

        if (glow) {
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(190, 212, 175, 55)
                this.textSize = textSize
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                maskFilter = BlurMaskFilter(max(8f, textSize * 0.32f), BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawText(text, w / 2f, yPos, glowPaint)
        }

        val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFDF7")
            this.textSize = textSize
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            setShadowLayer(6f, 0f, 3f, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText(text, w / 2f, yPos, mainPaint)
    }

    private fun applyFrameMatting(
        photo: Bitmap,
        frameStyle: FrameStyle,
        title: String,
        filterCode: String
    ): Bitmap {
        if (frameStyle == FrameStyle.NONE) return photo
        val pw = photo.width
        val ph = photo.height
        val unit = min(pw, ph)

        return when (frameStyle) {
            FrameStyle.MUSEUM_MATTE -> {
                val pad = (unit * 0.12f).toInt()
                val bottomExtra = (unit * 0.16f).toInt()
                val out = Bitmap.createBitmap(pw + pad * 2, ph + pad + bottomExtra, Bitmap.Config.ARGB_8888)
                val c = Canvas(out)
                c.drawColor(Color.parseColor("#F8F6F1"))
                // Subtle hairline inner mat
                val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#CFC8BA")
                    style = Paint.Style.STROKE
                    strokeWidth = max(2f, unit * 0.004f)
                }
                c.drawBitmap(photo, pad.toFloat(), pad.toFloat(), null)
                c.drawRect(pad.toFloat(), pad.toFloat(), (pad + pw).toFloat(), (pad + ph).toFloat(), border)
                val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#141413")
                    textSize = unit * 0.036f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                }
                c.drawText("$title · $filterCode", out.width / 2f, pad + ph + bottomExtra * 0.6f, captionPaint)
                out
            }
            FrameStyle.POLAROID -> {
                val pad = (unit * 0.07f).toInt()
                val bottomExtra = (unit * 0.24f).toInt()
                val out = Bitmap.createBitmap(pw + pad * 2, ph + pad + bottomExtra, Bitmap.Config.ARGB_8888)
                val c = Canvas(out)
                c.drawColor(Color.parseColor("#FCFBF8"))
                c.drawBitmap(photo, pad.toFloat(), pad.toFloat(), null)
                val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#2A2927")
                    textSize = unit * 0.045f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                }
                c.drawText(title, out.width / 2f, pad + ph + bottomExtra * 0.58f, captionPaint)
                out
            }
            FrameStyle.FILM_STRIP_35MM -> {
                val padX = (unit * 0.04f).toInt()
                val padY = (unit * 0.14f).toInt()
                val out = Bitmap.createBitmap(pw + padX * 2, ph + padY * 2, Bitmap.Config.ARGB_8888)
                val c = Canvas(out)
                c.drawColor(Color.parseColor("#121213"))
                c.drawBitmap(photo, padX.toFloat(), padY.toFloat(), null)
                val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#F2EFE9")
                }
                val holeW = unit * 0.045f
                val holeH = padY * 0.42f
                val count = max(6, pw / (unit * 0.09f).toInt())
                val step = out.width.toFloat() / count
                for (i in 0 until count) {
                    val hx = i * step + step * 0.25f
                    c.drawRoundRect(hx, padY * 0.22f, hx + holeW, padY * 0.22f + holeH, 6f, 6f, holePaint)
                    c.drawRoundRect(
                        hx, out.height - padY * 0.22f - holeH,
                        hx + holeW, out.height - padY * 0.22f, 6f, 6f, holePaint
                    )
                }
                val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#D4AF37")
                    textSize = padY * 0.22f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                }
                c.drawText("ATELIER 400 · $filterCode", padX.toFloat(), out.height - padY * 0.72f, markPaint)
                out
            }
            FrameStyle.GALLERY_BRASS -> {
                val pad = (unit * 0.09f).toInt()
                val out = Bitmap.createBitmap(pw + pad * 2, ph + pad * 2, Bitmap.Config.ARGB_8888)
                val c = Canvas(out)
                c.drawColor(Color.parseColor("#161513"))
                val goldOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#D4AF37")
                    style = Paint.Style.STROKE
                    strokeWidth = max(4f, unit * 0.012f)
                }
                c.drawRect(pad * 0.35f, pad * 0.35f, out.width - pad * 0.35f, out.height - pad * 0.35f, goldOuter)
                c.drawBitmap(photo, pad.toFloat(), pad.toFloat(), null)
                val goldInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#B68D40")
                    style = Paint.Style.STROKE
                    strokeWidth = max(2f, unit * 0.005f)
                }
                c.drawRect(pad.toFloat(), pad.toFloat(), (pad + pw).toFloat(), (pad + ph).toFloat(), goldInner)
                out
            }
            FrameStyle.NONE -> photo
        }
    }

    private fun createStarPath(points: Int, outerR: Float, innerR: Float): Path {
        val path = Path()
        val total = points * 2
        for (i in 0 until total) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = Math.toRadians((i * 360.0 / total) - 90.0)
            val x = (cos(angle) * r).toFloat()
            val y = (sin(angle) * r).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private fun createHeartPath(size: Float): Path {
        return Path().apply {
            moveTo(0f, size * 0.7f)
            cubicTo(-size * 1.2f, -size * 0.05f, -size * 0.55f, -size * 0.85f, 0f, -size * 0.22f)
            cubicTo(size * 0.55f, -size * 0.85f, size * 1.2f, -size * 0.05f, 0f, size * 0.7f)
            close()
        }
    }
}
