package com.cpen321.usermanagement

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cpen321.usermanagement.ui.navigation.AppNavigation
import com.cpen321.usermanagement.ui.theme.ProvideFontSizes
import com.cpen321.usermanagement.ui.theme.ProvideSpacing
import com.cpen321.usermanagement.ui.theme.UserManagementTheme
import dagger.hilt.android.AndroidEntryPoint
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log
import android.app.AlertDialog
import android.net.Uri


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MyFCM", "Notification permission granted")
        } else {
            Log.d("MyFCM", "Notification permission denied")        
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UserManagementTheme {
                UserManagementApp()
            }
        }

        // FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        //     if (!task.isSuccessful) {
        //         Log.w("MyFCM", "Fetching FCM token failed", task.exception)
        //         return@addOnCompleteListener
        //     }
        //     else {
        //         val token = task.result
        //         Log.d("ManualFCM", "Manual token: $token")
        //         MyFirebaseMessagingService().sendTokenToBackend(token)
        //     }
        // }

        Log.d("MyFCM", "Android SDK: ${Build.VERSION.SDK_INT}")

        // Request notification permission 
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            Log.d("MyFCM", "POST_NOTIFICATIONS granted? $granted")

            when {
                granted -> {
                    Log.d("MyFCM", "Permission already allowed")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.d("MyFCM", "Should show rationale")
                    showNotificationsRationale()
                }
                else -> {
                    Log.d("MyFCM", "Launching permission request")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun showNotificationsRationale() {
        AlertDialog.Builder(this)
            .setTitle("Enable notifications")
            .setMessage("We need permission to show notifications for chat messages and alerts.")
            .setPositiveButton("Allow") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("No thanks", null)
            .show()
    }

    private fun openAppNotificationSettings() {
        val intent = Intent().apply {
            action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    private fun openGeneralAppSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}

@Composable
fun UserManagementApp() {
    ProvideSpacing {
        ProvideFontSizes {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AppNavigation()
            }
        }
    }
}
