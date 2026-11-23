package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.PhotoEntity
import com.example.data.model.PhotoSourceType

class PhotoConverters {
    @TypeConverter
    fun fromSourceType(value: PhotoSourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): PhotoSourceType =
        PhotoSourceType.entries.firstOrNull { it.name == value } ?: PhotoSourceType.CURATED_DRAWABLE
}

@Database(entities = [PhotoEntity::class], version = 1, exportSchema = false)
@TypeConverters(PhotoConverters::class)
abstract class PhotoDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao

    companion object {
        @Volatile
        private var INSTANCE: PhotoDatabase? = null

        fun getInstance(context: Context): PhotoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PhotoDatabase::class.java,
                    "atelier_gallery.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
