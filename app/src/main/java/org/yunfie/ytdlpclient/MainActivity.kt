package org.yunfie.ytdlpclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.yunfie.ytdlpclient.ui.theme.YtdlpClientTheme
// import org.yunfie.ytdlpclient.ui.AppContent // Assuming this doesn't exist yet

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YtdlpClientTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // AppContent(modifier = Modifier.padding(innerPadding))
                    Text(
                        text = "Hello Android!",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
