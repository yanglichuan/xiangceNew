package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.ParticleStyle
import com.example.data.model.SlideshowTransition
import com.example.ui.components.ParticleOverlayCanvas
import com.example.ui.theme.AtelierGalleryTheme
import com.example.ui.theme.ScreeningBorder
import com.example.ui.theme.ScreeningChampagne
import com.example.ui.theme.ScreeningIvory
import com.example.ui.theme.ScreeningObsidian
import com.example.ui.theme.ScreeningSurface
import com.example.ui.theme.ScreeningSurfaceHigh
import com.example.ui.viewmodel.GalleryViewModel
import kotlinx.coroutines.delay

@Composable
fun MotionReelScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val photos = remember(uiState.photos, uiState.selectedIds) {
        if (uiState.selectedIds.isNotEmpty()) {
            uiState.photos.filter { uiState.selectedIds.contains(it.id) }.ifEmpty { uiState.photos }
        } else {
            uiState.photos
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var fastTempo by remember { mutableStateOf(false) }
    var cinemaScope by remember { mutableStateOf(true) }
    var activeTransition by remember { mutableStateOf(SlideshowTransition.KEN_BURNS) }
    var activeParticle by remember { mutableStateOf(ParticleStyle.DUST_BOKEH) }
    var slideProgress by remember { mutableFloatStateOf(0f) }

    val durationMs = if (fastTempo) 2600L else 4500L

    LaunchedEffect(isPlaying, currentIndex, photos.size, durationMs) {
        if (!isPlaying || photos.isEmpty()) return@LaunchedEffect
        slideProgress = 0f
        val steps = 60
        val stepDelay = durationMs / steps
        for (i in 1..steps) {
            delay(stepDelay)
            slideProgress = i / steps.toFloat()
        }
        currentIndex = (currentIndex + 1) % photos.size
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns")
    val kenBurnsScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(5500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "kb_scale"
    )
    val kenBurnsPan by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "kb_pan"
    )

    val density = LocalDensity.current.density

    AtelierGalleryTheme(darkScreeningRoom = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreeningObsidian)
        ) {
            if (photos.isNotEmpty()) {
                val safeIndex = currentIndex.coerceIn(0, photos.lastIndex)

                AnimatedContent(
                    targetState = safeIndex,
                    transitionSpec = {
                        when (activeTransition) {
                            SlideshowTransition.KEN_BURNS -> {
                                (fadeIn(tween(800)) + scaleIn(initialScale = 1.06f, animationSpec = tween(800))) togetherWith
                                    fadeOut(tween(700))
                            }
                            SlideshowTransition.CUBE_3D_FLIP -> {
                                (slideInHorizontally(tween(700)) { it } + fadeIn(tween(500))) togetherWith
                                    (slideOutHorizontally(tween(700)) { -it } + fadeOut(tween(500)))
                            }
                            SlideshowTransition.SHUTTER_CURTAIN -> {
                                (scaleIn(initialScale = 0.85f, animationSpec = tween(550)) + fadeIn(tween(450))) togetherWith
                                    (scaleOut(targetScale = 1.12f, animationSpec = tween(550)) + fadeOut(tween(450)))
                            }
                            SlideshowTransition.FILM_DISSOLVE -> {
                                fadeIn(tween(950)) togetherWith fadeOut(tween(950))
                            }
                            SlideshowTransition.PRISM_SPLIT -> {
                                (slideInHorizontally(tween(650)) { it / 2 } + scaleIn(initialScale = 0.9f, animationSpec = tween(650)) + fadeIn(tween(500))) togetherWith
                                    (slideOutHorizontally(tween(650)) { -it / 2 } + fadeOut(tween(500)))
                            }
                            SlideshowTransition.VINYL_SPIN -> {
                                (scaleIn(initialScale = 0.45f, animationSpec = tween(700)) + fadeIn(tween(600))) togetherWith
                                    (scaleOut(targetScale = 1.35f, animationSpec = tween(700)) + fadeOut(tween(500)))
                            }
                        }
                    },
                    label = "reel_stage"
                ) { idx ->
                    val photo = photos[idx]
                    val cubeAngle = if (activeTransition == SlideshowTransition.CUBE_3D_FLIP) {
                        (slideProgress - 0.5f) * 16f
                    } else 0f
                    val spinAngle = if (activeTransition == SlideshowTransition.VINYL_SPIN) {
                        slideProgress * 12f
                    } else 0f

                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = viewModel.mediaManager.resolveCoilModel(photo),
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    cameraDistance = 16f * density
                                    scaleX = if (activeTransition == SlideshowTransition.KEN_BURNS) kenBurnsScale else 1.06f
                                    scaleY = if (activeTransition == SlideshowTransition.KEN_BURNS) kenBurnsScale else 1.06f
                                    translationX = if (activeTransition == SlideshowTransition.KEN_BURNS) kenBurnsPan else 0f
                                    rotationY = cubeAngle
                                    rotationZ = spinAngle
                                    transformOrigin = TransformOrigin.Center
                                }
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.58f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )

                        ParticleOverlayCanvas(
                            style = activeParticle,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Cinema 2.39:1 Letterbox Bars
                if (cinemaScope) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(84.dp)
                            .background(Color.Black.copy(alpha = 0.92f))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(215.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.96f))
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        photos.forEachIndexed { idx, _ ->
                            val fill = when {
                                idx < safeIndex -> 1f
                                idx == safeIndex -> slideProgress
                                else -> 0f
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fill)
                                        .height(3.dp)
                                        .background(ScreeningChampagne)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ScreeningObsidian.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, ScreeningBorder)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("reel_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "退出影集", tint = ScreeningIvory)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = {
                                    val list = ParticleStyle.entries
                                    activeParticle = list[(list.indexOf(activeParticle) + 1) % list.size]
                                },
                                shape = RoundedCornerShape(50),
                                color = ScreeningObsidian.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, ScreeningBorder),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = "粒子", tint = ScreeningChampagne, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(activeParticle.displayName, style = MaterialTheme.typography.labelLarge, color = ScreeningIvory)
                                }
                            }

                            Surface(
                                onClick = { cinemaScope = !cinemaScope },
                                shape = RoundedCornerShape(50),
                                color = ScreeningObsidian.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, if (cinemaScope) ScreeningChampagne else ScreeningBorder),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.AspectRatio, contentDescription = "宽银幕", tint = ScreeningChampagne, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("2.39:1", style = MaterialTheme.typography.labelLarge, color = ScreeningIvory)
                                }
                            }

                            Surface(
                                onClick = { fastTempo = !fastTempo },
                                shape = RoundedCornerShape(50),
                                color = ScreeningObsidian.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, ScreeningBorder),
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("reel_tempo_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Speed, contentDescription = "切换节奏", tint = ScreeningChampagne, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (fastTempo) "2.6s" else "4.5s", style = MaterialTheme.typography.labelLarge, color = ScreeningIvory)
                                }
                            }
                        }
                    }
                }

                val currentPhoto = photos[safeIndex]
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${currentPhoto.catalogCode} · ${activeTransition.code}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScreeningChampagne
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentPhoto.title,
                        style = MaterialTheme.typography.displayMedium,
                        color = ScreeningIvory
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(SlideshowTransition.entries) { transition ->
                            val selected = activeTransition == transition
                            Surface(
                                onClick = { activeTransition = transition },
                                shape = RoundedCornerShape(50),
                                color = if (selected) ScreeningChampagne else ScreeningSurfaceHigh.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, if (selected) ScreeningChampagne else ScreeningBorder),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text(
                                    text = transition.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) ScreeningObsidian else ScreeningIvory,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ScreeningSurface.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, ScreeningBorder)
                        ) {
                            IconButton(
                                onClick = {
                                    currentIndex = if (currentIndex <= 0) photos.lastIndex else currentIndex - 1
                                    slideProgress = 0f
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.SkipPrevious, contentDescription = "上一张", tint = ScreeningIvory)
                            }
                        }

                        Surface(
                            onClick = { isPlaying = !isPlaying },
                            shape = CircleShape,
                            color = ScreeningChampagne,
                            modifier = Modifier.size(60.dp).testTag("reel_play_pause_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "暂停" else "播放",
                                    tint = ScreeningObsidian,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = ScreeningSurface.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, ScreeningBorder)
                        ) {
                            IconButton(
                                onClick = {
                                    currentIndex = (currentIndex + 1) % photos.size
                                    slideProgress = 0f
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.SkipNext, contentDescription = "下一张", tint = ScreeningIvory)
                            }
                        }
                    }
                }
            }
        }
    }
}
