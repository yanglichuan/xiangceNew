package com.example.data

import android.content.Context
import com.example.data.local.LocalMediaManager
import com.example.data.local.PhotoDatabase
import com.example.data.repository.PhotoRepository

object AtelierContainer {
    @Volatile
    private var repository: PhotoRepository? = null

    fun provideRepository(context: Context): PhotoRepository {
        return repository ?: synchronized(this) {
            repository ?: run {
                val appContext = context.applicationContext
                val db = PhotoDatabase.getInstance(appContext)
                val media = LocalMediaManager(appContext)
                PhotoRepository(db.photoDao(), media).also { repository = it }
            }
        }
    }
}
