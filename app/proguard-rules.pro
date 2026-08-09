# Compose keeps what it needs via consumer rules; this file holds app-specific keeps.
-dontwarn kotlinx.**
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable *;
}
