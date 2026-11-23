package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ArtFilter
import com.example.data.model.EffectConfig
import com.example.data.model.FrameStyle
import com.example.data.model.ParticleStyle
import com.example.domain.engine.ArtworkExporter
import com.example.domain.engine.EffectProcessor
import com.example.domain.engine.Physics2DEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun appNameMatchesAtelierGallery() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Atelier 相册", appName)
    }

    @Test
    fun physicsEngineKeepsCardsInsideBounds() {
        val engine = Physics2DEngine()
        val initial = engine.syncBodies(
            photoIdsAndRatios = listOf(1L to 0.75f, 2L to 1.0f, 3L to 1.33f),
            widthPx = 1000f,
            heightPx = 1400f,
            cardBaseWidthPx = 220f
        )
        assertEquals(3, initial.size)

        engine.impulseBurst()
        var bodies = initial
        repeat(45) {
            bodies = engine.step(0.016f, 0f, 980f, zeroGravity = false)
        }
        bodies.forEach { b ->
            assertTrue("x=${b.x} within arena", b.x in 0f..1000f)
            assertTrue("y=${b.y} within arena", b.y in 0f..1400f)
        }
    }

    @Test
    fun effectProcessorAndExporterProduceValidFramedArtwork() = runTest {
        val source = Bitmap.createBitmap(120, 160, Bitmap.Config.ARGB_8888)
        val config = EffectConfig(
            filter = ArtFilter.CYBER_NEON,
            filterIntensity = 0.8f,
            particleStyle = ParticleStyle.NEON_SPARKS,
            frameStyle = FrameStyle.MUSEUM_MATTE,
            luminousText = "ATELIER TEST"
        )
        val filtered = EffectProcessor.processBitmap(source, config)
        assertEquals(120, filtered.width)
        assertEquals(160, filtered.height)

        val exported = ArtworkExporter.renderMasterpiece(source, config, "测试作品")
        assertTrue("Framed width exceeds source width", exported.width > source.width)
        assertTrue("Framed height exceeds source height", exported.height > source.height)
    }
}
