package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AtelierContainer
import com.example.data.model.ArtFilter
import com.example.data.model.CURATED_FX_PRESETS
import com.example.data.model.EffectConfig
import com.example.data.model.FrameStyle
import com.example.data.model.FxPreset
import com.example.data.model.ParticleStyle
import com.example.data.model.PhotoEntity
import com.example.data.model.PlacedSticker
import com.example.data.model.StickerType
import com.example.domain.engine.ArtworkExporter
import com.example.domain.engine.EffectProcessor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class StudioSaveNotice(
    val savedPhotoId: Long,
    val message: String
)

data class StudioUiState(
    val photo: PhotoEntity? = null,
    val config: EffectConfig = EffectConfig(),
    val previewBitmap: Bitmap? = null,
    val isRenderingPreview: Boolean = false,
    val isSaving: Boolean = false,
    val savedCount: Int = 0,
    val lastSavedPhotoId: Long? = null,
    val batchProgressText: String? = null,
    val activeTab: Int = 0
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AtelierContainer.provideRepository(application)
    private val mediaManager = repository.mediaManager

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private val _savedNoticeEvent = MutableSharedFlow<StudioSaveNotice>(extraBufferCapacity = 4)
    val savedNoticeEvent = _savedNoticeEvent.asSharedFlow()

    private val _shareUriEvent = MutableSharedFlow<Uri>(extraBufferCapacity = 2)
    val shareUriEvent = _shareUriEvent.asSharedFlow()

    private val _errorEvent = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val errorEvent = _errorEvent.asSharedFlow()

    private var basePreviewBitmap: Bitmap? = null
    private var renderJob: Job? = null
    private var loadedPhotoId: Long = -1L

    fun loadPhoto(photoId: Long) {
        if (loadedPhotoId == photoId && basePreviewBitmap != null) return
        loadedPhotoId = photoId
        viewModelScope.launch {
            _uiState.update { it.copy(isRenderingPreview = true) }
            val photo = repository.getPhoto(photoId)
            if (photo == null) {
                _uiState.update { it.copy(isRenderingPreview = false) }
                _errorEvent.emit("未能加载该作品")
                return@launch
            }
            val decoded = mediaManager.decodeBitmap(photo, maxDimension = 760)
            basePreviewBitmap = decoded
            _uiState.update {
                it.copy(
                    photo = photo,
                    previewBitmap = decoded,
                    isRenderingPreview = false
                )
            }
        }
    }

    fun setActiveTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun applyPreset(preset: FxPreset) {
        _uiState.update { it.copy(config = preset.config) }
        schedulePreviewRender()
    }

    fun selectFilter(filter: ArtFilter) {
        updateConfig { it.copy(filter = filter) }
    }

    fun setFilterIntensity(value: Float) {
        updateConfig { it.copy(filterIntensity = value.coerceIn(0f, 1f)) }
    }

    fun setParallaxDepth(value: Float) {
        _uiState.update { it.copy(config = it.config.copy(parallaxDepth = value.coerceIn(0f, 1f))) }
    }

    fun toggleHologramFoil() {
        _uiState.update { it.copy(config = it.config.copy(hologramFoil = !it.config.hologramFoil)) }
    }

    fun selectParticle(style: ParticleStyle) {
        _uiState.update { it.copy(config = it.config.copy(particleStyle = style)) }
    }

    fun selectFrame(frame: FrameStyle) {
        _uiState.update { it.copy(config = it.config.copy(frameStyle = frame)) }
    }

    fun setGrain(value: Float) {
        updateConfig { it.copy(grainAmount = value.coerceIn(0f, 1f)) }
    }

    fun setVignette(value: Float) {
        updateConfig { it.copy(vignetteAmount = value.coerceIn(0f, 1f)) }
    }

    fun setWarmth(value: Float) {
        updateConfig { it.copy(warmth = value.coerceIn(-1f, 1f)) }
    }

    fun setContrast(value: Float) {
        updateConfig { it.copy(contrast = value.coerceIn(0.65f, 1.55f)) }
    }

    fun setLuminousText(text: String) {
        _uiState.update { it.copy(config = it.config.copy(luminousText = text)) }
    }

    fun toggleLuminousGlow() {
        _uiState.update {
            it.copy(config = it.config.copy(luminousTextGlow = !it.config.luminousTextGlow))
        }
    }

    fun addSticker(type: StickerType) {
        val count = _uiState.value.config.stickers.size
        val offset = ((count % 4) - 1.5f) * 0.08f
        val sticker = PlacedSticker(
            id = UUID.randomUUID().toString(),
            type = type,
            normX = (0.5f + offset).coerceIn(0.2f, 0.8f),
            normY = (0.42f + offset).coerceIn(0.2f, 0.8f),
            scale = 1f,
            rotationDeg = if (count % 2 == 0) -6f else 6f
        )
        _uiState.update {
            it.copy(config = it.config.copy(stickers = it.config.stickers + sticker))
        }
    }

    fun moveSticker(id: String, normX: Float, normY: Float) {
        _uiState.update { state ->
            val updated = state.config.stickers.map {
                if (it.id == id) it.copy(
                    normX = normX.coerceIn(0.1f, 0.9f),
                    normY = normY.coerceIn(0.1f, 0.9f)
                ) else it
            }
            state.copy(config = state.config.copy(stickers = updated))
        }
    }

    fun removeSticker(id: String) {
        _uiState.update { state ->
            state.copy(
                config = state.config.copy(
                    stickers = state.config.stickers.filterNot { it.id == id }
                )
            )
        }
    }

    fun resetAllEffects() {
        _uiState.update {
            it.copy(
                config = EffectConfig(),
                previewBitmap = basePreviewBitmap
            )
        }
    }

    fun saveMasterpiece() {
        val photo = _uiState.value.photo ?: return
        val cfg = _uiState.value.config
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, batchProgressText = null) }
            try {
                val hiResSource = mediaManager.decodeBitmap(photo, maxDimension = 1600)
                if (hiResSource == null) {
                    _errorEvent.emit("无法读取原图进行高清渲染")
                    _uiState.update { it.copy(isSaving = false) }
                    return@launch
                }
                val rendered = ArtworkExporter.renderMasterpiece(hiResSource, cfg, photo.title)
                if (hiResSource != rendered && !hiResSource.isRecycled) hiResSource.recycle()

                val tag = buildEffectTag(cfg)
                val outcome = repository.saveCreativeArtwork(
                    sourcePhoto = photo,
                    renderedBitmap = rendered,
                    effectTag = tag
                )
                if (!rendered.isRecycled) rendered.recycle()

                val msg = if (outcome.savedToSystemPhotos) {
                    "「$tag」已保存至手机系统相册与展厅"
                } else {
                    "「$tag」已保存至 Atelier 展厅"
                }
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        savedCount = it.savedCount + 1,
                        lastSavedPhotoId = outcome.photoId
                    )
                }
                _savedNoticeEvent.emit(StudioSaveNotice(outcome.photoId, msg))
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _errorEvent.emit("保存特效成品失败，请重试")
            }
        }
    }

    fun batchSavePresetPack() {
        val photo = _uiState.value.photo ?: return
        val presets = CURATED_FX_PRESETS
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, batchProgressText = "准备渲染 ${presets.size} 种风格…") }
            var hiResSource: Bitmap? = null
            try {
                hiResSource = mediaManager.decodeBitmap(photo, maxDimension = 1280)
                if (hiResSource == null) {
                    _errorEvent.emit("无法读取原图进行批量保存")
                    _uiState.update { it.copy(isSaving = false, batchProgressText = null) }
                    return@launch
                }

                var lastId = -1L
                var systemCount = 0
                presets.forEachIndexed { idx, preset ->
                    _uiState.update {
                        it.copy(batchProgressText = "连存 ${idx + 1}/${presets.size} · ${preset.name}")
                    }
                    val rendered = ArtworkExporter.renderMasterpiece(hiResSource, preset.config, photo.title)
                    val outcome = repository.saveCreativeArtwork(
                        sourcePhoto = photo,
                        renderedBitmap = rendered,
                        effectTag = preset.name
                    )
                    if (!rendered.isRecycled) rendered.recycle()
                    lastId = outcome.photoId
                    if (outcome.savedToSystemPhotos) systemCount++
                }

                val msg = if (systemCount > 0) {
                    "已连存 ${presets.size} 款特效大片至手机相册与展厅"
                } else {
                    "已连存 ${presets.size} 款特效大片至 Atelier 展厅"
                }
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        batchProgressText = null,
                        savedCount = it.savedCount + presets.size,
                        lastSavedPhotoId = if (lastId > 0) lastId else it.lastSavedPhotoId
                    )
                }
                if (lastId > 0) {
                    _savedNoticeEvent.emit(StudioSaveNotice(lastId, msg))
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false, batchProgressText = null) }
                _errorEvent.emit("批量连存中途中断，请重试")
            } finally {
                if (hiResSource != null && !hiResSource.isRecycled) {
                    hiResSource.recycle()
                }
            }
        }
    }

    fun exportToCustomUri(targetUri: Uri) {
        val photo = _uiState.value.photo ?: return
        val cfg = _uiState.value.config
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val hiResSource = mediaManager.decodeBitmap(photo, maxDimension = 1600)
                if (hiResSource == null) {
                    _errorEvent.emit("无法读取原图")
                    _uiState.update { it.copy(isSaving = false) }
                    return@launch
                }
                val rendered = ArtworkExporter.renderMasterpiece(hiResSource, cfg, photo.title)
                if (hiResSource != rendered && !hiResSource.isRecycled) hiResSource.recycle()
                val ok = mediaManager.writeBitmapToUri(rendered, targetUri)
                if (!rendered.isRecycled) rendered.recycle()
                _uiState.update { it.copy(isSaving = false) }
                if (ok) {
                    _errorEvent.emit("已导出高清文件到指定位置")
                } else {
                    _errorEvent.emit("写入指定位置失败，请重试")
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _errorEvent.emit("导出失败，请重试")
            }
        }
    }

    fun shareMasterpiece() {
        val photo = _uiState.value.photo ?: return
        val cfg = _uiState.value.config
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val hiResSource = mediaManager.decodeBitmap(photo, maxDimension = 1600)
                if (hiResSource == null) {
                    _errorEvent.emit("无法读取原图进行分享")
                    _uiState.update { it.copy(isSaving = false) }
                    return@launch
                }
                val rendered = ArtworkExporter.renderMasterpiece(hiResSource, cfg, photo.title)
                if (hiResSource != rendered && !hiResSource.isRecycled) hiResSource.recycle()
                val uri = mediaManager.createShareUri(rendered)
                if (!rendered.isRecycled) rendered.recycle()
                _uiState.update { it.copy(isSaving = false) }
                _shareUriEvent.emit(uri)
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _errorEvent.emit("分享导出失败，请重试")
            }
        }
    }

    private fun buildEffectTag(cfg: EffectConfig): String {
        val parts = mutableListOf<String>()
        if (cfg.filter != ArtFilter.ORIGINAL) parts.add(cfg.filter.displayName)
        if (cfg.hologramFoil) parts.add("全息")
        if (cfg.particleStyle != ParticleStyle.NONE) parts.add(cfg.particleStyle.displayName)
        if (cfg.frameStyle != FrameStyle.NONE) parts.add(cfg.frameStyle.displayName)
        if (cfg.stickers.isNotEmpty()) parts.add("贴纸")
        return if (parts.isEmpty()) "精修典藏" else parts.take(2).joinToString("+")
    }

    private fun updateConfig(transform: (EffectConfig) -> EffectConfig) {
        _uiState.update { it.copy(config = transform(it.config)) }
        schedulePreviewRender()
    }

    private fun schedulePreviewRender() {
        val base = basePreviewBitmap ?: return
        val cfg = _uiState.value.config
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            delay(45L)
            _uiState.update { it.copy(isRenderingPreview = true) }
            val result = EffectProcessor.processBitmap(base, cfg)
            _uiState.update { it.copy(previewBitmap = result, isRenderingPreview = false) }
        }
    }
}
