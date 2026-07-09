package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.DashboardScreen
import com.example.ui.EditorScreen
import com.example.ui.ProjectViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val viewModel = ViewModelProvider(this)[ProjectViewModel::class.java]

    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val currentProject by viewModel.currentProject.collectAsState()

          if (currentProject == null) {
            DashboardScreen(viewModel = viewModel) { selected ->
              viewModel.selectProject(selected)
            }
          } else {
            EditorScreen(viewModel = viewModel) {
              viewModel.stopGame()
              // Select null to navigate back to dashboard
              viewModel.updateCurrentCode(viewModel.editorCode.value)
              viewModel.selectProject(null as com.example.data.Project?)
            }
          }
        }
      }
    }
  }
}

