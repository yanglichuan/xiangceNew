package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ArtFilter
import com.example.data.model.CollageLayout
import com.example.data.model.EffectConfig
import com.example.data.model.FrameStyle
import com.example.data.model.ParticleStyle
import com.example.domain.engine.ArtworkExporter
import com.example.domain.engine.EffectProcessor
import com.example.domain.engine.Physics2DEngine
import com.example.domain.engine.PhysicsMode
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
    fun physicsEngineKeepsCardsInsideBoundsInGravityAndVortex() {
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
            bodies = engine.step(0.016f, 0f, 980f, mode = PhysicsMode.GRAVITY)
        }
        bodies.forEach { b ->
            assertTrue("x=${b.x} within arena", b.x in 0f..1000f)
            assertTrue("y=${b.y} within arena", b.y in 0f..1400f)
        }

        engine.setVortexAnchor(500f, 650f)
        repeat(30) {
            bodies = engine.step(0.016f, 0f, 0f, mode = PhysicsMode.VORTEX)
        }
        bodies.forEach { b ->
            assertTrue("vortex x=${b.x} within arena", b.x in 0f..1000f)
            assertTrue("vortex y=${b.y} within arena", b.y in 0f..1400f)
        }
    }

    @Test
    fun effectProcessorAndExporterProduceValidFramedAndCollageArtworks() = runTest {
        val source = Bitmap.createBitmap(120, 160, Bitmap.Config.ARGB_8888)
        val config = EffectConfig(
            filter = ArtFilter.NEON_SKETCH,
            filterIntensity = 0.85f,
            particleStyle = ParticleStyle.MATRIX_RAIN,
            hologramFoil = true,
            frameStyle = FrameStyle.MUSEUM_MATTE,
            luminousText = "ATELIER TEST"
        )
        val filtered = EffectProcessor.processBitmap(source, config)
        assertEquals(120, filtered.width)
        assertEquals(160, filtered.height)

        val kaleido = EffectProcessor.processBitmap(
            source,
            EffectConfig(filter = ArtFilter.KALEIDOSCOPE, filterIntensity = 0.9f)
        )
        assertEquals(120, kaleido.width)
        assertEquals(160, kaleido.height)

        val exported = ArtworkExporter.renderMasterpiece(source, config, "测试作品")
        assertTrue("Framed width exceeds source width", exported.width > source.width)
        assertTrue("Framed height exceeds source height", exported.height > source.height)

        val collage = ArtworkExporter.renderCollagePoster(
            bitmaps = listOf(source, source, source, source),
            layout = CollageLayout.GRID_2X2,
            title = "ATELIER POSTER",
            darkTheme = true
        )
        assertEquals(1200, collage.width)
        assertEquals(1600, collage.height)

        val still = ArtworkExporter.renderCinemaStill(
            sourceBitmap = source,
            title = "CINEMA TEST",
            catalogCode = "NO. 01",
            particleStyle = ParticleStyle.DUST_BOKEH,
            cinemaScope = true
        )
        assertEquals(1080, still.width)
        assertEquals(1440, still.height)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = com.example.data.local.LocalMediaManager(context)
        val saved = manager.saveExportedBitmap(exported, "test_fx")
        assertTrue("Saved file exists", java.io.File(saved.filePath).exists())
    }
}
