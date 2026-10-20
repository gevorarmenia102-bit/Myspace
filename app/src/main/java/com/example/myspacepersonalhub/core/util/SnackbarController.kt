package com.example.myspacepersonalhub.core.util

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class SnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val duration: SnackbarDuration = SnackbarDuration.Short
)

/**
 * Global reactive controller for app-wide SnackBar notifications,
 * replacing standard Android Toast with modern Jetpack Compose Snackbars.
 * Uses SharedFlow so all active subscribers (activities / screens) receive events.
 */
object SnackbarController {
    private val _events = MutableSharedFlow<SnackbarEvent>(extraBufferCapacity = 10)
    val events = _events.asSharedFlow()

    fun showMessage(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        _events.tryEmit(SnackbarEvent(message = message, actionLabel = actionLabel, duration = duration))
    }
}
