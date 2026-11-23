package com.example.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.example.data.local.LocalMediaManager
import com.example.data.local.PhotoDao
import com.example.data.model.PhotoEntity
import com.example.data.model.PhotoSourceType
import kotlinx.coroutines.flow.Flow
import java.util.Locale

class PhotoRepository(
    private val photoDao: PhotoDao,
    val mediaManager: LocalMediaManager
) {
    val allPhotos: Flow<List<PhotoEntity>> = photoDao.getAllPhotos()

    fun observePhoto(id: Long): Flow<PhotoEntity?> = photoDao.observePhotoById(id)

    suspend fun getPhoto(id: Long): PhotoEntity? = photoDao.getPhotoById(id)

    suspend fun seedCuratedExhibitionIfEmpty() {
        if (photoDao.countPhotos() > 0) return

        val prints = mediaManager.createProceduralExhibitionPrints()
        val now = System.currentTimeMillis()
        val curated = listOf(
            PhotoEntity(
                title = "光影回廊 · Travertine",
                catalogCode = "NO. 01 · LEICA 35MM",
                exifSummary = "35mm · f/2.8 · 1/1000s · ISO 160",
                sourceType = PhotoSourceType.CURATED_DRAWABLE,
                drawableName = "img_gallery_arch",
                aspectRatio = 0.75f,
                isFavorite = true,
                createdAt = now - 600_000L
            ),
            PhotoEntity(
                title = "晨雾松林 · Dawn Mist",
                catalogCode = "NO. 02 · PORTRA 400",
                exifSummary = "50mm · f/2.0 · 1/500s · ISO 400",
                sourceType = PhotoSourceType.CURATED_DRAWABLE,
                drawableName = "img_gallery_mist",
                aspectRatio = 0.75f,
                isFavorite = true,
                createdAt = now - 500_000L
            ),
            PhotoEntity(
                title = "雨夜霓虹 · Shibuya Rain",
                catalogCode = "NO. 03 · CINESTILL 800T",
                exifSummary = "28mm · f/1.4 · 1/125s · ISO 800",
                sourceType = PhotoSourceType.CURATED_DRAWABLE,
                drawableName = "img_gallery_neon",
                aspectRatio = 1.33f,
                isFavorite = false,
                createdAt = now - 400_000L
            ),
            PhotoEntity(
                title = "金沙弧线 · Sahara Ridge",
                catalogCode = "NO. 04 · HASSELBLAD",
                exifSummary = "80mm · f/5.6 · 1/2000s · ISO 100",
                sourceType = PhotoSourceType.CURATED_DRAWABLE,
                drawableName = "img_gallery_dunes",
                aspectRatio = 1.0f,
                isFavorite = false,
                createdAt = now - 300_000L
            ),
            PhotoEntity(
                title = "包豪斯几何 · Study VII",
                catalogCode = "NO. 05 · ARCHIVAL PRINT",
                exifSummary = "EDITION 1/1 · MUSEUM LITHO",
                sourceType = PhotoSourceType.LOCAL_FILE,
                filePath = prints[0].first,
                aspectRatio = prints[0].second,
                isFavorite = false,
                createdAt = now - 200_000L
            ),
            PhotoEntity(
                title = "极光波纹 · Prism Horizon",
                catalogCode = "NO. 06 · EDITION 1/1",
                exifSummary = "SPECTRAL GRADIENT · 4K ARCHIVE",
                sourceType = PhotoSourceType.LOCAL_FILE,
                filePath = prints[1].first,
                aspectRatio = prints[1].second,
                isFavorite = true,
                createdAt = now - 100_000L
            )
        )
        photoDao.insertAll(curated)
    }

    suspend fun importPickedPhotos(uris: List<Uri>): Int {
        var importedCount = 0
        val currentTotal = photoDao.countPhotos()
        uris.forEachIndexed { idx, uri ->
            val result = mediaManager.importUriToPrivateFile(uri)
            if (result != null) {
                val (path, ratio) = result
                val num = String.format(Locale.US, "%02d", currentTotal + idx + 1)
                photoDao.insertPhoto(
                    PhotoEntity(
                        title = "私藏影像 #$num",
                        catalogCode = "IMPORT $num · LOCAL",
                        exifSummary = "手机相册导入 · 原画质留存",
                        sourceType = PhotoSourceType.LOCAL_FILE,
                        filePath = path,
                        aspectRatio = ratio,
                        isFavorite = false,
                        createdAt = System.currentTimeMillis() + idx
                    )
                )
                importedCount++
            }
        }
        return importedCount
    }

    suspend fun saveCreativeArtwork(
        sourcePhoto: PhotoEntity,
        renderedBitmap: Bitmap,
        effectTag: String
    ): Long {
        val (path, ratio) = mediaManager.saveExportedBitmap(renderedBitmap)
        val count = photoDao.countPhotos() + 1
        val code = String.format(Locale.US, "ATELIER %02d · %s", count, effectTag)
        val newTitle = if (sourcePhoto.title.contains("·")) {
            "${sourcePhoto.title.substringBefore("·").trim()} · 特效典藏"
        } else {
            "${sourcePhoto.title} · 特效版"
        }
        return photoDao.insertPhoto(
            PhotoEntity(
                title = newTitle,
                catalogCode = code,
                exifSummary = "特效工坊创作 · $effectTag",
                sourceType = PhotoSourceType.LOCAL_FILE,
                filePath = path,
                aspectRatio = ratio,
                isFavorite = true,
                createdAt = System.currentTimeMillis(),
                effectTag = effectTag
            )
        )
    }

    suspend fun toggleFavorite(photo: PhotoEntity) {
        photoDao.updatePhoto(photo.copy(isFavorite = !photo.isFavorite))
    }

    suspend fun deletePhoto(photo: PhotoEntity) {
        photoDao.deleteById(photo.id)
        mediaManager.deleteLocalFileIfOwned(photo.filePath)
    }
}
