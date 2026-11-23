package com.example.data.model

enum class ArtFilter(val displayName: String, val tagCode: String) {
    ORIGINAL("原片直出", "RAW"),
    KODAK_FILM("胶片质感", "PORTRA 400"),
    NOIR_SILVER("银盐黑白", "ILFORD HP5"),
    CYBER_NEON("赛博霓虹", "CYBER 2077"),
    PIXEL_8BIT("像素艺术", "8-BIT"),
    OIL_IMPASTO("油画笔触", "IMPASTO"),
    CHROMATIC_GLITCH("色散故障", "RGB SPLIT"),
    WARM_SEPIA("复古琥珀", "VINTAGE 74")
}

enum class ParticleStyle(val displayName: String) {
    NONE("无粒子"),
    DUST_BOKEH("浮尘光斑"),
    FILM_LEAK("胶片漏光"),
    NEON_SPARKS("星芒闪烁"),
    AURORA_PRISM("极光棱镜")
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
    val grainAmount: Float = 0.15f,
    val vignetteAmount: Float = 0.25f,
    val warmth: Float = 0f,
    val contrast: Float = 1f,
    val luminousText: String = "",
    val luminousTextNormY: Float = 0.84f,
    val luminousTextGlow: Boolean = true,
    val stickers: List<PlacedSticker> = emptyList()
)

enum class SlideshowTransition(val displayName: String, val code: String) {
    KEN_BURNS("Ken Burns 推拉", "CINEMA PAN"),
    CUBE_3D_FLIP("3D 魔方翻转", "CUBE ROTATE"),
    SHUTTER_CURTAIN("百叶窗光影", "SHUTTER WIPE"),
    FILM_DISSOLVE("胶片灼烧叠化", "FILM BURN"),
    PRISM_SPLIT("棱镜对开转场", "PRISM SPLIT")
}
