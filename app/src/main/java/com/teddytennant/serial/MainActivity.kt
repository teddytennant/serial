package com.teddytennant.serial

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.teddytennant.serial.ui.navigation.SerialNavHost
import com.teddytennant.serial.ui.theme.SerialTheme

class MainActivity : ComponentActivity() {

    var pendingEpubUri by mutableStateOf<Uri?>(null)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            SerialTheme {
                SerialNavHost(
                    pendingUri = pendingEpubUri,
                    onUriConsumed = { pendingEpubUri = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (intent.action == Intent.ACTION_VIEW) {
            pendingEpubUri = uri
        }
    }
}
