package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.PulsePlayApp
import com.example.ui.theme.PulsePlayTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.ToolsViewModel
import kotlinx.coroutines.flow.MutableStateFlow

/** Carries a link shared into the app (e.g. from YouTube's Share button) to the UI. */
object SharedLinkBus {
    val pending = MutableStateFlow<String?>(null)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleShareIntent(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            PulsePlayTheme {
                val mainViewModel: MainViewModel = viewModel()
                PulsePlayApp(viewModel = mainViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        ToolsViewModel.extractUrl(text)?.let { SharedLinkBus.pending.value = it }
    }
}
