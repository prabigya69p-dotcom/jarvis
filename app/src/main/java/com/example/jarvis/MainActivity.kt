package com.example.jarvis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.jarvis.ui.JarvisScreen
import com.example.jarvis.ui.JarvisViewModel
import com.example.jarvis.ui.theme.JarvisTheme

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JarvisTheme {
                JarvisScreen(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopListening()
    }
}
