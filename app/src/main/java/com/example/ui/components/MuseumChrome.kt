package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewInAr
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GalleryMode
import com.example.ui.theme.MuseumBrassGold
import com.example.ui.theme.MuseumCanvas
import com.example.ui.theme.MuseumFilmRed
import com.example.ui.theme.MuseumHairline
import com.example.ui.theme.MuseumInk
import com.example.ui.theme.MuseumPaper
import com.example.ui.theme.MuseumSlate
import com.example.ui.theme.MuseumWarmStone

@Composable
fun MuseumHeader(
    photoCount: Int,
    activeMode: GalleryMode,
    favoritesOnly: Boolean,
    onToggleFavorites: () -> Unit,
    onImportClick: () -> Unit,
    onMotionReelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ATELIER · ${activeMode.subtitle} · ${photoCount.toString().padStart(2, '0')} WORKS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MuseumSlate
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Atelier 相册",
                    style = MaterialTheme.typography.displayMedium,
                    color = MuseumInk
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (favoritesOnly) MuseumFilmRed.copy(alpha = 0.12f) else MuseumCanvas,
                    border = BorderStroke(1.dp, if (favoritesOnly) MuseumFilmRed else MuseumHairline)
                ) {
                    IconButton(
                        onClick = onToggleFavorites,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("favorites_filter_button")
                    ) {
                        Icon(
                            imageVector = if (favoritesOnly) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "仅看收藏",
                            tint = if (favoritesOnly) MuseumFilmRed else MuseumInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MuseumCanvas,
                    border = BorderStroke(1.dp, MuseumHairline)
                ) {
                    IconButton(
                        onClick = onMotionReelClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("motion_reel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesomeMotion,
                            contentDescription = stringResource(R.string.cd_motion_reel),
                            tint = MuseumBrassGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    onClick = onImportClick,
                    shape = RoundedCornerShape(50),
                    color = MuseumInk,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("import_photos_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = stringResource(R.string.cd_import_photos),
                            tint = MuseumPaper,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "导入",
                            style = MaterialTheme.typography.labelLarge,
                            color = MuseumPaper
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModeSwitcherBar(
    activeMode: GalleryMode,
    onModeSelected: (GalleryMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MuseumCanvas.copy(alpha = 0.94f),
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MuseumHairline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModePillItem(
                    mode = GalleryMode.CORRIDOR_3D,
                    icon = Icons.Filled.ViewCarousel,
                    selected = activeMode == GalleryMode.CORRIDOR_3D,
                    onClick = { onModeSelected(GalleryMode.CORRIDOR_3D) },
                    modifier = Modifier.weight(1f).testTag("mode_corridor_tab")
                )
                ModePillItem(
                    mode = GalleryMode.CUBE_PARALLAX,
                    icon = Icons.Filled.ViewInAr,
                    selected = activeMode == GalleryMode.CUBE_PARALLAX,
                    onClick = { onModeSelected(GalleryMode.CUBE_PARALLAX) },
                    modifier = Modifier.weight(1f).testTag("mode_cube_tab")
                )
                ModePillItem(
                    mode = GalleryMode.PHYSICS_PILE,
                    icon = Icons.Filled.BubbleChart,
                    selected = activeMode == GalleryMode.PHYSICS_PILE,
                    onClick = { onModeSelected(GalleryMode.PHYSICS_PILE) },
                    modifier = Modifier.weight(1f).testTag("mode_physics_tab")
                )
                ModePillItem(
                    mode = GalleryMode.TIMELINE,
                    icon = Icons.Filled.GridView,
                    selected = activeMode == GalleryMode.TIMELINE,
                    onClick = { onModeSelected(GalleryMode.TIMELINE) },
                    modifier = Modifier.weight(1f).testTag("mode_timeline_tab")
                )
            }
        }
    }
}

@Composable
private fun ModePillItem(
    mode: GalleryMode,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MuseumInk else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pill_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MuseumPaper else MuseumSlate,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pill_fg"
    )

    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(26.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = mode.label,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = mode.label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
