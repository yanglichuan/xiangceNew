package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlacedSticker
import com.example.data.model.StickerType
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MuseumBrassGold
import com.example.ui.theme.MuseumFilmRed
import com.example.ui.theme.MuseumInk
import com.example.ui.theme.MuseumPaper
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.ScreeningChampagne
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun InteractiveStickerItem(
    sticker: PlacedSticker,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    editable: Boolean,
    onMove: (normX: Float, normY: Float) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (canvasWidthPx <= 1f || canvasHeightPx <= 1f) return
    val stickerSize = 72.dp
    val halfPx = with(LocalDensity.current) { (stickerSize / 2).toPx() }
    val posX = (sticker.normX * canvasWidthPx - halfPx).roundToInt()
    val posY = (sticker.normY * canvasHeightPx - halfPx).roundToInt()

    Box(
        modifier = modifier
            .offset { IntOffset(posX, posY) }
            .size(stickerSize)
            .graphicsLayer {
                scaleX = sticker.scale
                scaleY = sticker.scale
                rotationZ = sticker.rotationDeg
            }
            .then(
                if (editable) {
                    Modifier.pointerInput(sticker.id, canvasWidthPx, canvasHeightPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val nx = sticker.normX + dragAmount.x / canvasWidthPx
                            val ny = sticker.normY + dragAmount.y / canvasHeightPx
                            onMove(nx, ny)
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        StickerBadgeGraphic(type = sticker.type, modifier = Modifier.size(60.dp))

        if (editable) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .background(MuseumInk.copy(alpha = 0.85f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "移除贴纸",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun StickerBadgeGraphic(
    type: StickerType,
    modifier: Modifier = Modifier
) {
    when (type) {
        StickerType.SEAL_EXHIBITION -> {
            Box(
                modifier = modifier
                    .shadow(6.dp, CircleShape)
                    .background(MuseumFilmRed, CircleShape)
                    .padding(4.dp)
                    .border(1.5.dp, MuseumPaper, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "馆藏",
                    color = MuseumPaper,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
        }
        StickerType.PRISM_STAR -> {
            Canvas(modifier = modifier) {
                val outer = size.minDimension * 0.48f
                val inner = outer * 0.34f
                val cx = size.width / 2f
                val cy = size.height / 2f
                val path = Path()
                for (i in 0 until 8) {
                    val r = if (i % 2 == 0) outer else inner
                    val a = Math.toRadians(i * 45.0 - 90.0)
                    val px = cx + (cos(a) * r).toFloat()
                    val py = cy + (sin(a) * r).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFF3B8), ScreeningChampagne),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                )
            }
        }
        StickerType.FILM_STAMP -> {
            Box(
                modifier = modifier
                    .shadow(6.dp, RoundedCornerShape(6.dp))
                    .background(MuseumInk, RoundedCornerShape(6.dp))
                    .border(1.2.dp, ScreeningChampagne, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "35MM",
                    color = MuseumPaper,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
        }
        StickerType.HEART_3D -> {
            Canvas(modifier = modifier) {
                val s = size.minDimension * 0.42f
                val cx = size.width / 2f
                val cy = size.height / 2f
                val path = Path().apply {
                    moveTo(cx, cy + s * 0.72f)
                    cubicTo(cx - s * 1.25f, cy - s * 0.05f, cx - s * 0.55f, cy - s * 0.85f, cx, cy - s * 0.22f)
                    cubicTo(cx + s * 0.55f, cy - s * 0.85f, cx + s * 1.25f, cy - s * 0.05f, cx, cy + s * 0.72f)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF7A6B), Color(0xFFB8292F))
                    )
                )
            }
        }
        StickerType.CROWN_GOLD -> {
            Canvas(modifier = modifier) {
                val r = size.minDimension * 0.38f
                val cx = size.width / 2f
                val cy = size.height / 2f
                val path = Path().apply {
                    moveTo(cx - r, cy + r * 0.5f)
                    lineTo(cx - r, cy - r * 0.4f)
                    lineTo(cx - r * 0.4f, cy + r * 0.05f)
                    lineTo(cx, cy - r * 0.65f)
                    lineTo(cx + r * 0.4f, cy + r * 0.05f)
                    lineTo(cx + r, cy - r * 0.4f)
                    lineTo(cx + r, cy + r * 0.5f)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFCE38A), MuseumBrassGold)
                    )
                )
            }
        }
        StickerType.FRAME_CORNER -> {
            Box(
                modifier = modifier
                    .shadow(4.dp, RoundedCornerShape(4.dp))
                    .background(ScreeningChampagne, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ATELIER",
                    color = MuseumInk,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlayfairDisplayFontFamily
                )
            }
        }
    }
}
