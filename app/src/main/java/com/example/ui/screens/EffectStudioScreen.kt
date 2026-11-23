package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.FrameStyle
import com.example.ui.components.HologramFoilOverlay
import com.example.ui.components.InteractiveStickerItem
import com.example.ui.components.ParticleOverlayCanvas
import com.example.ui.components.rememberTiltOffset
import com.example.ui.theme.AtelierGalleryTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MuseumPaper
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.ScreeningBorder
import com.example.ui.theme.ScreeningChampagne
import com.example.ui.theme.ScreeningIvory
import com.example.ui.theme.ScreeningMuted
import com.example.ui.theme.ScreeningObsidian
import com.example.ui.theme.ScreeningSurface
import com.example.ui.theme.ScreeningSurfaceHigh
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun EffectStudioScreen(
    photoId: Long,
    onBack: () -> Unit,
    onSavedArtwork: (Long) -> Unit,
    studioViewModel: StudioViewModel = viewModel()
) {
    val uiState by studioViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val tilt by rememberTiltOffset()
    var dragTiltX by remember { mutableFloatStateOf(0f) }
    var dragTiltY by remember { mutableFloatStateOf(0f) }
    var photoBoxSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(photoId) {
        studioViewModel.loadPhoto(photoId)
    }

    LaunchedEffect(Unit) {
        studioViewModel.savedPhotoEvent.collect { newId ->
            onSavedArtwork(newId)
        }
    }

    LaunchedEffect(Unit) {
        studioViewModel.shareUriEvent.collect { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "分享 Atelier 特效作品"))
        }
    }

    LaunchedEffect(Unit) {
        studioViewModel.errorEvent.collect { err ->
            snackbarHostState.showSnackbar(err)
        }
    }

    AtelierGalleryTheme(darkScreeningRoom = true) {
        Scaffold(
            containerColor = ScreeningObsidian,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScreeningObsidian)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("studio_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = ScreeningIvory
                            )
                        }
                        Column {
                            Text(
                                text = "FX ATELIER · ${uiState.config.filter.tagCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ScreeningChampagne
                            )
                            Text(
                                text = "创意特效工坊",
                                style = MaterialTheme.typography.titleMedium,
                                color = ScreeningIvory
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = studioViewModel::resetAllEffects,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("studio_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "重置特效",
                                tint = ScreeningMuted
                            )
                        }

                        IconButton(
                            onClick = studioViewModel::shareMasterpiece,
                            enabled = !uiState.isSaving,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("studio_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "分享作品",
                                tint = ScreeningIvory
                            )
                        }

                        Surface(
                            onClick = studioViewModel::saveMasterpiece,
                            enabled = !uiState.isSaving,
                            shape = RoundedCornerShape(50),
                            color = ScreeningChampagne,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("studio_save_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (uiState.isSaving) {
                                    CircularProgressIndicator(
                                        color = ScreeningObsidian,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "保存成品",
                                        tint = ScreeningObsidian,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "保存成品",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ScreeningObsidian
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    dragTiltX = 0f
                                    dragTiltY = 0f
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                dragTiltX = (dragTiltX + dragAmount.x / 220f).coerceIn(-1f, 1f)
                                dragTiltY = (dragTiltY + dragAmount.y / 220f).coerceIn(-1f, 1f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = uiState.previewBitmap
                    val photo = uiState.photo
                    val cfg = uiState.config
                    val density = LocalDensity.current.density
                    val parallaxStrength = cfg.parallaxDepth
                    val px = (tilt.x + dragTiltX) * parallaxStrength
                    val py = (tilt.y + dragTiltY) * parallaxStrength

                    if (bitmap != null && photo != null) {
                        StudioFramedPreview(
                            frameStyle = cfg.frameStyle,
                            title = photo.title,
                            filterTag = cfg.filter.tagCode,
                            modifier = Modifier.graphicsLayer {
                                cameraDistance = 15f * density
                                rotationY = px * 16f
                                rotationX = -py * 14f
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(
                                        (bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1))
                                            .coerceIn(0.58f, 1.45f)
                                    )
                                    .onSizeChanged { photoBoxSize = it }
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = photo.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            val zoom = 1f + parallaxStrength * 0.08f
                                            scaleX = zoom
                                            scaleY = zoom
                                            translationX = -px * 14f
                                            translationY = -py * 14f
                                        }
                                )

                                if (cfg.hologramFoil) {
                                    HologramFoilOverlay(
                                        parallaxOffset = Offset(px, py),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                ParticleOverlayCanvas(
                                    style = cfg.particleStyle,
                                    parallaxOffset = Offset(px, py),
                                    modifier = Modifier.fillMaxSize()
                                )

                                cfg.stickers.forEach { sticker ->
                                    InteractiveStickerItem(
                                        sticker = sticker,
                                        canvasWidthPx = photoBoxSize.width.toFloat(),
                                        canvasHeightPx = photoBoxSize.height.toFloat(),
                                        editable = true,
                                        onMove = { nx, ny -> studioViewModel.moveSticker(sticker.id, nx, ny) },
                                        onRemove = { studioViewModel.removeSticker(sticker.id) }
                                    )
                                }

                                if (cfg.luminousText.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 18.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cfg.luminousText,
                                            color = Color(0xFFFFFDF7),
                                            fontFamily = PlayfairDisplayFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp,
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                shadow = if (cfg.luminousTextGlow) {
                                                    Shadow(
                                                        color = ScreeningChampagne,
                                                        offset = Offset.Zero,
                                                        blurRadius = 24f
                                                    )
                                                } else {
                                                    Shadow(
                                                        color = Color.Black.copy(alpha = 0.7f),
                                                        offset = Offset(0f, 3f),
                                                        blurRadius = 8f
                                                    )
                                                }
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        CircularProgressIndicator(color = ScreeningChampagne)
                    }

                    if (uiState.isRenderingPreview) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp),
                            shape = RoundedCornerShape(50),
                            color = ScreeningObsidian.copy(alpha = 0.75f)
                        ) {
                            Text(
                                text = "RENDERING",
                                style = MaterialTheme.typography.labelSmall,
                                color = ScreeningChampagne,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = ScreeningSurface,
                    border = BorderStroke(1.dp, ScreeningBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("滤镜模板", "3D镭射", "装裱贴纸", "精修参数").forEachIndexed { idx, title ->
                                val active = uiState.activeTab == idx
                                Surface(
                                    onClick = { studioViewModel.setActiveTab(idx) },
                                    shape = RoundedCornerShape(50),
                                    color = if (active) ScreeningChampagne else ScreeningSurfaceHigh,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("studio_tab_$idx")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (active) ScreeningObsidian else ScreeningIvory
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        when (uiState.activeTab) {
                            0 -> StudioFilterTab(
                                config = uiState.config,
                                onApplyPreset = studioViewModel::applyPreset,
                                onSelectFilter = studioViewModel::selectFilter,
                                onIntensityChange = studioViewModel::setFilterIntensity
                            )
                            1 -> StudioParallaxParticleTab(
                                config = uiState.config,
                                onParallaxChange = studioViewModel::setParallaxDepth,
                                onToggleHologram = studioViewModel::toggleHologramFoil,
                                onSelectParticle = studioViewModel::selectParticle
                            )
                            2 -> StudioFrameStickerTab(
                                config = uiState.config,
                                onSelectFrame = studioViewModel::selectFrame,
                                onAddSticker = studioViewModel::addSticker,
                                onTextChange = studioViewModel::setLuminousText,
                                onToggleGlow = studioViewModel::toggleLuminousGlow
                            )
                            3 -> StudioFineTuneTab(
                                config = uiState.config,
                                onGrainChange = studioViewModel::setGrain,
                                onVignetteChange = studioViewModel::setVignette,
                                onWarmthChange = studioViewModel::setWarmth,
                                onContrastChange = studioViewModel::setContrast
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioFramedPreview(
    frameStyle: FrameStyle,
    title: String,
    filterTag: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    when (frameStyle) {
        FrameStyle.NONE -> {
            Box(
                modifier = modifier
                    .shadow(20.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
            ) {
                content()
            }
        }
        FrameStyle.MUSEUM_MATTE -> {
            Surface(
                modifier = modifier,
                color = MuseumPaper,
                shape = RoundedCornerShape(6.dp),
                shadowElevation = 22.dp
            ) {
                Column(
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.border(1.dp, Color(0xFFCFC8BA))) {
                        content()
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "$title · $filterTag",
                        fontFamily = PlayfairDisplayFontFamily,
                        fontSize = 12.sp,
                        color = Color(0xFF141413)
                    )
                }
            }
        }
        FrameStyle.POLAROID -> {
            Surface(
                modifier = modifier,
                color = Color(0xFFFCFBF8),
                shape = RoundedCornerShape(4.dp),
                shadowElevation = 22.dp
            ) {
                Column(
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    content()
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = title,
                        fontFamily = PlayfairDisplayFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp,
                        color = Color(0xFF2A2927)
                    )
                }
            }
        }
        FrameStyle.FILM_STRIP_35MM -> {
            Surface(
                modifier = modifier,
                color = Color(0xFF101012),
                shape = RoundedCornerShape(6.dp),
                shadowElevation = 22.dp,
                border = BorderStroke(1.dp, Color(0xFF2C2C30))
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                    FilmSprocketRow()
                    Spacer(modifier = Modifier.height(6.dp))
                    content()
                    Spacer(modifier = Modifier.height(6.dp))
                    FilmSprocketRow()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ATELIER 400 · $filterTag",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = ScreeningChampagne
                    )
                }
            }
        }
        FrameStyle.GALLERY_BRASS -> {
            Surface(
                modifier = modifier,
                color = Color(0xFF161513),
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 24.dp,
                border = BorderStroke(3.dp, ScreeningChampagne)
            ) {
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .border(1.dp, ScreeningChampagne.copy(alpha = 0.6f))
                        .padding(4.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun FilmSprocketRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(8) {
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(7.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFEBE7DF))
            )
        }
    }
}
