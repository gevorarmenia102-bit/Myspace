package com.example.myspacepersonalhub.ui.components

import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.myspacepersonalhub.CloudConfigActivity
import com.example.myspacepersonalhub.core.util.SnackbarController

/**
 * Secret 15-click detector for opening the hidden CloudConfigActivity.
 * When the user taps the "MySpace Personal Hub" layout 15 times within 2.5s intervals,
 * it launches the secret Cloud Configuration Activity (Firebase & GitHub settings).
 */
@Composable
fun rememberSecretCloudTrigger(targetClicks: Int = 15): () -> Unit {
    val context = LocalContext.current
    var clickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    return remember(context) {
        {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > 2500L) {
                // Reset if user stops or pauses for more than 2.5 seconds
                clickCount = 1
            } else {
                clickCount++
            }
            lastClickTime = currentTime

            if (clickCount >= targetClicks) {
                clickCount = 0
                SnackbarController.showMessage("🔓 Opening Secret Cloud Config...")
                val intent = Intent(context, CloudConfigActivity::class.java)
                context.startActivity(intent)
            } else if (clickCount >= 10) {
                val remaining = targetClicks - clickCount
                SnackbarController.showMessage("⚡ $remaining taps to Secret Cloud Config")
            }
        }
    }
}
