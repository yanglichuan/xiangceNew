package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ParticleStyle
import com.example.ui.components.ParticleOverlayCanvas
import com.example.ui.components.rememberTiltOffset
import com.example.ui.theme.AtelierGalleryTheme
import com.example.ui.theme.MuseumFilmRed
import com.example.ui.theme.ScreeningBorder
import com.example.ui.theme.ScreeningChampagne
import com.example.ui.theme.ScreeningIvory
import com.example.ui.theme.ScreeningMuted
import com.example.ui.theme.ScreeningObsidian
import com.example.ui.theme.ScreeningSurface
import com.example.ui.theme.ScreeningSurfaceHigh
import com.example.ui.viewmodel.GalleryViewModel

@Composable
fun PhotoDetailScreen(
    photoId: Long,
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    onOpenStudio: (Long) -> Unit
) {
    val photo by viewModel.observePhoto(photoId).collectAsStateWithLifecycle(initialValue = null)
    val tilt by rememberTiltOffset()
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var activeParticle by remember { mutableStateOf(ParticleStyle.DUST_BOKEH) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val smoothDragX by animateFloatAsState(
        targetValue = dragX,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow),
        label = "drag_x"
    )
    val smoothDragY by animateFloatAsState(
        targetValue = dragY,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow),
        label = "drag_y"
    )

    val combinedX = (tilt.x + smoothDragX).coerceIn(-1.2f, 1.2f)
    val combinedY = (tilt.y + smoothDragY).coerceIn(-1.2f, 1.2f)
    val density = LocalDensity.current.density

    AtelierGalleryTheme(darkScreeningRoom = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ScreeningObsidian, Color(0xFF141317), ScreeningObsidian)
                    )
                )
        ) {
            val currentPhoto = photo
            if (currentPhoto != null) {
                // Top Screening Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ScreeningSurface,
                        border = BorderStroke(1.dp, ScreeningBorder)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = ScreeningIvory
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = ScreeningSurface,
                            border = BorderStroke(1.dp, ScreeningBorder)
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleFavorite(currentPhoto) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("detail_favorite_button")
                            ) {
                                Icon(
                                    imageVector = if (currentPhoto.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = stringResource(R.string.cd_favorite),
                                    tint = if (currentPhoto.isFavorite) MuseumFilmRed else ScreeningIvory
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = ScreeningSurface,
                            border = BorderStroke(1.dp, ScreeningBorder)
                        ) {
                            IconButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("detail_delete_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = stringResource(R.string.cd_delete_photo),
                                    tint = ScreeningMuted
                                )
                            }
                        }
                    }
                }

                // Naked-Eye 3D Parallax Frame Stage
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 76.dp, bottom = 148.dp, start = 24.dp, end = 24.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    dragX = 0f
                                    dragY = 0f
                                },
                                onDragCancel = {
                                    dragX = 0f
                                    dragY = 0f
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                dragX = (dragX + dragAmount.x / 260f).coerceIn(-1f, 1f)
                                dragY = (dragY + dragAmount.y / 260f).coerceIn(-1f, 1f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Layer 1: Ambient Blurred Aura (Depth -1)
                    AsyncImage(
                        model = viewModel.mediaManager.resolveCoilModel(currentPhoto),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .widthIn(max = 420.dp)
                            .fillMaxWidth(0.84f)
                            .aspectRatio(currentPhoto.aspectRatio.coerceIn(0.62f, 1.4f))
                            .graphicsLayer {
                                translationX = -combinedX * 26f
                                translationY = -combinedY * 26f
                                alpha = 0.42f
                            }
                            .blur(36.dp)
                    )

                    // Layer 2: 3D Tilted Museum Frame + Window-Parallax Artwork
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 440.dp)
                            .fillMaxWidth()
                            .aspectRatio(currentPhoto.aspectRatio.coerceIn(0.62f, 1.4f))
                            .graphicsLayer {
                                cameraDistance = 15f * density
                                rotationY = combinedX * 15f
                                rotationX = -combinedY * 13f
                                translationX = combinedX * 10f
                                translationY = combinedY * 10f
                            },
                        shape = RoundedCornerShape(22.dp),
                        color = ScreeningSurface,
                        shadowElevation = 28.dp,
                        border = BorderStroke(1.2.dp, ScreeningChampagne.copy(alpha = 0.42f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                // Inner image moves smoothly inside the frame window for true 3D depth
                                AsyncImage(
                                    model = viewModel.mediaManager.resolveCoilModel(currentPhoto),
                                    contentDescription = currentPhoto.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = 1.08f
                                            scaleY = 1.08f
                                            translationX = -combinedX * 18f
                                            translationY = -combinedY * 18f
                                        }
                                )

                                // Dynamic Specular Sheen
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.14f),
                                                    Color.Transparent,
                                                    Color.Transparent
                                                ),
                                                start = Offset(
                                                    x = (0.5f - combinedX * 0.4f) * 600f,
                                                    y = (0.2f - combinedY * 0.4f) * 600f
                                                ),
                                                end = Offset(900f, 1200f)
                                            )
                                        )
                                )

                                // Live Particle Atmosphere
                                ParticleOverlayCanvas(
                                    style = activeParticle,
                                    parallaxOffset = Offset(combinedX, combinedY),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Layer 3: Floating Glassmorphic EXIF Tag (Front Z-Plane)
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp)
                                    .graphicsLayer {
                                        translationX = combinedX * 18f
                                        translationY = combinedY * 18f
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = ScreeningObsidian.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, ScreeningBorder)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(
                                        text = currentPhoto.catalogCode,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ScreeningChampagne
                                    )
                                    Text(
                                        text = currentPhoto.exifSummary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ScreeningIvory
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Action Plaque
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentPhoto.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = ScreeningIvory
                    )
                    Text(
                        text = "滑动屏幕体验裸眼 3D 视差光影",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScreeningMuted
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            onClick = {
                                val list = ParticleStyle.entries
                                activeParticle = list[(list.indexOf(activeParticle) + 1) % list.size]
                            },
                            shape = RoundedCornerShape(50),
                            color = ScreeningSurfaceHigh,
                            border = BorderStroke(1.dp, ScreeningBorder),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("detail_cycle_particle_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "切换光斑特效",
                                    tint = ScreeningChampagne,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeParticle.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ScreeningIvory
                                )
                            }
                        }

                        Surface(
                            onClick = { onOpenStudio(currentPhoto.id) },
                            shape = RoundedCornerShape(50),
                            color = ScreeningChampagne,
                            modifier = Modifier
                                .weight(1.25f)
                                .minimumInteractiveComponentSize()
                                .testTag("detail_open_studio_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoFixHigh,
                                    contentDescription = "进入创意特效工坊",
                                    tint = ScreeningObsidian,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "进入特效工坊",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ScreeningObsidian
                                )
                            }
                        }
                    }
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        containerColor = ScreeningSurface,
                        titleContentColor = ScreeningIvory,
                        textContentColor = ScreeningMuted,
                        title = { Text("移出展厅？") },
                        text = { Text("确定要将「${currentPhoto.title}」从当前相册展厅中移除吗？") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteDialog = false
                                    viewModel.deletePhoto(currentPhoto)
                                    onBack()
                                }
                            ) {
                                Text("确认移除", color = MuseumFilmRed)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("保留", color = ScreeningIvory)
                            }
                        }
                    )
                }
            }
        }
    }
}
