package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PhotoSourceType {
    CURATED_DRAWABLE,
    LOCAL_FILE
}

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val catalogCode: String,
    val exifSummary: String,
    val sourceType: PhotoSourceType,
    val drawableName: String? = null,
    val filePath: String? = null,
    val aspectRatio: Float = 0.75f,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val effectTag: String? = null
)

enum class GalleryMode(val label: String, val subtitle: String) {
    CORRIDOR_3D("3D走廊", "ARCADE 3D"),
    CUBE_PARALLAX("3D魔方", "CUBE & RING"),
    GLOBE_3D("3D球幕", "SPHERE & MANDALA"),
    PHYSICS_PILE("重力堆", "PHYSICS ARENA"),
    TIMELINE("时间轴", "CHRONOLOGY")
}

enum class ExhibitionFilter(val label: String) {
    ALL("全部馆藏"),
    FX_ONLY("特效创作"),
    FAVORITES("私藏精选")
}
