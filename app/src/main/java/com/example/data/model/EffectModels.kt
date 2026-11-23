package com.example.data.model

enum class ArtFilter(val displayName: String, val tagCode: String) {
    ORIGINAL("原片直出", "RAW"),
    KODAK_FILM("胶片质感", "PORTRA 400"),
    NOIR_SILVER("银盐黑白", "ILFORD HP5"),
    CYBER_NEON("赛博霓虹", "CYBER 2077"),
    PIXEL_8BIT("像素艺术", "8-BIT"),
    OIL_IMPASTO("油画笔触", "IMPASTO"),
    CHROMATIC_GLITCH("色散故障", "RGB SPLIT"),
    WARM_SEPIA("复古琥珀", "VINTAGE 74"),
    HALFTONE_POP("波普网点", "POP ART"),
    NEON_SKETCH("霓虹线稿", "EDGE GLOW"),
    THERMAL_INFRARED("红外热感", "INFRARED"),
    KALEIDOSCOPE("镜像万花筒", "MIRROR 8X"),
    WATER_RIPPLE("流体水波", "LIQUID"),
    PRISM_SHATTER("棱镜碎裂", "CRYSTAL")
}

enum class ParticleStyle(val displayName: String) {
    NONE("无粒子"),
    DUST_BOKEH("浮尘光斑"),
    FILM_LEAK("胶片漏光"),
    NEON_SPARKS("星芒闪烁"),
    AURORA_PRISM("极光棱镜"),
    MATRIX_RAIN("流光雨幕"),
    SNOW_PETALS("浮雪光瓣"),
    HEARTBEAT_PULSE("霓虹脉冲")
}

enum class FrameStyle(val displayName: String) {
    NONE("无边框"),
    MUSEUM_MATTE("美术馆卡纸"),
    POLAROID("经典拍立得"),
    FILM_STRIP_35MM("35mm 胶卷"),
    GALLERY_BRASS("鎏金画框")
}

enum class StickerType(val label: String) {
    SEAL_EXHIBITION("馆藏印章"),
    PRISM_STAR("棱镜星芒"),
    FILM_STAMP("35MM 胶片标"),
    HEART_3D("立体爱心"),
    CROWN_GOLD("金箔桂冠"),
    FRAME_CORNER("展馆铭牌")
}

data class PlacedSticker(
    val id: String,
    val type: StickerType,
    val normX: Float = 0.5f,
    val normY: Float = 0.5f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f
)

data class EffectConfig(
    val filter: ArtFilter = ArtFilter.ORIGINAL,
    val filterIntensity: Float = 0.85f,
    val parallaxDepth: Float = 0.65f,
    val particleStyle: ParticleStyle = ParticleStyle.NONE,
    val frameStyle: FrameStyle = FrameStyle.NONE,
    val hologramFoil: Boolean = false,
    val grainAmount: Float = 0.15f,
    val vignetteAmount: Float = 0.25f,
    val warmth: Float = 0f,
    val contrast: Float = 1f,
    val luminousText: String = "",
    val luminousTextNormY: Float = 0.84f,
    val luminousTextGlow: Boolean = true,
    val stickers: List<PlacedSticker> = emptyList()
)

data class FxPreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val config: EffectConfig
)

val CURATED_FX_PRESETS = listOf(
    FxPreset(
        id = "cyber_holo",
        name = "赛博全息海报",
        subtitle = "霓虹 + 镭射膜",
        config = EffectConfig(
            filter = ArtFilter.CYBER_NEON,
            filterIntensity = 0.92f,
            parallaxDepth = 0.9f,
            particleStyle = ParticleStyle.NEON_SPARKS,
            frameStyle = FrameStyle.GALLERY_BRASS,
            hologramFoil = true,
            grainAmount = 0.1f,
            vignetteAmount = 0.45f,
            contrast = 1.15f,
            luminousText = "CYBER EXHIBITION"
        )
    ),
    FxPreset(
        id = "museum_gold",
        name = "美术馆典藏",
        subtitle = "胶片 + 卡纸装裱",
        config = EffectConfig(
            filter = ArtFilter.KODAK_FILM,
            filterIntensity = 0.85f,
            parallaxDepth = 0.65f,
            particleStyle = ParticleStyle.DUST_BOKEH,
            frameStyle = FrameStyle.MUSEUM_MATTE,
            grainAmount = 0.22f,
            vignetteAmount = 0.22f,
            warmth = 0.15f,
            stickers = listOf(
                PlacedSticker("preset_seal", StickerType.SEAL_EXHIBITION, 0.82f, 0.18f, 0.9f, -8f)
            )
        )
    ),
    FxPreset(
        id = "neon_blueprint",
        name = "3D 霓虹线稿",
        subtitle = "边沿发光 + 流光雨",
        config = EffectConfig(
            filter = ArtFilter.NEON_SKETCH,
            filterIntensity = 0.95f,
            parallaxDepth = 0.85f,
            particleStyle = ParticleStyle.MATRIX_RAIN,
            frameStyle = FrameStyle.FILM_STRIP_35MM,
            hologramFoil = true,
            grainAmount = 0.08f,
            vignetteAmount = 0.35f,
            luminousText = "NEON ARCHIVE"
        )
    ),
    FxPreset(
        id = "kaleido_dream",
        name = "万花筒棱镜",
        subtitle = "8倍镜像 + 极光",
        config = EffectConfig(
            filter = ArtFilter.KALEIDOSCOPE,
            filterIntensity = 0.95f,
            parallaxDepth = 0.8f,
            particleStyle = ParticleStyle.AURORA_PRISM,
            frameStyle = FrameStyle.GALLERY_BRASS,
            hologramFoil = true,
            vignetteAmount = 0.3f
        )
    ),
    FxPreset(
        id = "pop_comic",
        name = "波普艺术刊",
        subtitle = "网点版画 + 拍立得",
        config = EffectConfig(
            filter = ArtFilter.HALFTONE_POP,
            filterIntensity = 0.9f,
            parallaxDepth = 0.7f,
            particleStyle = ParticleStyle.HEARTBEAT_PULSE,
            frameStyle = FrameStyle.POLAROID,
            contrast = 1.2f,
            luminousText = "POP STUDIO"
        )
    ),
    FxPreset(
        id = "glitch_35mm",
        name = "故障胶卷",
        subtitle = "RGB色散 + 漏光",
        config = EffectConfig(
            filter = ArtFilter.CHROMATIC_GLITCH,
            filterIntensity = 0.88f,
            parallaxDepth = 0.78f,
            particleStyle = ParticleStyle.FILM_LEAK,
            frameStyle = FrameStyle.FILM_STRIP_35MM,
            grainAmount = 0.35f,
            vignetteAmount = 0.4f
        )
    )
)

enum class SlideshowTransition(val displayName: String, val code: String) {
    KEN_BURNS("Ken Burns 推拉", "CINEMA PAN"),
    CUBE_3D_FLIP("3D 魔方翻转", "CUBE ROTATE"),
    SHUTTER_CURTAIN("百叶窗光影", "SHUTTER WIPE"),
    FILM_DISSOLVE("胶片灼烧叠化", "FILM BURN"),
    PRISM_SPLIT("棱镜对开转场", "PRISM SPLIT"),
    VINYL_SPIN("黑胶旋转入场", "VINYL SPIN")
}

enum class CollageLayout(val displayName: String, val requiredCount: Int) {
    TRIPTYCH("三联画廊", 3),
    GRID_2X2("四格策展", 4),
    FILM_STRIP("胶片连拍", 3),
    POLAROID_STACK("拍立得拼贴", 2)
}
