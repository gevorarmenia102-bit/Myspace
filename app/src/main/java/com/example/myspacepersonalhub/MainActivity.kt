package com.example.myspacepersonalhub

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.myspacepersonalhub.core.model.ThemeMode
import com.example.myspacepersonalhub.core.util.SnackbarController
import com.example.myspacepersonalhub.navigation.NavigationHost
import com.example.myspacepersonalhub.theme.MySpaceTheme
import com.example.myspacepersonalhub.ui.components.ModernSnackbar

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MySpaceApplication

        setContent {
            val userPrefs by app.preferencesRepository.userPreferencesFlow.collectAsState(
                initial = com.example.myspacepersonalhub.core.datastore.UserPreferences()
            )

            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(Unit) {
                SnackbarController.events.collect { event ->
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = event.duration
                    )
                }
            }

            // Dynamic Window Flags for Privacy / App-switcher obscuring when biometric is enabled
            if (userPrefs.biometricLockEnabled) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            MySpaceTheme(themeMode = userPrefs.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavigationHost(app = app)

                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 80.dp, start = 12.dp, end = 12.dp)
                        ) { snackbarData ->
                            ModernSnackbar(snackbarData = snackbarData)
                        }
                    }
                }
            }
        }
    }
}
