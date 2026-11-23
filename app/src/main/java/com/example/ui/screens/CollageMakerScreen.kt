package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.CollageLayout
import com.example.data.model.PhotoEntity
import com.example.ui.theme.AtelierGalleryTheme
import com.example.ui.theme.MuseumBrassGold
import com.example.ui.theme.MuseumInk
import com.example.ui.theme.MuseumPaper
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.ScreeningBorder
import com.example.ui.theme.ScreeningChampagne
import com.example.ui.theme.ScreeningIvory
import com.example.ui.theme.ScreeningMuted
import com.example.ui.theme.ScreeningObsidian
import com.example.ui.theme.ScreeningSurface
import com.example.ui.theme.ScreeningSurfaceHigh
import com.example.ui.viewmodel.GalleryViewModel

@Composable
fun CollageMakerScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    onCreatedArtwork: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allPhotos = uiState.allPhotos
    if (allPhotos.isEmpty()) return

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var layout by remember { mutableStateOf(CollageLayout.GRID_2X2) }
    var darkPosterTheme by remember { mutableStateOf(false) }
    var posterTitle by remember { mutableStateOf("ATELIER 策展海报") }

    val pickedPhotos = remember(allPhotos, uiState.selectedIds) {
        val preselected = if (uiState.selectedIds.isNotEmpty()) {
            allPhotos.filter { uiState.selectedIds.contains(it.id) }
        } else {
            allPhotos.take(4)
        }
        mutableStateListOf<PhotoEntity>().apply {
            addAll(preselected.ifEmpty { allPhotos.take(4) })
            while (size < 4) {
                add(allPhotos[size % allPhotos.size])
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveNotice.collect { notice ->
            val result = snackbarHostState.showSnackbar(
                message = notice.message,
                actionLabel = "查看海报",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                onCreatedArtwork(notice.savedPhotoId)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.shareUriEvent.collect { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "分享 Atelier 拼图海报"))
        }
    }

    AtelierGalleryTheme(darkScreeningRoom = true) {
        Scaffold(
            containerColor = ScreeningObsidian,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreeningObsidian)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("collage_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = ScreeningIvory)
                    }
                    Column {
                        Text("POSTER STUDIO", style = MaterialTheme.typography.labelSmall, color = ScreeningChampagne)
                        Text("艺术拼图海报", style = MaterialTheme.typography.titleMedium, color = ScreeningIvory)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.shareCollagePoster(
                                selectedPhotos = pickedPhotos.take(layout.requiredCount),
                                layout = layout,
                                title = posterTitle,
                                darkTheme = darkPosterTheme
                            )
                        },
                        enabled = !uiState.isSavingCollage,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("collage_share_btn")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "分享海报", tint = ScreeningIvory)
                    }

                    Surface(
                        onClick = {
                            viewModel.createAndSaveCollage(
                                selectedPhotos = pickedPhotos.take(layout.requiredCount),
                                layout = layout,
                                title = posterTitle,
                                darkTheme = darkPosterTheme
                            )
                        },
                        enabled = !uiState.isSavingCollage,
                        shape = RoundedCornerShape(50),
                        color = ScreeningChampagne,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("collage_save_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.isSavingCollage) {
                                CircularProgressIndicator(
                                    color = ScreeningObsidian,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.Check, contentDescription = "保存海报", tint = ScreeningObsidian, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("保存海报", style = MaterialTheme.typography.labelLarge, color = ScreeningObsidian)
                        }
                    }
                }
            }

            // Live Poster Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                val bg = if (darkPosterTheme) Color(0xFF141416) else MuseumPaper
                val fg = if (darkPosterTheme) ScreeningIvory else MuseumInk

                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .aspectRatio(0.75f),
                    color = bg,
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 20.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ATELIER CURATED POSTER · ${layout.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MuseumBrassGold
                        )
                        Text(
                            text = posterTitle.ifBlank { "EXHIBITION SERIES" },
                            fontFamily = PlayfairDisplayFontFamily,
                            fontSize = 16.sp,
                            color = fg,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (layout) {
                                CollageLayout.TRIPTYCH -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        repeat(3) { idx ->
                                            AsyncImage(
                                                model = viewModel.mediaManager.resolveCoilModel(pickedPhotos[idx]),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(4.dp))
                                            )
                                        }
                                    }
                                }
                                CollageLayout.GRID_2X2 -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            repeat(2) { idx ->
                                                AsyncImage(
                                                    model = viewModel.mediaManager.resolveCoilModel(pickedPhotos[idx]),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp))
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            repeat(2) { idx ->
                                                AsyncImage(
                                                    model = viewModel.mediaManager.resolveCoilModel(pickedPhotos[idx + 2]),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp))
                                                )
                                            }
                                        }
                                    }
                                }
                                CollageLayout.FILM_STRIP -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF141416))
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        repeat(3) { idx ->
                                            AsyncImage(
                                                model = viewModel.mediaManager.resolveCoilModel(pickedPhotos[idx]),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(3.dp))
                                            )
                                        }
                                    }
                                }
                                CollageLayout.POLAROID_STACK -> {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth(0.76f)
                                                .aspectRatio(0.9f)
                                                .align(Alignment.TopStart)
                                                .graphicsLayer { rotationZ = -5f },
                                            color = Color.White,
                                            shadowElevation = 10.dp
                                        ) {
                                            AsyncImage(
                                                model = viewModel.mediaManager.resolveCoilModel(pickedPhotos[0]),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 26.dp)
                                            )
                                        }
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth(0.76f)
                                                .aspectRatio(0.9f)
                                                .align(Alignment.BottomEnd)
                                                .graphicsLayer { rotationZ = 5f },
                                            color = Color.White,
                                            shadowElevation = 12.dp
                                        ) {
                                            AsyncImage(
                                                model = viewModel.mediaManager.resolveCoilModel(pickedPhotos[1]),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 26.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ARCHIVAL EDITION · ATELIER GALLERY",
                            style = MaterialTheme.typography.labelSmall,
                            color = fg.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            // Bottom Collage Controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = ScreeningSurface,
                border = BorderStroke(1.dp, ScreeningBorder)
            ) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(CollageLayout.entries) { item ->
                            val selected = layout == item
                            Surface(
                                onClick = { layout = item },
                                shape = RoundedCornerShape(50),
                                color = if (selected) ScreeningChampagne else ScreeningSurfaceHigh,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text(
                                    text = item.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) ScreeningObsidian else ScreeningIvory,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                )
                            }
                        }

                        item {
                            Surface(
                                onClick = { darkPosterTheme = !darkPosterTheme },
                                shape = RoundedCornerShape(50),
                                color = ScreeningSurfaceHigh,
                                border = BorderStroke(1.dp, ScreeningChampagne),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text(
                                    text = if (darkPosterTheme) "展纸：黑曜石" else "展纸：宣纸白",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ScreeningChampagne,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = posterTitle,
                        onValueChange = { posterTitle = it },
                        singleLine = true,
                        placeholder = { Text("输入海报标题", color = ScreeningMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ScreeningIvory,
                            unfocusedTextColor = ScreeningIvory,
                            focusedBorderColor = ScreeningChampagne,
                            unfocusedBorderColor = ScreeningBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击下方照片替换海报画格",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScreeningMuted,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allPhotos, key = { it.id }) { candidate ->
                            val included = pickedPhotos.take(layout.requiredCount).any { it.id == candidate.id }
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (included) 2.dp else 1.dp,
                                        color = if (included) ScreeningChampagne else ScreeningBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        // Rotate into slot 0..3
                                        pickedPhotos.removeAt(3)
                                        pickedPhotos.add(0, candidate)
                                    }
                            ) {
                                AsyncImage(
                                    model = viewModel.mediaManager.resolveCoilModel(candidate),
                                    contentDescription = candidate.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
