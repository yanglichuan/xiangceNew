package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.data.local.LocalMediaManager
import com.example.data.model.PhotoEntity
import com.example.domain.engine.Physics2DEngine
import com.example.domain.engine.PhysicsCardBody
import com.example.domain.engine.PhysicsMode
import com.example.ui.components.rememberTiltOffset
import com.example.ui.theme.MuseumBrassGold
import com.example.ui.theme.MuseumCanvas
import com.example.ui.theme.MuseumHairline
import com.example.ui.theme.MuseumInk
import com.example.ui.theme.MuseumPaper
import com.example.ui.theme.MuseumSlate
import com.example.ui.theme.MuseumWarmStone
import kotlin.math.roundToInt

@Composable
fun PhysicsPileScreen(
    photos: List<PhotoEntity>,
    mediaManager: LocalMediaManager,
    onOpenDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = remember { Physics2DEngine() }
    var bodies by remember { mutableStateOf<List<PhysicsCardBody>>(emptyList()) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var physicsMode by remember { mutableStateOf(PhysicsMode.GRAVITY) }
    var vortexTouch by remember { mutableStateOf<Offset?>(null) }

    val tilt by rememberTiltOffset()
    val density = LocalDensity.current
    val cardBaseWidthPx = with(density) { 114.dp.toPx() }
    val photoMap = remember(photos) { photos.associateBy { it.id } }

    LaunchedEffect(photos, boxSize) {
        if (boxSize.width > 0 && boxSize.height > 0) {
            bodies = engine.syncBodies(
                photoIdsAndRatios = photos.map { it.id to it.aspectRatio },
                widthPx = boxSize.width.toFloat(),
                heightPx = boxSize.height.toFloat(),
                cardBaseWidthPx = cardBaseWidthPx
            )
        }
    }

    LaunchedEffect(boxSize, physicsMode) {
        if (boxSize.width <= 0 || boxSize.height <= 0) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (lastNanos != 0L) {
                    val dt = (nanos - lastNanos) / 1_000_000_000f
                    val gx = tilt.x * 900f
                    val gy = 1080f + tilt.y * 420f
                    bodies = engine.step(
                        dtSec = dt,
                        gravityX = gx,
                        gravityY = gy,
                        mode = physicsMode
                    )
                }
                lastNanos = nanos
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Surface(
                    onClick = { engine.impulseBurst() },
                    shape = RoundedCornerShape(50),
                    color = MuseumInk,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("physics_burst_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = "脉冲弹跳",
                            tint = MuseumBrassGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "能量弹跳",
                            style = MaterialTheme.typography.labelLarge,
                            color = MuseumPaper
                        )
                    }
                }
            }

            items(PhysicsMode.entries) { mode ->
                val active = physicsMode == mode
                Surface(
                    onClick = {
                        physicsMode = mode
                        if (mode != PhysicsMode.VORTEX) {
                            vortexTouch = null
                            engine.setVortexAnchor(null, null)
                        }
                    },
                    shape = RoundedCornerShape(50),
                    color = if (active) MuseumBrassGold else MuseumCanvas,
                    border = BorderStroke(1.dp, if (active) MuseumBrassGold else MuseumHairline),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("physics_mode_${mode.name}")
                ) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MuseumInk,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
                    )
                }
            }

            item {
                Surface(
                    onClick = { engine.tidyStack() },
                    shape = RoundedCornerShape(50),
                    color = MuseumCanvas,
                    border = BorderStroke(1.dp, MuseumHairline),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("physics_tidy_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GridOn,
                            contentDescription = "整齐整理",
                            tint = MuseumInk,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "整理",
                            style = MaterialTheme.typography.labelLarge,
                            color = MuseumInk
                        )
                    }
                }
            }
        }

        Text(
            text = if (physicsMode == PhysicsMode.VORTEX) {
                "磁力漩涡已开启：在画布任意位置滑动，吸附照片围绕指尖旋转"
            } else {
                "按住任意照片自由抛掷碰撞，轻点卡片立即展开"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MuseumSlate,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp, bottom = 92.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MuseumWarmStone.copy(alpha = 0.72f), Color(0xFFE5DFD3))
                    )
                )
                .onSizeChanged { boxSize = it }
                .pointerInput(physicsMode) {
                    if (physicsMode == PhysicsMode.VORTEX) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                vortexTouch = pos
                                engine.setVortexAnchor(pos.x, pos.y)
                            },
                            onDragEnd = {
                                vortexTouch = null
                                engine.setVortexAnchor(null, null)
                            },
                            onDragCancel = {
                                vortexTouch = null
                                engine.setVortexAnchor(null, null)
                            }
                        ) { change, _ ->
                            change.consume()
                            vortexTouch = change.position
                            engine.setVortexAnchor(change.position.x, change.position.y)
                        }
                    }
                }
        ) {
            // Vortex Orbital Indicator Rings
            if (physicsMode == PhysicsMode.VORTEX && boxSize.width > 0) {
                val center = vortexTouch ?: Offset(boxSize.width * 0.5f, boxSize.height * 0.46f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = MuseumBrassGold.copy(alpha = 0.30f),
                        radius = 120f,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )
                    drawCircle(
                        color = MuseumBrassGold.copy(alpha = 0.16f),
                        radius = 220f,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                }
            }

            bodies.forEach { body ->
                val photo = photoMap[body.photoId] ?: return@forEach
                val wDp = with(density) { body.width.toDp() }
                val hDp = with(density) { body.height.toDp() }

                Surface(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (body.x - body.width * 0.5f).roundToInt(),
                                (body.y - body.height * 0.5f).roundToInt()
                            )
                        }
                        .size(wDp, hDp)
                        .zIndex(body.zOrder.toFloat())
                        .graphicsLayer {
                            rotationZ = body.angleDeg
                        }
                        .pointerInput(body.photoId) {
                            detectTapGestures(onTap = { onOpenDetail(photo.id) })
                        }
                        .pointerInput(body.photoId) {
                            detectDragGestures(
                                onDragStart = { engine.startDrag(body.photoId) },
                                onDragEnd = { engine.endDrag(body.photoId) },
                                onDragCancel = { engine.endDrag(body.photoId) }
                            ) { change, dragAmount ->
                                change.consume()
                                engine.dragBy(body.photoId, dragAmount.x, dragAmount.y)
                            }
                        }
                        .testTag("physics_card_${photo.id}"),
                    shape = RoundedCornerShape(10.dp),
                    color = MuseumCanvas,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MuseumHairline)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(5.dp)) {
                        AsyncImage(
                            model = mediaManager.resolveCoilModel(photo),
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = photo.title.substringBefore("·").trim(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MuseumInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
