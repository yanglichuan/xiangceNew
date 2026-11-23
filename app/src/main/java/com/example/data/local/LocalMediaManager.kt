package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.PhotoEntity
import com.example.data.model.PhotoSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

class LocalMediaManager(private val context: Context) {

    private val mediaDir: File by lazy {
        File(context.filesDir, "gallery_media").apply { if (!exists()) mkdirs() }
    }

    private val shareDir: File by lazy {
        File(context.cacheDir, "shared_exports").apply { if (!exists()) mkdirs() }
    }

    fun resolveDrawableResId(drawableName: String?): Int {
        return when (drawableName) {
            "img_gallery_arch" -> R.drawable.img_gallery_arch
            "img_gallery_mist" -> R.drawable.img_gallery_mist
            "img_gallery_neon" -> R.drawable.img_gallery_neon
            "img_gallery_dunes" -> R.drawable.img_gallery_dunes
            else -> R.drawable.img_gallery_arch
        }
    }

    fun resolveCoilModel(photo: PhotoEntity): Any {
        return if (photo.sourceType == PhotoSourceType.CURATED_DRAWABLE) {
            resolveDrawableResId(photo.drawableName)
        } else {
            val file = photo.filePath?.let { File(it) }
            if (file != null && file.exists()) file else R.drawable.img_gallery_arch
        }
    }

    suspend fun decodeBitmap(photo: PhotoEntity, maxDimension: Int = 1080): Bitmap? =
        withContext(Dispatchers.IO) {
            if (photo.sourceType == PhotoSourceType.CURATED_DRAWABLE) {
                val resId = resolveDrawableResId(photo.drawableName)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeResource(context.resources, resId, bounds)
                val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeResource(context.resources, resId, opts)?.let {
                    scaleDownIfNeeded(it, maxDimension)
                }
            } else {
                val path = photo.filePath ?: return@withContext null
                val file = File(path)
                if (!file.exists()) return@withContext null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeFile(path, opts)?.let {
                    scaleDownIfNeeded(it, maxDimension)
                }
            }
        }

    suspend fun importUriToPrivateFile(uri: Uri): Pair<String, Float>? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val rawBytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
            val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, 1800)
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            var bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                ?: return@withContext null

            // Apply EXIF rotation if present
            val orientation = try {
                rawBytes.inputStream().use { stream ->
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                }
            } catch (_: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }
            bitmap = applyExifRotation(bitmap, orientation)
            bitmap = scaleDownIfNeeded(bitmap, 1800)

            val aspectRatio = (bitmap.width.toFloat() / max(1, bitmap.height)).coerceIn(0.45f, 2.2f)
            val outFile = File(mediaDir, "import_${UUID.randomUUID()}.jpg")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }
            bitmap.recycle()
            outFile.absolutePath to aspectRatio
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveExportedBitmap(bitmap: Bitmap): Pair<String, Float> = withContext(Dispatchers.IO) {
        val outFile = File(mediaDir, "atelier_fx_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        }
        val aspectRatio = (bitmap.width.toFloat() / max(1, bitmap.height)).coerceIn(0.45f, 2.2f)
        outFile.absolutePath to aspectRatio
    }

    suspend fun createShareUri(bitmap: Bitmap): Uri = withContext(Dispatchers.IO) {
        val outFile = File(shareDir, "atelier_share_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    }

    suspend fun deleteLocalFileIfOwned(filePath: String?) = withContext(Dispatchers.IO) {
        if (filePath != null) {
            val file = File(filePath)
            if (file.exists() && file.parentFile?.absolutePath == mediaDir.absolutePath) {
                file.delete()
            }
        }
    }

    suspend fun createProceduralExhibitionPrints(): List<Pair<String, Float>> = withContext(Dispatchers.IO) {
        val bauhausFile = File(mediaDir, "curated_bauhaus_vii.png")
        if (!bauhausFile.exists()) {
            val bmp = renderBauhausExhibitionPrint(900, 1200)
            FileOutputStream(bauhausFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
        }

        val prismFile = File(mediaDir, "curated_prism_horizon.png")
        if (!prismFile.exists()) {
            val bmp = renderPrismHorizonPrint(1080, 1080)
            FileOutputStream(prismFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
        }

        val cyberFile = File(mediaDir, "curated_cyber_grid.png")
        if (!cyberFile.exists()) {
            val bmp = renderCyberGridPrint(1080, 810)
            FileOutputStream(cyberFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
        }

        val cyanotypeFile = File(mediaDir, "curated_cyanotype.png")
        if (!cyanotypeFile.exists()) {
            val bmp = renderCyanotypeBotanicalPrint(900, 1200)
            FileOutputStream(cyanotypeFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
        }

        val silkFile = File(mediaDir, "curated_silk_wave.png")
        if (!silkFile.exists()) {
            val bmp = renderFluidSilkPrint(900, 1200)
            FileOutputStream(silkFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
        }

        val monolithFile = File(mediaDir, "curated_noir_monolith.png")
        if (!monolithFile.exists()) {
            val bmp = renderNoirMonolithPrint(1080, 1080)
            FileOutputStream(monolithFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
        }

        listOf(
            bauhausFile.absolutePath to 0.75f,
            prismFile.absolutePath to 1.0f,
            cyberFile.absolutePath to 1.33f,
            cyanotypeFile.absolutePath to 0.75f,
            silkFile.absolutePath to 0.75f,
            monolithFile.absolutePath to 1.0f
        )
    }

    private fun renderCyberGridPrint(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, h * 0.58f,
                intArrayOf(Color.parseColor("#0B0C1C"), Color.parseColor("#2B124C"), Color.parseColor("#B8296B")),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.58f, skyPaint)

        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, h * 0.14f, 0f, h * 0.58f,
                Color.parseColor("#FFE47A"), Color.parseColor("#FF4E78"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(w * 0.5f, h * 0.42f, h * 0.22f, sunPaint)

        val groundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0B0D18") }
        canvas.drawRect(0f, h * 0.58f, w.toFloat(), h.toFloat(), groundPaint)

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#47E5FF")
            strokeWidth = 2.5f
        }
        val horizonY = h * 0.58f
        for (i in -8..8) {
            canvas.drawLine(w * 0.5f + i * 28f, horizonY, w * 0.5f + i * 165f, h.toFloat(), gridPaint)
        }
        for (j in 0..8) {
            val t = (j / 8f)
            val y = horizonY + t * t * (h - horizonY)
            canvas.drawLine(0f, y, w.toFloat(), y, gridPaint)
        }
        addSubtlePaperGrain(bmp, 8)
        return bmp
    }

    private fun renderCyanotypeBotanicalPrint(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.5f, h * 0.48f, h * 0.72f,
                intArrayOf(Color.parseColor("#1D4E78"), Color.parseColor("#102B48")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(215, 240, 245, 250)
            style = Paint.Style.FILL
        }
        val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 245, 248, 252)
            strokeWidth = 6f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(w * 0.5f, h * 0.86f, w * 0.48f, h * 0.16f, stemPaint)
        for (i in 0..11) {
            val py = h * (0.78f - i * 0.052f)
            val spread = w * (0.24f - i * 0.013f)
            val dir = if (i % 2 == 0) 1f else -1f
            canvas.save()
            canvas.translate(w * 0.49f, py)
            canvas.rotate(dir * 34f)
            canvas.drawOval(0f, -22f, dir * spread, 22f, leafPaint)
            canvas.restore()
        }
        addSubtlePaperGrain(bmp, 12)
        return bmp
    }

    private fun renderFluidSilkPrint(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#171514"))

        val colors = intArrayOf(
            Color.parseColor("#D4AF37"),
            Color.parseColor("#C84B31"),
            Color.parseColor("#EAE2D3")
        )
        for (layer in 0 until 3) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colors[layer]
                alpha = 185 - layer * 25
            }
            val path = Path().apply {
                moveTo(0f, h * (0.28f + layer * 0.16f))
                cubicTo(
                    w * 0.32f, h * (0.12f + layer * 0.18f),
                    w * 0.68f, h * (0.58f - layer * 0.08f),
                    w.toFloat(), h * (0.34f + layer * 0.15f)
                )
                lineTo(w.toFloat(), h.toFloat())
                lineTo(0f, h.toFloat())
                close()
            }
            canvas.drawPath(path, paint)
        }
        addSubtlePaperGrain(bmp, 10)
        return bmp
    }

    private fun renderNoirMonolithPrint(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                Color.parseColor("#EFECE6"), Color.parseColor("#CFC9BE"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                w * 0.42f, h * 0.78f, w * 0.92f, h * 0.92f,
                Color.argb(130, 20, 20, 19), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        val shadowPath = Path().apply {
            moveTo(w * 0.36f, h * 0.78f)
            lineTo(w * 0.58f, h * 0.78f)
            lineTo(w * 0.92f, h * 0.96f)
            lineTo(w * 0.62f, h * 0.96f)
            close()
        }
        canvas.drawPath(shadowPath, shadowPaint)

        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                w * 0.34f, h * 0.16f, w * 0.60f, h * 0.78f,
                Color.parseColor("#2A2A28"), Color.parseColor("#111110"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(w * 0.34f, h * 0.16f, w * 0.58f, h * 0.78f, monoPaint)

        val goldLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")
            strokeWidth = 6f
        }
        canvas.drawLine(w * 0.34f, h * 0.16f, w * 0.34f, h * 0.78f, goldLine)
        addSubtlePaperGrain(bmp, 11)
        return bmp
    }

    private fun renderBauhausExhibitionPrint(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#F3EFE6"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Warm terracotta sun disc
        paint.color = Color.parseColor("#C84B31")
        canvas.drawCircle(w * 0.54f, h * 0.38f, w * 0.25f, paint)

        // Charcoal architectural arch
        paint.color = Color.parseColor("#181816")
        canvas.drawRect(w * 0.16f, h * 0.45f, w * 0.48f, h * 0.84f, paint)

        // Burnished brass triangle
        paint.color = Color.parseColor("#C79C46")
        val path = Path().apply {
            moveTo(w * 0.48f, h * 0.84f)
            lineTo(w * 0.84f, h * 0.84f)
            lineTo(w * 0.66f, h * 0.50f)
            close()
        }
        canvas.drawPath(path, paint)

        // Crisp museum horizon line
        paint.color = Color.parseColor("#181816")
        paint.strokeWidth = 5f
        canvas.drawLine(w * 0.12f, h * 0.84f, w * 0.88f, h * 0.84f, paint)
        addSubtlePaperGrain(bmp, 10)
        return bmp
    }

    private fun renderPrismHorizonPrint(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                intArrayOf(
                    Color.parseColor("#141926"),
                    Color.parseColor("#1F3B4D"),
                    Color.parseColor("#D48C46"),
                    Color.parseColor("#F1DFB6")
                ),
                floatArrayOf(0f, 0.42f, 0.78f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.5f, h * 0.46f, w * 0.36f,
                intArrayOf(Color.parseColor("#FFF4D4"), Color.parseColor("#44E89B4B"), Color.TRANSPARENT),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(w * 0.5f, h * 0.46f, w * 0.36f, orbPaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 255, 250, 240)
            strokeWidth = 3f
        }
        for (i in 0..8) {
            val y = h * (0.56f + i * 0.042f)
            canvas.drawLine(w * 0.08f, y, w * 0.92f, y, linePaint)
        }
        addSubtlePaperGrain(bmp, 12)
        return bmp
    }

    private fun addSubtlePaperGrain(bmp: Bitmap, strength: Int) {
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val rng = Random(42)
        for (i in pixels.indices) {
            val c = pixels[i]
            val delta = rng.nextInt(-strength, strength + 1)
            val r = (Color.red(c) + delta).coerceIn(0, 255)
            val g = (Color.green(c) + delta).coerceIn(0, 255)
            val b = (Color.blue(c) + delta).coerceIn(0, 255)
            pixels[i] = Color.rgb(r, g, b)
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        if (width > maxDim || height > maxDim) {
            val halfW = width / 2
            val halfH = height / 2
            while ((halfW / inSampleSize) >= maxDim && (halfH / inSampleSize) >= maxDim) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val largest = max(w, h)
        if (largest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / largest.toFloat()
        val targetW = max(1, (w * ratio).roundToInt())
        val targetH = max(1, (h * ratio).roundToInt())
        val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        if (scaled != bitmap) bitmap.recycle()
        return scaled
    }

    private fun applyExifRotation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }
}
