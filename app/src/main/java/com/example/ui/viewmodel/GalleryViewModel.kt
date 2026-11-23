package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AtelierContainer
import com.example.data.model.GalleryMode
import com.example.data.model.PhotoEntity
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
    val photos: List<PhotoEntity> = emptyList(),
    val activeMode: GalleryMode = GalleryMode.CORRIDOR_3D,
    val timelineColumns: Int = 2,
    val favoritesOnly: Boolean = false,
    val isImporting: Boolean = false,
    val isLoaded: Boolean = false
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AtelierContainer.provideRepository(application)
    val mediaManager = repository.mediaManager

    private val _activeMode = MutableStateFlow(GalleryMode.CORRIDOR_3D)
    private val _timelineColumns = MutableStateFlow(2)
    private val _favoritesOnly = MutableStateFlow(false)
    private val _isImporting = MutableStateFlow(false)
    private val _isLoaded = MutableStateFlow(false)

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toastMessage = _toastMessage.asSharedFlow()

    val uiState: StateFlow<GalleryUiState> = combine(
        repository.allPhotos,
        _activeMode,
        _timelineColumns,
        _favoritesOnly,
        _isImporting,
        _isLoaded
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val all = values[0] as List<PhotoEntity>
        val mode = values[1] as GalleryMode
        val cols = values[2] as Int
        val favOnly = values[3] as Boolean
        val importing = values[4] as Boolean
        val loaded = values[5] as Boolean
        val filtered = if (favOnly) all.filter { it.isFavorite } else all
        GalleryUiState(
            photos = filtered,
            activeMode = mode,
            timelineColumns = cols,
            favoritesOnly = favOnly,
            isImporting = importing,
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
    }

    fun setTimelineColumns(columns: Int) {
        _timelineColumns.value = columns.coerceIn(2, 4)
    }

    fun toggleFavoritesFilter() {
        _favoritesOnly.value = !_favoritesOnly.value
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
