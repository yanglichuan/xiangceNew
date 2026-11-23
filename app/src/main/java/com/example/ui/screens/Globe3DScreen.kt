package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.data.local.LocalMediaManager
import com.example.data.model.ParticleStyle
import com.example.data.model.PhotoEntity
import com.example.ui.components.ParticleOverlayCanvas
import com.example.ui.theme.MuseumBrassGold
import com.example.ui.theme.MuseumCanvas
import com.example.ui.theme.MuseumHairline
import com.example.ui.theme.MuseumInk
import com.example.ui.theme.MuseumSlate
import com.example.ui.theme.ScreeningChampagne
import com.example.ui.theme.ScreeningIvory
import com.example.ui.theme.ScreeningObsidian
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private data class SphereNode(
    val photo: PhotoEntity,
    val sx: Float,
    val sy: Float,
    val sz: Float,
    val scale: Float
)

@Composable
fun Globe3DScreen(
    photos: List<PhotoEntity>,
    mediaManager: LocalMediaManager,
    onOpenDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (photos.isEmpty()) return

    var mandalaMode by remember { mutableStateOf(false) }
    var rotYaw by remember { mutableFloatStateOf(0f) }
    var rotPitch by remember { mutableFloatStateOf(16f) }
    var isDragging by remember { mutableStateOf(false) }
    var mandalaIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(isDragging) {
        if (!isDragging) {
            var lastNanos = 0L
            while (true) {
                withFrameNanos { nanos ->
                    if (lastNanos != 0L) {
                        val dt = (nanos - lastNanos) / 1_000_000_000f
                        rotYaw = (rotYaw + dt * 18f) % 360f
                    }
                    lastNanos = nanos
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Submode Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Surface(
                onClick = { mandalaMode = false },
                shape = RoundedCornerShape(50),
                color = if (!mandalaMode) MuseumInk else MuseumCanvas,
                border = BorderStroke(1.dp, if (!mandalaMode) MuseumInk else MuseumHairline),
                modifier = Modifier.minimumInteractiveComponentSize().testTag("globe_sphere_mode_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = "3D 全景球幕",
                        tint = if (!mandalaMode) MuseumBrassGold else MuseumSlate,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "3D 全景球幕",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (!mandalaMode) ScreeningIvory else MuseumInk
                    )
                }
            }

            Surface(
                onClick = { mandalaMode = true },
                shape = RoundedCornerShape(50),
                color = if (mandalaMode) MuseumInk else MuseumCanvas,
                border = BorderStroke(1.dp, if (mandalaMode) MuseumInk else MuseumHairline),
                modifier = Modifier.minimumInteractiveComponentSize().testTag("globe_mandala_mode_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.BlurOn,
                        contentDescription = "8重万花筒",
                        tint = if (mandalaMode) MuseumBrassGold else MuseumSlate,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "8重万花筒",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (mandalaMode) ScreeningIvory else MuseumInk
                    )
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp, bottom = 92.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1A191E), ScreeningObsidian)
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false }
                    ) { change, dragAmount ->
                        change.consume()
                        rotYaw += dragAmount.x * 0.4f
                        rotPitch = (rotPitch - dragAmount.y * 0.35f).coerceIn(-75f, 75f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            ParticleOverlayCanvas(
                style = if (mandalaMode) ParticleStyle.AURORA_PRISM else ParticleStyle.NEON_SPARKS,
                modifier = Modifier.fillMaxSize()
            )

            if (!mandalaMode) {
                // Fibonacci Sphere Lattice of Photos
                val sphereNodes = remember(photos, rotYaw, rotPitch, maxWidth, maxHeight) {
                    val items = if (photos.size >= 10) photos.take(16) else photos + photos
                    val n = items.size
                    val goldenAngle = PI * (3.0 - sqrt(5.0))
                    val yawRad = Math.toRadians(rotYaw.toDouble())
                    val pitchRad = Math.toRadians(rotPitch.toDouble())
                    val cosY = cos(yawRad).toFloat()
                    val sinY = sin(yawRad).toFloat()
                    val cosP = cos(pitchRad).toFloat()
                    val sinP = sin(pitchRad).toFloat()

                    items.mapIndexed { i, photo ->
                        val y0 = 1f - (i / (n - 1).coerceAtLeast(1).toFloat()) * 2f
                        val radiusAtY = sqrt((1f - y0 * y0).coerceAtLeast(0f))
                        val theta = goldenAngle * i
                        val x0 = (cos(theta) * radiusAtY).toFloat()
                        val z0 = (sin(theta) * radiusAtY).toFloat()

                        // Rotate around Y then X
                        val x1 = x0 * cosY + z0 * sinY
                        val z1 = -x0 * sinY + z0 * cosY
                        val y2 = y0 * cosP - z1 * sinP
                        val z2 = y0 * sinP + z1 * cosP

                        val scale = (0.55f + (z2 + 1f) * 0.35f).coerceIn(0.45f, 1.25f)
                        SphereNode(photo, x1, y2, z2, scale)
                    }.sortedBy { it.sz }
                }

                val density = LocalDensity.current.density
                val radiusPx = with(LocalDensity.current) { (minOf(maxWidth, maxHeight) * 0.35f).toPx() }

                // Subtle Latitude/Longitude Wireframe Rings
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawCircle(
                        color = ScreeningChampagne.copy(alpha = 0.14f),
                        radius = radiusPx * 1.06f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = ScreeningChampagne.copy(alpha = 0.08f),
                        radius = radiusPx * 0.72f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1f)
                    )
                }

                sphereNodes.forEach { node ->
                    val px = node.sx * radiusPx
                    val py = node.sy * radiusPx
                    val alpha = (0.28f + (node.sz + 1f) * 0.38f).coerceIn(0.24f, 1f)

                    Surface(
                        modifier = Modifier
                            .offset { IntOffset(px.roundToInt(), py.roundToInt()) }
                            .size(86.dp, 108.dp)
                            .zIndex((node.sz + 1f) * 10f)
                            .graphicsLayer {
                                cameraDistance = 14f * density
                                scaleX = node.scale
                                scaleY = node.scale
                                rotationY = -node.sx * 32f
                                rotationX = node.sy * 28f
                                this.alpha = alpha
                            }
                            .clickable { onOpenDetail(node.photo.id) },
                        shape = RoundedCornerShape(10.dp),
                        color = MuseumCanvas,
                        shadowElevation = if (node.sz > 0.2f) 12.dp else 2.dp,
                        border = BorderStroke(1.dp, ScreeningChampagne.copy(alpha = if (node.sz > 0f) 0.7f else 0.2f))
                    ) {
                        AsyncImage(
                            model = mediaManager.resolveCoilModel(node.photo),
                            contentDescription = node.photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }

                Text(
                    text = "360° 拖拽旋转球幕星阵 · 轻点任意浮窗进入看图",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScreeningIvory.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                )
            } else {
                // 8-Fold Rotating Kaleidoscope Mandala
                val focusPhoto = photos[mandalaIndex % photos.size]
                val petalRadiusPx = with(LocalDensity.current) { (minOf(maxWidth, maxHeight) * 0.26f).toPx() }

                for (k in 0 until 8) {
                    val angleDeg = rotYaw + k * 45f
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val px = (cos(angleRad) * petalRadiusPx).toFloat()
                    val py = (sin(angleRad) * petalRadiusPx).toFloat()

                    Surface(
                        modifier = Modifier
                            .offset { IntOffset(px.roundToInt(), py.roundToInt()) }
                            .size(104.dp, 134.dp)
                            .graphicsLayer {
                                rotationZ = angleDeg + 90f
                                scaleX = if (k % 2 == 0) 1f else -1f
                                rotationY = (rotPitch * 0.4f).coerceIn(-25f, 25f)
                            }
                            .clickable { onOpenDetail(focusPhoto.id) },
                        shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                        color = MuseumCanvas,
                        border = BorderStroke(1.5.dp, ScreeningChampagne.copy(alpha = 0.65f))
                    ) {
                        AsyncImage(
                            model = mediaManager.resolveCoilModel(focusPhoto),
                            contentDescription = focusPhoto.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Center Jewel Medallion
                Surface(
                    onClick = { mandalaIndex = (mandalaIndex + 1) % photos.size },
                    shape = CircleShape,
                    color = ScreeningObsidian,
                    border = BorderStroke(2.dp, ScreeningChampagne),
                    shadowElevation = 16.dp,
                    modifier = Modifier.size(92.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = mediaManager.resolveCoilModel(focusPhoto),
                            contentDescription = focusPhoto.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                Text(
                    text = "点击中心切换万花筒核心画作 · 拖拽加速旋转",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScreeningChampagne,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                )
            }
        }
    }
}
