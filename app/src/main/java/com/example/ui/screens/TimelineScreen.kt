package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.LocalMediaManager
import com.example.data.model.PhotoEntity
import com.example.ui.theme.MuseumBrassGold
import com.example.ui.theme.MuseumCanvas
import com.example.ui.theme.MuseumFilmRed
import com.example.ui.theme.MuseumHairline
import com.example.ui.theme.MuseumInk
import com.example.ui.theme.MuseumPaper
import com.example.ui.theme.MuseumSlate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(
    photos: List<PhotoEntity>,
    columns: Int,
    selectedIds: Set<Long>,
    mediaManager: LocalMediaManager,
    onChangeColumns: (Int) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onCreateCollageFromSelection: () -> Unit,
    onPlayReelFromSelection: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onToggleFavorite: (PhotoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var pinchAccumulator by remember { mutableFloatStateOf(1f) }
    val selectionActive = selectedIds.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(columns) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f) {
                        pinchAccumulator *= zoom
                        if (pinchAccumulator > 1.24f && columns > 2) {
                            onChangeColumns(columns - 1)
                            pinchAccumulator = 1f
                        } else if (pinchAccumulator < 0.80f && columns < 4) {
                            onChangeColumns(columns + 1)
                            pinchAccumulator = 1f
                        }
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "双指捏合缩放 · 长按多选拼图",
                    style = MaterialTheme.typography.labelSmall,
                    color = MuseumSlate
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2 to "大图", 3 to "中列", 4 to "胶片").forEach { (colCount, label) ->
                        val active = columns == colCount
                        Surface(
                            onClick = { onChangeColumns(colCount) },
                            shape = RoundedCornerShape(50),
                            color = if (active) MuseumInk else MuseumCanvas,
                            border = BorderStroke(1.dp, if (active) MuseumInk else MuseumHairline),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("timeline_scale_$colCount")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) MuseumPaper else MuseumInk,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(if (columns == 2) 14.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (columns == 2) 16.dp else 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(columns) }) {
                    Text(
                        text = "馆藏精选时间轴 · 2026 EXHIBITION",
                        style = MaterialTheme.typography.titleMedium,
                        color = MuseumInk,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                items(photos, key = { it.id }) { photo ->
                    val isSelected = selectedIds.contains(photo.id)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .testTag("timeline_card_${photo.id}")
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.5.dp, MuseumBrassGold, RoundedCornerShape(if (columns == 2) 16.dp else 10.dp))
                                } else Modifier
                            )
                            .combinedClickable(
                                onClick = {
                                    if (selectionActive) onToggleSelect(photo.id) else onOpenDetail(photo.id)
                                },
                                onLongClick = { onToggleSelect(photo.id) }
                            ),
                        shape = RoundedCornerShape(if (columns == 2) 16.dp else 10.dp),
                        color = MuseumCanvas,
                        shadowElevation = if (columns == 2) 5.dp else 2.dp,
                        border = BorderStroke(1.dp, MuseumHairline)
                    ) {
                        Column(modifier = Modifier.padding(if (columns == 2) 8.dp else 4.dp)) {
                            Box {
                                AsyncImage(
                                    model = mediaManager.resolveCoilModel(photo),
                                    contentDescription = photo.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(if (columns == 2) 0.82f else 1f)
                                        .clip(RoundedCornerShape(if (columns == 2) 10.dp else 6.dp))
                                )

                                if (selectionActive) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = "选择",
                                        tint = if (isSelected) MuseumBrassGold else MuseumCanvas,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(22.dp)
                                            .background(MuseumInk.copy(alpha = 0.55f), CircleShape)
                                    )
                                } else if (columns == 2) {
                                    IconButton(
                                        onClick = { onToggleFavorite(photo) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .minimumInteractiveComponentSize()
                                            .background(MuseumCanvas.copy(alpha = 0.80f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (photo.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = "收藏",
                                            tint = if (photo.isFavorite) MuseumFilmRed else MuseumInk,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            if (columns == 2) {
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
                }
            }
        }

        // Floating Multi-Select Action Bar
        AnimatedVisibility(
            visible = selectionActive,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 96.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MuseumInk,
                shadowElevation = 14.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "取消选择",
                            tint = MuseumPaper
                        )
                    }
                    Text(
                        text = "已选 ${selectedIds.size} 项",
                        style = MaterialTheme.typography.labelLarge,
                        color = MuseumPaper
                    )
                    Surface(
                        onClick = onCreateCollageFromSelection,
                        shape = RoundedCornerShape(50),
                        color = MuseumBrassGold,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DashboardCustomize,
                                contentDescription = "制作拼图",
                                tint = MuseumInk,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("拼图", style = MaterialTheme.typography.labelLarge, color = MuseumInk)
                        }
                    }
                    Surface(
                        onClick = onPlayReelFromSelection,
                        shape = RoundedCornerShape(50),
                        color = MuseumPaper,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesomeMotion,
                                contentDescription = "播放影集",
                                tint = MuseumInk,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("影集", style = MaterialTheme.typography.labelLarge, color = MuseumInk)
                        }
                    }
                }
            }
        }
    }
}
