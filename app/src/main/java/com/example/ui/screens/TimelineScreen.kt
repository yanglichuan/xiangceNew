package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
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

@Composable
fun TimelineScreen(
    photos: List<PhotoEntity>,
    columns: Int,
    mediaManager: LocalMediaManager,
    onChangeColumns: (Int) -> Unit,
    onOpenDetail: (Long) -> Unit,
    onToggleFavorite: (PhotoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var pinchAccumulator by remember { mutableFloatStateOf(1f) }

    Column(
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
        // Scale Selector Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "支持双指捏合缩放 · ARCHIVAL ROLL",
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
            horizontalArrangement = Arrangement.spacedBy(if (columns == 2) 14.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (columns == 2) 16.dp else 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(columns) }) {
                Text(
                    text = "馆藏精选时间轴 · 2026 EXHIBITION",
                    style = MaterialTheme.typography.titleMedium,
                    color = MuseumInk,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(photos, key = { it.id }) { photo ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .testTag("timeline_card_${photo.id}")
                        .clickable { onOpenDetail(photo.id) },
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

                            if (columns == 2) {
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
}
