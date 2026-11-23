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

        listOf(
            bauhausFile.absolutePath to 0.75f,
            prismFile.absolutePath to 1.0f
        )
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
