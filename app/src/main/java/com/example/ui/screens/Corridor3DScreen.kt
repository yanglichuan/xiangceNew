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
import com.example.data.model.PhotoEntity
import com.example.ui.components.rememberTiltOffset
import com.example.ui.theme.MuseumBrassGold
import com.example.ui.theme.MuseumCanvas
import com.example.ui.theme.MuseumFilmRed
import com.example.ui.theme.MuseumHairline
import com.example.ui.theme.MuseumInk
import com.example.ui.theme.MuseumPaper
import com.example.ui.theme.MuseumSlate
import kotlin.math.abs

@Composable
fun Corridor3DScreen(
    photos: List<PhotoEntity>,
    mediaManager: LocalMediaManager,
    onOpenDetail: (Long) -> Unit,
    onOpenStudio: (Long) -> Unit,
    onToggleFavorite: (PhotoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (photos.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { photos.size })
    val tilt by rememberTiltOffset()
    val density = LocalDensity.current.density
    val activePhoto = photos[pagerState.currentPage.coerceIn(0, photos.lastIndex)]

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val cardWidth = (maxWidth * 0.68f).coerceIn(220.dp, 360.dp)
            val horizontalPad = ((maxWidth - cardWidth) / 2f).coerceAtLeast(16.dp)

            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fixed(cardWidth),
                contentPadding = PaddingValues(horizontal = horizontalPad),
                pageSpacing = 12.dp,
                beyondViewportPageCount = 2,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val photo = photos[page]
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val absOffset = abs(pageOffset)

                val scale = (1f - absOffset * 0.16f).coerceIn(0.68f, 1f)
                val rotY = (pageOffset * -28f + tilt.x * 5f).coerceIn(-46f, 46f)
                val rotX = (tilt.y * -4f).coerceIn(-8f, 8f)
                val arcDropPx = absOffset * absOffset * 38f * density
                val curveXShift = -pageOffset * 26f * density
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
                            translationY = arcDropPx
                            translationX = curveXShift
                            alpha = itemAlpha
                        }
                ) {
                    // Main Museum Framed Artwork
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Gallery Floor Mirror Reflection
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .height(42.dp)
                            .alpha(0.28f * itemAlpha)
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

        // Focused Plaque & Quick Actions
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
                Spacer(modifier = Modifier.height(10.dp))
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

        // Minimalist Gallery Index Dots
        Row(
            modifier = Modifier.padding(top = 12.dp, bottom = 92.dp),
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
