package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ArtFilter
import com.example.data.model.CURATED_FX_PRESETS
import com.example.data.model.EffectConfig
import com.example.data.model.FrameStyle
import com.example.data.model.FxPreset
import com.example.data.model.ParticleStyle
import com.example.data.model.StickerType
import com.example.ui.components.StickerBadgeGraphic
import com.example.ui.theme.ScreeningBorder
import com.example.ui.theme.ScreeningChampagne
import com.example.ui.theme.ScreeningIvory
import com.example.ui.theme.ScreeningMuted
import com.example.ui.theme.ScreeningObsidian
import com.example.ui.theme.ScreeningSurfaceHigh

@Composable
fun StudioFilterTab(
    config: EffectConfig,
    isSaving: Boolean,
    onApplyPreset: (FxPreset) -> Unit,
    onBatchSavePresets: () -> Unit,
    onSelectFilter: (ArtFilter) -> Unit,
    onIntensityChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(205.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "大师特效模板（滤镜 + 光效 + 装裱）",
                style = MaterialTheme.typography.labelSmall,
                color = ScreeningChampagne
            )
            Surface(
                onClick = onBatchSavePresets,
                enabled = !isSaving,
                shape = RoundedCornerShape(50),
                color = ScreeningChampagne.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, ScreeningChampagne),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("studio_batch_save_btn")
            ) {
                Text(
                    text = "一键连存6套",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScreeningChampagne,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CURATED_FX_PRESETS, key = { it.id }) { preset ->
                Surface(
                    onClick = { onApplyPreset(preset) },
                    shape = RoundedCornerShape(14.dp),
                    color = ScreeningSurfaceHigh,
                    border = BorderStroke(1.dp, ScreeningChampagne.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("fx_preset_${preset.id}")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(preset.name, style = MaterialTheme.typography.labelLarge, color = ScreeningChampagne)
                        Text(preset.subtitle, style = MaterialTheme.typography.labelSmall, color = ScreeningMuted)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "14 种实时像素 & 艺术滤镜",
            style = MaterialTheme.typography.labelSmall,
            color = ScreeningMuted,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ArtFilter.entries) { filter ->
                val selected = config.filter == filter
                Surface(
                    onClick = { onSelectFilter(filter) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) ScreeningChampagne else ScreeningSurfaceHigh,
                    border = BorderStroke(1.dp, if (selected) ScreeningChampagne else ScreeningBorder),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("filter_chip_${filter.name}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = filter.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) ScreeningObsidian else ScreeningIvory
                        )
                        Text(
                            text = filter.tagCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) ScreeningObsidian.copy(alpha = 0.75f) else ScreeningMuted
                        )
                    }
                }
            }
        }
        StudioSliderRow("滤镜强度", config.filterIntensity, 0f..1f, onIntensityChange)
    }
}

@Composable
fun StudioParallaxParticleTab(
    config: EffectConfig,
    onParallaxChange: (Float) -> Unit,
    onToggleHologram: () -> Unit,
    onSelectParticle: (ParticleStyle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(195.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("全息镭射幻彩镀膜", style = MaterialTheme.typography.labelLarge, color = ScreeningIvory)
                Text("随裸眼 3D 角度折射流光虹彩", style = MaterialTheme.typography.labelSmall, color = ScreeningMuted)
            }
            Switch(
                checked = config.hologramFoil,
                onCheckedChange = { onToggleHologram() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ScreeningObsidian,
                    checkedTrackColor = ScreeningChampagne
                ),
                modifier = Modifier.testTag("studio_hologram_switch")
            )
        }

        Text(
            text = "动态光斑与粒子氛围",
            style = MaterialTheme.typography.labelSmall,
            color = ScreeningMuted,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ParticleStyle.entries) { style ->
                val selected = config.particleStyle == style
                Surface(
                    onClick = { onSelectParticle(style) },
                    shape = RoundedCornerShape(50),
                    color = if (selected) ScreeningChampagne else ScreeningSurfaceHigh,
                    border = BorderStroke(1.dp, if (selected) ScreeningChampagne else ScreeningBorder),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(
                        text = style.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) ScreeningObsidian else ScreeningIvory,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
        }
        StudioSliderRow("裸眼 3D 景深", config.parallaxDepth, 0f..1f, onParallaxChange)
    }
}

@Composable
fun StudioFrameStickerTab(
    config: EffectConfig,
    onSelectFrame: (FrameStyle) -> Unit,
    onAddSticker: (StickerType) -> Unit,
    onTextChange: (String) -> Unit,
    onToggleGlow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(195.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "美术馆装裱画框",
            style = MaterialTheme.typography.labelSmall,
            color = ScreeningMuted,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FrameStyle.entries) { frame ->
                val selected = config.frameStyle == frame
                Surface(
                    onClick = { onSelectFrame(frame) },
                    shape = RoundedCornerShape(50),
                    color = if (selected) ScreeningChampagne else ScreeningSurfaceHigh,
                    border = BorderStroke(1.dp, if (selected) ScreeningChampagne else ScreeningBorder),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(
                        text = frame.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) ScreeningObsidian else ScreeningIvory,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "点击添加立体贴纸（可在画面中自由拖拽）",
            style = MaterialTheme.typography.labelSmall,
            color = ScreeningMuted,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(StickerType.entries) { type ->
                Surface(
                    onClick = { onAddSticker(type) },
                    shape = RoundedCornerShape(14.dp),
                    color = ScreeningSurfaceHigh,
                    border = BorderStroke(1.dp, ScreeningBorder),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StickerBadgeGraphic(type = type, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = type.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = ScreeningIvory
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = config.luminousText,
                onValueChange = onTextChange,
                placeholder = { Text("输入光影海报题字…", color = ScreeningMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ScreeningIvory,
                    unfocusedTextColor = ScreeningIvory,
                    focusedBorderColor = ScreeningChampagne,
                    unfocusedBorderColor = ScreeningBorder
                ),
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("光晕", style = MaterialTheme.typography.labelSmall, color = ScreeningMuted)
                Switch(
                    checked = config.luminousTextGlow,
                    onCheckedChange = { onToggleGlow() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ScreeningObsidian,
                        checkedTrackColor = ScreeningChampagne
                    )
                )
            }
        }
    }
}

@Composable
fun StudioFineTuneTab(
    config: EffectConfig,
    onGrainChange: (Float) -> Unit,
    onVignetteChange: (Float) -> Unit,
    onWarmthChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(195.dp)
            .verticalScroll(rememberScrollState())
    ) {
        StudioSliderRow("胶片颗粒", config.grainAmount, 0f..1f, onGrainChange)
        StudioSliderRow("暗角氛围", config.vignetteAmount, 0f..1f, onVignetteChange)
        StudioSliderRow("色温冷暖", config.warmth, -1f..1f, onWarmthChange)
        StudioSliderRow("对比层次", config.contrast, 0.65f..1.55f, onContrastChange)
    }
}

@Composable
fun StudioSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = ScreeningIvory,
            modifier = Modifier.width(86.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = ScreeningChampagne,
                activeTrackColor = ScreeningChampagne,
                inactiveTrackColor = ScreeningSurfaceHigh
            ),
            modifier = Modifier.weight(1f)
        )
    }
}
