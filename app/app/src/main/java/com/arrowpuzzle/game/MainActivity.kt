package com.arrowpuzzle.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arrowpuzzle.game.core.data.AppViewModel
import com.arrowpuzzle.game.core.design.ArrowPuzzleTheme
import com.arrowpuzzle.game.navigation.ArrowPuzzleApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Installed before super.onCreate so the system splash hands straight over
        // to our own launch screen with no flash of an unstyled window.
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ArrowPuzzleTheme {
                val appViewModel: AppViewModel = viewModel(
                    factory = AppViewModel.factory(applicationContext)
                )
                ArrowPuzzleApp(appViewModel = appViewModel)
            }
        }
    }
}
