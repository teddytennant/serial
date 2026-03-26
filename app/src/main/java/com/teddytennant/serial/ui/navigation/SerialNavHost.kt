package com.teddytennant.serial.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.teddytennant.serial.ui.screen.LibraryScreen
import com.teddytennant.serial.ui.screen.ReaderScreen
import com.teddytennant.serial.ui.screen.SettingsScreen

@Composable
fun SerialNavHost(
    pendingUri: Uri?,
    onUriConsumed: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "library") {
        composable("library") {
            LibraryScreen(
                onBookSelected = { bookId ->
                    navController.navigate("reader/$bookId")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                },
                pendingUri = pendingUri,
                onUriConsumed = onUriConsumed
            )
        }

        composable(
            route = "reader/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
            ReaderScreen(
                bookId = bookId,
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
