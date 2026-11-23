package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExhibitionFilter
import com.example.data.model.GalleryMode
import com.example.ui.components.ModeSwitcherBar
import com.example.ui.components.MuseumHeader
import com.example.ui.theme.MuseumBrassGold
import com.example.ui.theme.MuseumHairline
import com.example.ui.theme.MuseumInk
import com.example.ui.theme.MuseumPaper
import com.example.ui.theme.MuseumSlate
import com.example.ui.viewmodel.GalleryViewModel

@Composable
fun GalleryHubScreen(
    viewModel: GalleryViewModel,
    onOpenDetail: (Long) -> Unit,
    onOpenStudio: (Long) -> Unit,
    onOpenCollage: () -> Unit,
    onOpenMotionReel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 12)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importPhotos(uris)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MuseumPaper,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 84.dp)
            )
        }
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MuseumPaper)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MuseumHeader(
                    photoCount = uiState.photos.size,
                    activeMode = uiState.activeMode,
                    activeFilter = uiState.exhibitionFilter,
                    onFilterSelected = viewModel::setExhibitionFilter,
                    onImportClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onCollageClick = onOpenCollage,
                    onMotionReelClick = onOpenMotionReel
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (!uiState.isLoaded) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MuseumInk)
                        }
                    } else if (uiState.photos.isEmpty()) {
                        EmptyExhibitionView(
                            filter = uiState.exhibitionFilter,
                            onResetFilter = { viewModel.setExhibitionFilter(ExhibitionFilter.ALL) },
                            onImport = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    } else {
                        AnimatedContent(
                            targetState = uiState.activeMode,
                            transitionSpec = {
                                fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                                    fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            label = "gallery_mode_transition"
                        ) { mode ->
                            when (mode) {
                                GalleryMode.CORRIDOR_3D -> Corridor3DScreen(
                                    photos = uiState.photos,
                                    mediaManager = viewModel.mediaManager,
                                    onOpenDetail = onOpenDetail,
                                    onOpenStudio = onOpenStudio,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onQuickSave = { photo -> viewModel.quickSavePhotoWithEffect(photo) }
                                )
                                GalleryMode.CUBE_PARALLAX -> CubeParallaxScreen(
                                    photos = uiState.photos,
                                    mediaManager = viewModel.mediaManager,
                                    onOpenDetail = onOpenDetail
                                )
                                GalleryMode.GLOBE_3D -> Globe3DScreen(
                                    photos = uiState.photos,
                                    mediaManager = viewModel.mediaManager,
                                    onOpenDetail = onOpenDetail
                                )
                                GalleryMode.PHYSICS_PILE -> PhysicsPileScreen(
                                    photos = uiState.photos,
                                    mediaManager = viewModel.mediaManager,
                                    onOpenDetail = onOpenDetail
                                )
                                GalleryMode.TIMELINE -> TimelineScreen(
                                    photos = uiState.photos,
                                    columns = uiState.timelineColumns,
                                    selectedIds = uiState.selectedIds,
                                    mediaManager = viewModel.mediaManager,
                                    onChangeColumns = viewModel::setTimelineColumns,
                                    onToggleSelect = viewModel::toggleSelectPhoto,
                                    onClearSelection = viewModel::clearSelection,
                                    onCreateCollageFromSelection = onOpenCollage,
                                    onPlayReelFromSelection = onOpenMotionReel,
                                    onOpenDetail = onOpenDetail,
                                    onToggleFavorite = viewModel::toggleFavorite
                                )
                            }
                        }
                    }

                    if (uiState.isImporting) {
                        Surface(
                            modifier = Modifier.align(Alignment.Center),
                            shape = RoundedCornerShape(20.dp),
                            color = MuseumInk.copy(alpha = 0.90f),
                            shadowElevation = 12.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = MuseumBrassGold,
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "正在装裱导入照片…",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MuseumPaper
                                )
                            }
                        }
                    }
                }
            }

            ModeSwitcherBar(
                activeMode = uiState.activeMode,
                onModeSelected = viewModel::setMode,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun EmptyExhibitionView(
    filter: ExhibitionFilter,
    onResetFilter: () -> Unit,
    onImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoLibrary,
            contentDescription = null,
            tint = MuseumSlate,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = when (filter) {
                ExhibitionFilter.FX_ONLY -> "还没有创作特效作品"
                ExhibitionFilter.FAVORITES -> "暂无收藏的展品"
                ExhibitionFilter.ALL -> "展厅暂无作品"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MuseumInk
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when (filter) {
                ExhibitionFilter.FX_ONLY -> "打开任意作品点击「制作特效」，即可保存你的独家艺术大作"
                ExhibitionFilter.FAVORITES -> "点击任意照片右上角红心即可收入私藏列表"
                ExhibitionFilter.ALL -> "从手机相册导入照片，开启 3D 照片墙与特效创作"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MuseumSlate
        )
        Spacer(modifier = Modifier.height(20.dp))
        Surface(
            onClick = if (filter != ExhibitionFilter.ALL) onResetFilter else onImport,
            shape = RoundedCornerShape(50),
            color = MuseumInk,
            border = BorderStroke(1.dp, MuseumHairline),
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            Text(
                text = if (filter != ExhibitionFilter.ALL) "返回全部馆藏" else "从相册导入照片",
                style = MaterialTheme.typography.labelLarge,
                color = MuseumPaper,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}
