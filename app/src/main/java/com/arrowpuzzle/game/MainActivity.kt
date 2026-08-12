package com.arrowpuzzle.game

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arrowpuzzle.game.core.ads.AdManager
import com.arrowpuzzle.game.core.data.AppViewModel
import com.arrowpuzzle.game.core.design.ArrowPuzzleTheme
import com.arrowpuzzle.game.navigation.ArrowPuzzleApp

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* either way, we carry on */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Installed before super.onCreate so the system splash hands straight over
        // to our own launch screen with no flash of an unstyled window.
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        // GDPR/UMP consent gate, then (if applicable) kick off the ad preloads
        // so a banner/interstitial/rewarded ad is ready by the time the player
        // reaches a screen that shows one.
        AdManager.requestConsent(this) {}

        setContent {
            ArrowPuzzleTheme {
                val appViewModel: AppViewModel = viewModel(
                    factory = AppViewModel.factory(applicationContext)
                )
                ArrowPuzzleApp(appViewModel = appViewModel)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
