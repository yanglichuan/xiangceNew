package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Camera
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.data.local.LocalMediaManager
import com.example.data.model.PhotoEntity
import com.example.ui.components.rememberTiltOffset
import com.example.ui.theme.MuseumBrassGold
import com.example.ui.theme.MuseumCanvas
import com.example.ui.theme.MuseumHairline
import com.example.ui.theme.MuseumInk
import com.example.ui.theme.MuseumPaper
import com.example.ui.theme.MuseumSlate
import com.example.ui.theme.MuseumWarmStone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private data class CubeFaceSpec(
    val index: Int,
    val baseRotY: Float,
    val baseRotX: Float,
    val normalZ: Float,
    val photo: PhotoEntity
)

@Composable
fun CubeParallaxScreen(
    photos: List<PhotoEntity>,
    mediaManager: LocalMediaManager,
    onOpenDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (photos.isEmpty()) return
    val tilt by rememberTiltOffset()
    var showRingInsteadOfCube by remember { mutableStateOf(false) }

    val leftColumn = remember(photos) { photos.filterIndexed { index, _ -> index % 2 == 0 } }
    val rightColumn = remember(photos) { photos.filterIndexed { index, _ -> index % 2 == 1 } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "cube_showcase") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(false to "3D 悬浮魔方", true to "3D 轨道星环").forEach { (isRing, label) ->
                            val selected = showRingInsteadOfCube == isRing
                            Surface(
                                onClick = { showRingInsteadOfCube = isRing },
                                shape = RoundedCornerShape(50),
                                color = if (selected) MuseumInk else MuseumCanvas,
                                border = BorderStroke(1.dp, if (selected) MuseumInk else MuseumHairline)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MuseumPaper else MuseumSlate,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.Filled.RotateRight,
                        contentDescription = "3D 旋转提示",
                        tint = MuseumBrassGold,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!showRingInsteadOfCube) {
                    Interactive3DCubeShowcase(
                        photos = photos,
                        mediaManager = mediaManager,
                        onFaceClick = onOpenDetail
                    )
                } else {
                    Interactive3DOrbitRingShowcase(
                        photos = photos,
                        mediaManager = mediaManager,
                        onCardClick = onOpenDetail
                    )
                }
            }
        }

        item(key = "waterfall_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "倾斜视差瀑布流 · PARALLAX WALL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MuseumSlate
                )
                Text(
                    text = "${photos.size} 件作品",
                    style = MaterialTheme.typography.labelSmall,
                    color = MuseumBrassGold
                )
            }
        }

        item(key = "waterfall_columns") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    leftColumn.forEachIndexed { idx, photo ->
                        ParallaxWaterfallCard(
                            photo = photo,
                            mediaManager = mediaManager,
                            tiltX = tilt.x,
                            tiltY = tilt.y,
                            intrinsicTilt = if (idx % 2 == 0) -3.5f else 3.5f,
                            onClick = { onOpenDetail(photo.id) }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rightColumn.forEachIndexed { idx, photo ->
                        ParallaxWaterfallCard(
                            photo = photo,
                            mediaManager = mediaManager,
                            tiltX = tilt.x,
                            tiltY = tilt.y,
                            intrinsicTilt = if (idx % 2 == 0) 3.5f else -3.5f,
                            onClick = { onOpenDetail(photo.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Interactive3DOrbitRingShowcase(
    photos: List<PhotoEntity>,
    mediaManager: LocalMediaManager,
    onCardClick: (Long) -> Unit
) {
    val ringPhotos = remember(photos) { photos.take(8) }
    var orbitAngle by remember { mutableFloatStateOf(0f) }
    var tiltAngle by remember { mutableFloatStateOf(12f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(isDragging) {
        if (!isDragging) {
            var lastNanos = 0L
            while (true) {
                withFrameNanos { nanos ->
                    if (lastNanos != 0L) {
                        val dt = (nanos - lastNanos) / 1_000_000_000f
                        orbitAngle = (orbitAngle + dt * 24f) % 360f
                    }
                    lastNanos = nanos
                }
            }
        }
    }

    val density = LocalDensity.current.density

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(276.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E1D1B), Color(0xFF101012))
                )
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, dragAmount ->
                    change.consume()
                    orbitAngle += dragAmount.x * 0.45f
                    tiltAngle = (tiltAngle - dragAmount.y * 0.25f).coerceIn(-25f, 35f)
                }
            }
            .testTag("orbit_ring_3d_canvas"),
        contentAlignment = Alignment.Center
    ) {
        val radiusX = with(LocalDensity.current) { (maxWidth * 0.34f).toPx() }
        val radiusY = with(LocalDensity.current) { 36.dp.toPx() } * (tiltAngle / 18f)

        // Glowing central brass ring guide
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(MuseumBrassGold.copy(alpha = 0.28f), Color.Transparent)
                    )
                )
        )

        ringPhotos.forEachIndexed { idx, photo ->
            val thetaDeg = orbitAngle + idx * (360f / ringPhotos.size)
            val thetaRad = thetaDeg * (PI / 180.0)
            val sinT = sin(thetaRad).toFloat()
            val cosT = cos(thetaRad).toFloat() // 1 = front, -1 = back

            val xOff = sinT * radiusX
            val yOff = cosT * radiusY
            val depthScale = 0.56f + (cosT + 1f) * 0.28f
            val cardAlpha = (0.36f + (cosT + 1f) * 0.32f).coerceIn(0.32f, 1f)

            Surface(
                modifier = Modifier
                    .offset { IntOffset(xOff.roundToInt(), yOff.roundToInt()) }
                    .size(96.dp, 124.dp)
                    .zIndex((cosT + 1f) * 10f)
                    .graphicsLayer {
                        cameraDistance = 14f * density
                        scaleX = depthScale
                        scaleY = depthScale
                        rotationY = -sinT * 38f
                        alpha = cardAlpha
                    }
                    .clickable { onCardClick(photo.id) },
                shape = RoundedCornerShape(10.dp),
                color = MuseumCanvas,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, MuseumBrassGold.copy(alpha = 0.6f))
            ) {
                AsyncImage(
                    model = mediaManager.resolveCoilModel(photo),
                    contentDescription = photo.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
private fun Interactive3DCubeShowcase(
    photos: List<PhotoEntity>,
    mediaManager: LocalMediaManager,
    onFaceClick: (Long) -> Unit
) {
    var rotY by remember { mutableFloatStateOf(-26f) }
    var rotX by remember { mutableFloatStateOf(-14f) }
    var isDragging by remember { mutableStateOf(false) }

    val cubePhotos = remember(photos) {
        List(6) { idx -> photos[idx % photos.size] }
    }

    var faceBitmaps by remember { mutableStateOf<Map<Long, Bitmap>>(emptyMap()) }
    LaunchedEffect(cubePhotos) {
        val loaded = mutableMapOf<Long, Bitmap>()
        cubePhotos.distinctBy { it.id }.forEach { photo ->
            val raw = mediaManager.decodeBitmap(photo, maxDimension = 480)
            if (raw != null) {
                val cropped = withContext(Dispatchers.Default) { centerCropSquare(raw) }
                if (cropped != raw) raw.recycle()
                loaded[photo.id] = cropped
            }
        }
        faceBitmaps = loaded
    }

    LaunchedEffect(isDragging) {
        if (!isDragging) {
            var lastNanos = 0L
            while (true) {
                withFrameNanos { nanos ->
                    if (lastNanos != 0L) {
                        val dt = (nanos - lastNanos) / 1_000_000_000f
                        rotY = (rotY + dt * 18f) % 360f
                    }
                    lastNanos = nanos
                }
            }
        }
    }

    val radY = Math.toRadians(rotY.toDouble())
    val radX = Math.toRadians(rotX.toDouble())
    val cosY = cos(radY).toFloat()
    val sinY = sin(radY).toFloat()
    val cosX = cos(radX).toFloat()
    val sinX = sin(radX).toFloat()

    fun transformedZ(nx: Float, ny: Float, nz: Float): Float {
        val z1 = -nx * sinY + nz * cosY
        return ny * sinX + z1 * cosX
    }

    val faces = listOf(
        CubeFaceSpec(0, 0f, 0f, transformedZ(0f, 0f, 1f), cubePhotos[0]),
        CubeFaceSpec(1, 180f, 0f, transformedZ(0f, 0f, -1f), cubePhotos[1]),
        CubeFaceSpec(2, 90f, 0f, transformedZ(1f, 0f, 0f), cubePhotos[2]),
        CubeFaceSpec(3, -90f, 0f, transformedZ(-1f, 0f, 0f), cubePhotos[3]),
        CubeFaceSpec(4, 0f, 90f, transformedZ(0f, -1f, 0f), cubePhotos[4]),
        CubeFaceSpec(5, 0f, -90f, transformedZ(0f, 1f, 0f), cubePhotos[5])
    ).sortedBy { it.normalZ }

    val frontPhoto = faces.last().photo
    val camera = remember { Camera() }
    val matrix = remember { Matrix() }
    val framePaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE } }
    val bmpPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val shadePaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(268.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(MuseumWarmStone, Color(0xFFE7E1D6))
                    )
                )
                .pointerInput(frontPhoto.id) {
                    detectTapGestures(onTap = { onFaceClick(frontPhoto.id) })
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false }
                    ) { change, dragAmount ->
                        change.consume()
                        rotY += dragAmount.x * 0.48f
                        rotX = (rotX + dragAmount.y * 0.42f).coerceIn(-60f, 60f)
                    }
                }
                .testTag("cube_3d_canvas"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .width(146.dp)
                    .height(14.dp)
                    .clip(CircleShape)
                    .background(MuseumInk.copy(alpha = 0.14f))
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cubeSide = min(size.width, size.height) * 0.56f
                val half = cubeSide / 2f
                val cx = size.width / 2f
                val cy = size.height / 2f - 8f
                val borderPad = cubeSide * 0.045f

                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    faces.filter { it.normalZ > -0.1f }.forEach { face ->
                        camera.save()
                        camera.setLocation(0f, 0f, -14f)
                        camera.rotateX(rotX)
                        camera.rotateY(rotY)
                        camera.rotateY(face.baseRotY)
                        camera.rotateX(face.baseRotX)
                        camera.translate(0f, 0f, -half)
                        camera.getMatrix(matrix)
                        camera.restore()

                        matrix.preTranslate(-half, -half)
                        matrix.postTranslate(cx, cy)

                        native.save()
                        native.concat(matrix)

                        native.drawRoundRect(
                            RectF(0f, 0f, cubeSide, cubeSide),
                            14f, 14f,
                            framePaint
                        )

                        val bmp = faceBitmaps[face.photo.id]
                        if (bmp != null) {
                            native.drawBitmap(
                                bmp,
                                Rect(0, 0, bmp.width, bmp.height),
                                RectF(borderPad, borderPad, cubeSide - borderPad, cubeSide - borderPad),
                                bmpPaint
                            )
                        }

                        val shadeAlpha = ((1f - face.normalZ.coerceIn(0f, 1f)) * 90f).toInt().coerceIn(0, 110)
                        if (shadeAlpha > 4) {
                            shadePaint.color = android.graphics.Color.argb(shadeAlpha, 20, 20, 19)
                            native.drawRoundRect(
                                RectF(0f, 0f, cubeSide, cubeSide),
                                14f, 14f,
                                shadePaint
                            )
                        }
                        native.restore()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击魔方正面打开：${frontPhoto.title}",
            style = MaterialTheme.typography.bodyMedium,
            color = MuseumInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun centerCropSquare(src: Bitmap): Bitmap {
    val size = min(src.width, src.height)
    val x = (src.width - size) / 2
    val y = (src.height - size) / 2
    return Bitmap.createBitmap(src, x, y, size, size)
}

@Composable
private fun ParallaxWaterfallCard(
    photo: PhotoEntity,
    mediaManager: LocalMediaManager,
    tiltX: Float,
    tiltY: Float,
    intrinsicTilt: Float,
    onClick: () -> Unit
) {
    val density = LocalDensity.current.density
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                cameraDistance = 12f * density
                rotationY = intrinsicTilt + tiltX * 9f
                rotationX = -tiltY * 7f
                translationX = tiltX * 6f
            }
            .testTag("waterfall_card_${photo.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MuseumCanvas,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MuseumHairline)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = mediaManager.resolveCoilModel(photo),
                contentDescription = photo.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(photo.aspectRatio.coerceIn(0.62f, 1.35f))
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = photo.catalogCode,
                style = MaterialTheme.typography.labelSmall,
                color = MuseumBrassGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = photo.title,
                style = MaterialTheme.typography.titleMedium,
                color = MuseumInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
