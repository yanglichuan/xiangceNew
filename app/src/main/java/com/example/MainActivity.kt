package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.CollageMakerScreen
import com.example.ui.screens.EffectStudioScreen
import com.example.ui.screens.GalleryHubScreen
import com.example.ui.screens.MotionReelScreen
import com.example.ui.screens.PhotoDetailScreen
import com.example.ui.theme.AtelierGalleryTheme
import com.example.ui.viewmodel.GalleryViewModel

private const val ROUTE_GALLERY = "gallery"
private const val ROUTE_DETAIL = "detail/{photoId}"
private const val ROUTE_STUDIO = "studio/{photoId}"
private const val ROUTE_COLLAGE = "collage"
private const val ROUTE_REEL = "reel"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AtelierGalleryTheme {
                AtelierAppNavigation()
            }
        }
    }
}

@Composable
fun AtelierAppNavigation(
    galleryViewModel: GalleryViewModel = viewModel()
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_GALLERY
    ) {
        composable(ROUTE_GALLERY) {
            GalleryHubScreen(
                viewModel = galleryViewModel,
                onOpenDetail = { photoId ->
                    navController.navigate("detail/$photoId")
                },
                onOpenStudio = { photoId ->
                    navController.navigate("studio/$photoId")
                },
                onOpenCollage = {
                    navController.navigate(ROUTE_COLLAGE)
                },
                onOpenMotionReel = {
                    navController.navigate(ROUTE_REEL)
                }
            )
        }

        composable(
            route = ROUTE_DETAIL,
            arguments = listOf(navArgument("photoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong("photoId") ?: return@composable
            PhotoDetailScreen(
                photoId = photoId,
                viewModel = galleryViewModel,
                onBack = { navController.popBackStack() },
                onOpenStudio = { id ->
                    navController.navigate("studio/$id")
                }
            )
        }

        composable(
            route = ROUTE_STUDIO,
            arguments = listOf(navArgument("photoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong("photoId") ?: return@composable
            EffectStudioScreen(
                photoId = photoId,
                onBack = { navController.popBackStack() },
                onSavedArtwork = { newId ->
                    navController.navigate("detail/$newId") {
                        popUpTo(ROUTE_GALLERY)
                    }
                }
            )
        }

        composable(ROUTE_COLLAGE) {
            CollageMakerScreen(
                viewModel = galleryViewModel,
                onBack = { navController.popBackStack() },
                onCreatedArtwork = { newId ->
                    navController.navigate("detail/$newId") {
                        popUpTo(ROUTE_GALLERY)
                    }
                }
            )
        }

        composable(ROUTE_REEL) {
            MotionReelScreen(
                viewModel = galleryViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
