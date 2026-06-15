package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.repository.StreamingRepository
import com.example.ui.screens.AppNavigationContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MediaViewModel
import com.example.ui.viewmodel.MediaViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local Room database
        val database = AppDatabase.getDatabase(this)
        val dao = database.streamingDao()

        // Create unified stream scraper repository
        val repository = StreamingRepository(dao, this)

        setContent {
            MyApplicationTheme {
                // Initialize central Media ViewModel
                val viewModel: MediaViewModel = viewModel(
                    factory = MediaViewModelFactory(application, repository)
                )

                // Render master view navigation graph container
                AppNavigationContainer(viewModel = viewModel)
            }
        }
    }
}
