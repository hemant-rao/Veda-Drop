package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.data.LocationHelper
import com.example.ui.NikhatGlowViewModel
import com.example.ui.NikhatGlowMainShell
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        androidx.compose.material3.Surface(
          modifier = Modifier.fillMaxSize(),
          color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
          val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<NikhatGlowViewModel>()
          // §687 — request location once on launch; on grant (or if already
          // granted) capture the device fix so "near me" discovery engages. The
          // app works fine if the user denies — discovery just isn't distance-sorted
          // and addresses fall back to manual entry.
          val permLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
          ) { granted ->
            if (granted.values.any { it }) viewModel.captureDeviceLocation()
          }
          LaunchedEffect(Unit) {
            // §703 — pull the admin-controlled app config on launch so feature
            // gates + role-based nav + policy copy reflect the server immediately.
            viewModel.loadAppConfig()
            if (LocationHelper.hasPermission(this@MainActivity)) {
              viewModel.captureDeviceLocation()
            } else {
              permLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
              )
            }
          }
          NikhatGlowMainShell(viewModel = viewModel)
        }
      }
    }
  }
}
