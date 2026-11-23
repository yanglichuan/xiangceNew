package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AtelierContainer
import com.example.data.model.CollageLayout
import com.example.data.model.ExhibitionFilter
import com.example.data.model.GalleryMode
import com.example.data.model.PhotoEntity
import com.example.domain.engine.ArtworkExporter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GalleryUiState(
    val allPhotos: List<PhotoEntity> = emptyList(),
    val photos: List<PhotoEntity> = emptyList(),
    val activeMode: GalleryMode = GalleryMode.CORRIDOR_3D,
    val exhibitionFilter: ExhibitionFilter = ExhibitionFilter.ALL,
    val timelineColumns: Int = 2,
    val selectedIds: Set<Long> = emptySet(),
    val isImporting: Boolean = false,
    val isSavingCollage: Boolean = false,
    val isLoaded: Boolean = false
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AtelierContainer.provideRepository(application)
    val mediaManager = repository.mediaManager

    private val _activeMode = MutableStateFlow(GalleryMode.CORRIDOR_3D)
    private val _exhibitionFilter = MutableStateFlow(ExhibitionFilter.ALL)
    private val _timelineColumns = MutableStateFlow(2)
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isImporting = MutableStateFlow(false)
    private val _isSavingCollage = MutableStateFlow(false)
    private val _isLoaded = MutableStateFlow(false)

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toastMessage = _toastMessage.asSharedFlow()

    val uiState: StateFlow<GalleryUiState> = combine(
        repository.allPhotos,
        _activeMode,
        _exhibitionFilter,
        _timelineColumns,
        _selectedIds,
        _isImporting,
        _isSavingCollage,
        _isLoaded
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val all = values[0] as List<PhotoEntity>
        val mode = values[1] as GalleryMode
        val filter = values[2] as ExhibitionFilter
        val cols = values[3] as Int
        @Suppress("UNCHECKED_CAST")
        val selected = values[4] as Set<Long>
        val importing = values[5] as Boolean
        val savingCollage = values[6] as Boolean
        val loaded = values[7] as Boolean

        val filtered = when (filter) {
            ExhibitionFilter.ALL -> all
            ExhibitionFilter.FX_ONLY -> all.filter { it.effectTag != null }
            ExhibitionFilter.FAVORITES -> all.filter { it.isFavorite }
        }
        GalleryUiState(
            allPhotos = all,
            photos = filtered,
            activeMode = mode,
            exhibitionFilter = filter,
            timelineColumns = cols,
            selectedIds = selected,
            isImporting = importing,
            isSavingCollage = savingCollage,
            isLoaded = loaded
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GalleryUiState()
    )

    init {
        viewModelScope.launch {
            repository.seedCuratedExhibitionIfEmpty()
            _isLoaded.value = true
        }
    }

    fun setMode(mode: GalleryMode) {
        _activeMode.value = mode
        if (mode != GalleryMode.TIMELINE) {
            _selectedIds.value = emptySet()
        }
    }

    fun setExhibitionFilter(filter: ExhibitionFilter) {
        _exhibitionFilter.value = filter
    }

    fun setTimelineColumns(columns: Int) {
        _timelineColumns.value = columns.coerceIn(2, 4)
    }

    fun toggleSelectPhoto(photoId: Long) {
        val current = _selectedIds.value
        _selectedIds.value = if (current.contains(photoId)) current - photoId else current + photoId
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun importPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isImporting.value = true
            val count = repository.importPickedPhotos(uris)
            _isImporting.value = false
            if (count > 0) {
                _toastMessage.emit("已策展导入 $count 张精选照片")
            } else {
                _toastMessage.emit("未能读取所选图片，请重试")
            }
        }
    }

    fun createAndSaveCollage(
        selectedPhotos: List<PhotoEntity>,
        layout: CollageLayout,
        title: String,
        darkTheme: Boolean,
        onDone: (Long) -> Unit
    ) {
        if (selectedPhotos.isEmpty()) return
        viewModelScope.launch {
            _isSavingCollage.value = true
            val bitmaps = mutableListOf<Bitmap>()
            try {
                selectedPhotos.take(4).forEach { p ->
                    mediaManager.decodeBitmap(p, maxDimension = 1000)?.let { bitmaps.add(it) }
                }
                if (bitmaps.isEmpty()) {
                    _toastMessage.emit("无法解码所选照片")
                    _isSavingCollage.value = false
                    return@launch
                }
                val poster = ArtworkExporter.renderCollagePoster(
                    bitmaps = bitmaps,
                    layout = layout,
                    title = title,
                    darkTheme = darkTheme
                )
                val newId = repository.saveCollageArtwork(title, layout, poster)
                poster.recycle()
                _selectedIds.value = emptySet()
                _isSavingCollage.value = false
                _toastMessage.emit("拼图海报已收入展厅")
                onDone(newId)
            } catch (_: Exception) {
                _isSavingCollage.value = false
                _toastMessage.emit("生成拼图海报失败，请重试")
            } finally {
                bitmaps.forEach { if (!it.isRecycled) it.recycle() }
            }
        }
    }

    fun toggleFavorite(photo: PhotoEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(photo)
        }
    }

    fun deletePhoto(photo: PhotoEntity) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
            _toastMessage.emit("已从展厅移除「${photo.title}」")
        }
    }

    fun observePhoto(photoId: Long) = repository.observePhoto(photoId)
}
