package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.JellyTuneViewModel
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainLibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.theme.JellyTuneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JellyTuneTheme {
                val viewModel: JellyTuneViewModel = viewModel()
                val activeServer by viewModel.activeServer.collectAsState()
                var isPlayerExpanded by remember { mutableStateOf(false) }

                if (activeServer == null) {
                    LoginScreen(viewModel = viewModel)
                } else {
                    MainLibraryScreen(
                        viewModel = viewModel,
                        onExpandPlayer = { isPlayerExpanded = true }
                    )

                    // Phonograph style Slide-up full screen media player
                    AnimatedVisibility(
                        visible = isPlayerExpanded,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        NowPlayingScreen(
                            viewModel = viewModel,
                            onDismiss = { isPlayerExpanded = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
