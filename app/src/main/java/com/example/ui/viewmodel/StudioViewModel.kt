package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AtelierContainer
import com.example.data.model.ArtFilter
import com.example.data.model.EffectConfig
import com.example.data.model.FrameStyle
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

data class StudioUiState(
    val photo: PhotoEntity? = null,
    val config: EffectConfig = EffectConfig(),
    val previewBitmap: Bitmap? = null,
    val isRenderingPreview: Boolean = false,
    val isSaving: Boolean = false,
    val activeTab: Int = 0
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AtelierContainer.provideRepository(application)
    private val mediaManager = repository.mediaManager

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private val _savedPhotoEvent = MutableSharedFlow<Long>(extraBufferCapacity = 2)
    val savedPhotoEvent = _savedPhotoEvent.asSharedFlow()

    private val _shareUriEvent = MutableSharedFlow<Uri>(extraBufferCapacity = 2)
    val shareUriEvent = _shareUriEvent.asSharedFlow()

    private val _errorEvent = MutableSharedFlow<String>(extraBufferCapacity = 2)
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

    fun selectFilter(filter: ArtFilter) {
        updateConfig { it.copy(filter = filter) }
    }

    fun setFilterIntensity(value: Float) {
        updateConfig { it.copy(filterIntensity = value.coerceIn(0f, 1f)) }
    }

    fun setParallaxDepth(value: Float) {
        updateConfig { it.copy(parallaxDepth = value.coerceIn(0f, 1f)) }
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

    fun setLuminousTextNormY(normY: Float) {
        _uiState.update {
            it.copy(config = it.config.copy(luminousTextNormY = normY.coerceIn(0.15f, 0.88f)))
        }
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
            _uiState.update { it.copy(isSaving = true) }
            try {
                val hiResSource = mediaManager.decodeBitmap(photo, maxDimension = 1600)
                if (hiResSource == null) {
                    _errorEvent.emit("无法读取原图进行高清渲染")
                    _uiState.update { it.copy(isSaving = false) }
                    return@launch
                }
                val rendered = ArtworkExporter.renderMasterpiece(hiResSource, cfg, photo.title)
                if (hiResSource != rendered) hiResSource.recycle()
                val newId = repository.saveCreativeArtwork(
                    sourcePhoto = photo,
                    renderedBitmap = rendered,
                    effectTag = cfg.filter.tagCode
                )
                rendered.recycle()
                _uiState.update { it.copy(isSaving = false) }
                _savedPhotoEvent.emit(newId)
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _errorEvent.emit("保存特效成品失败，请重试")
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
                if (hiResSource != rendered) hiResSource.recycle()
                val uri = mediaManager.createShareUri(rendered)
                rendered.recycle()
                _uiState.update { it.copy(isSaving = false) }
                _shareUriEvent.emit(uri)
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _errorEvent.emit("分享导出失败，请重试")
            }
        }
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
            delay(55L)
            _uiState.update { it.copy(isRenderingPreview = true) }
            val result = EffectProcessor.processBitmap(base, cfg)
            _uiState.update { it.copy(previewBitmap = result, isRenderingPreview = false) }
        }
    }
}
