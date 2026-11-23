package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.data.local.LocalMediaManager
import com.example.data.model.ParticleStyle
import com.example.data.model.PhotoEntity
import com.example.ui.components.ParticleOverlayCanvas
import com.example.ui.components.rememberTiltOffset
import com.example.ui.theme.MuseumBrassGold
import com.example.ui.theme.MuseumCanvas
import com.example.ui.theme.MuseumFilmRed
import com.example.ui.theme.MuseumHairline
import com.example.ui.theme.MuseumInk
import com.example.ui.theme.MuseumPaper
import com.example.ui.theme.MuseumSlate
import kotlin.math.abs
import kotlin.math.sin

private enum class CorridorStyle(val label: String) {
    CURVED_ARC("弧形走廊"),
    SPIRAL_HELIX("3D 螺旋"),
    VINYL_RACK("黑胶唱片架")
}

@Composable
fun Corridor3DScreen(
    photos: List<PhotoEntity>,
    mediaManager: LocalMediaManager,
    onOpenDetail: (Long) -> Unit,
    onOpenStudio: (Long) -> Unit,
    onToggleFavorite: (PhotoEntity) -> Unit,
    onQuickSave: (PhotoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (photos.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { photos.size })
    val tilt by rememberTiltOffset()
    val density = LocalDensity.current.density
    var corridorStyle by remember { mutableStateOf(CorridorStyle.CURVED_ARC) }
    val activePhoto = photos[pagerState.currentPage.coerceIn(0, photos.lastIndex)]

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Corridor 3D Geometry Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            CorridorStyle.entries.forEach { style ->
                val selected = corridorStyle == style
                Surface(
                    onClick = { corridorStyle = style },
                    shape = RoundedCornerShape(50),
                    color = if (selected) MuseumBrassGold.copy(alpha = 0.20f) else Color.Transparent,
                    border = BorderStroke(1.dp, if (selected) MuseumBrassGold else MuseumHairline)
                ) {
                    Text(
                        text = style.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MuseumInk else MuseumSlate,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // Subtle warm museum dust motes floating in background
            ParticleOverlayCanvas(
                style = ParticleStyle.DUST_BOKEH,
                parallaxOffset = tilt,
                modifier = Modifier.fillMaxSize()
            )

            // Ambient spotlight halo behind center card
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(MuseumBrassGold.copy(alpha = 0.22f), Color.Transparent)
                        )
                    )
            )

            val cardWidth = (maxWidth * 0.66f).coerceIn(210.dp, 350.dp)
            val horizontalPad = ((maxWidth - cardWidth) / 2f).coerceAtLeast(16.dp)

            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fixed(cardWidth),
                contentPadding = PaddingValues(horizontal = horizontalPad),
                pageSpacing = if (corridorStyle == CorridorStyle.VINYL_RACK) (-28).dp else 10.dp,
                beyondViewportPageCount = 3,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val photo = photos[page]
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val absOffset = abs(pageOffset)

                val scale: Float
                val rotY: Float
                val rotX: Float
                val rotZ: Float
                val transY: Float
                val transX: Float
                when (corridorStyle) {
                    CorridorStyle.CURVED_ARC -> {
                        scale = (1f - absOffset * 0.16f).coerceIn(0.68f, 1f)
                        rotY = (pageOffset * -28f + tilt.x * 5f).coerceIn(-46f, 46f)
                        rotX = (tilt.y * -4f).coerceIn(-8f, 8f)
                        rotZ = 0f
                        transY = absOffset * absOffset * 38f * density
                        transX = -pageOffset * 26f * density
                    }
                    CorridorStyle.SPIRAL_HELIX -> {
                        scale = (1f - absOffset * 0.18f).coerceIn(0.64f, 1f)
                        rotY = (pageOffset * -42f).coerceIn(-68f, 68f)
                        rotX = (pageOffset * 10f).coerceIn(-20f, 20f)
                        rotZ = (pageOffset * -8f).coerceIn(-18f, 18f)
                        transY = sin(pageOffset * 1.1f) * 54f * density
                        transX = -pageOffset * 32f * density
                    }
                    CorridorStyle.VINYL_RACK -> {
                        scale = (1f - absOffset * 0.12f).coerceIn(0.72f, 1f)
                        rotY = if (absOffset < 0.15f) 0f else (if (pageOffset > 0) -54f else 54f)
                        rotX = 0f
                        rotZ = 0f
                        transY = absOffset * 14f * density
                        transX = 0f
                    }
                }
                val itemAlpha = (1f - absOffset * 0.34f).coerceIn(0.32f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(10f - absOffset)
                        .graphicsLayer {
                            cameraDistance = 14f * density
                            scaleX = scale
                            scaleY = scale
                            rotationY = rotY
                            rotationX = rotX
                            rotationZ = rotZ
                            translationY = transY
                            translationX = transX
                            alpha = itemAlpha
                        }
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(photo.aspectRatio.coerceIn(0.66f, 1.25f))
                            .testTag("corridor_card_${photo.id}")
                            .clickable { onOpenDetail(photo.id) },
                        shape = RoundedCornerShape(18.dp),
                        color = MuseumCanvas,
                        shadowElevation = if (absOffset < 0.3f) 20.dp else 8.dp,
                        border = BorderStroke(1.dp, MuseumHairline)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            AsyncImage(
                                model = mediaManager.resolveCoilModel(photo),
                                contentDescription = photo.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                            )

                            if (photo.effectTag != null) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp),
                                    shape = RoundedCornerShape(50),
                                    color = MuseumInk.copy(alpha = 0.82f)
                                ) {
                                    Text(
                                        text = photo.effectTag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MuseumBrassGold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onToggleFavorite(photo) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .minimumInteractiveComponentSize()
                                    .background(MuseumCanvas.copy(alpha = 0.78f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (photo.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "收藏",
                                    tint = if (photo.isFavorite) MuseumFilmRed else MuseumInk,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.84f)
                            .height(36.dp)
                            .alpha(0.26f * itemAlpha)
                            .graphicsLayer {
                                scaleY = -1f
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black)
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = mediaManager.resolveCoilModel(photo),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        AnimatedContent(
            targetState = activePhoto,
            transitionSpec = {
                fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                    fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
            },
            label = "corridor_plaque"
        ) { photo ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = photo.catalogCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = MuseumBrassGold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = photo.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MuseumInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { onOpenDetail(photo.id) },
                        shape = RoundedCornerShape(50),
                        color = MuseumCanvas,
                        border = BorderStroke(1.dp, MuseumHairline),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("corridor_detail_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.OpenInFull,
                                contentDescription = "3D 视差看图",
                                tint = MuseumInk,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "3D 看图",
                                style = MaterialTheme.typography.labelLarge,
                                color = MuseumInk
                            )
                        }
                    }

                    Surface(
                        onClick = { onQuickSave(photo) },
                        shape = CircleShape,
                        color = MuseumCanvas,
                        border = BorderStroke(1.dp, MuseumHairline),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("corridor_save_btn")
                    ) {
                        Box(
                            modifier = Modifier.padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "保存到手机相册",
                                tint = MuseumInk,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = { onOpenStudio(photo.id) },
                        shape = RoundedCornerShape(50),
                        color = MuseumInk,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("corridor_studio_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = "进入特效工坊",
                                tint = MuseumBrassGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "制作特效",
                                style = MaterialTheme.typography.labelLarge,
                                color = MuseumPaper
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 10.dp, bottom = 88.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            photos.take(10).forEachIndexed { idx, _ ->
                val active = idx == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .width(if (active) 20.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (active) MuseumInk else MuseumSlate.copy(alpha = 0.3f))
                )
            }
        }
    }
}
