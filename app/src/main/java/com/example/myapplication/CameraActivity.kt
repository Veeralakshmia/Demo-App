package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class CameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraPermissionExample()
        }
    }
}



@Composable
fun CameraPermissionExample() {
    val context = LocalContext.current

    // Define required permissions
    val permissions = remember {
        mutableStateListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).apply {
            // For Android 10+ we need different storage permissions
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    // Track permission states
    var permissionGranted by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var permissionDeniedPermanently by remember { mutableStateOf(false) }

    // Check if all permissions are granted
    fun checkPermissions() {
        permissionGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        val allGranted = permissionsResult.values.all { it }

        if (allGranted) {
            permissionGranted = true
            showPermissionRationale = false
            permissionDeniedPermanently = false
        } else {
            // Check if we should show rationale for any permission
            val shouldShowRationale = permissions.any {
                !permissionsResult.getOrDefault(it, false) &&
                        (context as ComponentActivity).shouldShowRequestPermissionRationale(it)
            }

            if (shouldShowRationale) {
                showPermissionRationale = true
                permissionDeniedPermanently = false
            } else {
                // User denied with "Don't ask again"
                permissionDeniedPermanently = true
                showPermissionRationale = false
            }

            permissionGranted = false
        }
    }

    // Initialize permissions check and request if needed
    LaunchedEffect(Unit) {
        checkPermissions()

        // If permissions not already granted, request them automatically
        if (!permissionGranted) {
            // Check if any permission needs rationale before requesting
            val shouldShowRationale = permissions.any {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED &&
                        (context as ComponentActivity).shouldShowRequestPermissionRationale(it)
            }

            if (shouldShowRationale) {
                showPermissionRationale = true
            } else {
                // Directly launch permission request if no rationale needed
                permissionLauncher.launch(permissions.toTypedArray())
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            permissionGranted -> {
                // All permissions granted, show camera UI
                CameraContent()
            }
            showPermissionRationale -> {
                // Need to explain why we need permissions
                PermissionRationaleScreen {
                    // Request permissions again
                    permissionLauncher.launch(permissions.toTypedArray())
                }
            }
            permissionDeniedPermanently -> {
                // User denied permissions permanently, direct to settings
                PermissionDeniedPermanentlyScreen {
                    // Open app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }
            else -> {
                // Initial request for permissions
                PermissionRequestScreen {
                    permissionLauncher.launch(permissions.toTypedArray())
                }
            }
        }
    }
}

@Composable
fun CameraContent() {
    // Mock camera UI for demonstration
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Camera Preview Would Show Here",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Button(
                onClick = { /* Take picture */ },
                modifier = Modifier.size(72.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                // Camera button
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Camera & Storage Access Required",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "This app requires camera and storage access to take and save photos. " +
                    "Please grant these permissions to continue.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text("Grant Permissions")
        }
    }
}

@Composable
fun PermissionRationaleScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Permissions Required",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Without camera and storage permissions, we cannot provide the main features of this app. " +
                    "These permissions are used only for taking and saving photos.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text("Try Again")
        }
    }
}

@Composable
fun PermissionDeniedPermanentlyScreen(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Permissions Required",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Camera and storage permissions have been permanently denied. " +
                    "Please enable them in your device settings to use this feature.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text("Open Settings")
        }
    }
}