package com.agmnetwork.md5analyzer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.agmnetwork.md5analyzer.service.OverlayService
import com.agmnetwork.md5analyzer.ui.screens.DashboardScreen
import com.agmnetwork.md5analyzer.ui.screens.LockScreen
import com.agmnetwork.md5analyzer.ui.theme.AGMMD5Theme
import com.agmnetwork.md5analyzer.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AGMMD5Theme {
                val isActivated by viewModel.isActivated.collectAsState()
                var isOverlayActive by remember { mutableStateOf(false) }

                // Check overlay active state by attempting to read standard flags or checking service state
                // In Kotlin, we can check overlay active status by sending broadcast or monitoring
                // For simplicity, we can maintain the state inside MainActivity
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (!isActivated) {
                        LockScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        DashboardScreen(
                            viewModel = viewModel,
                            isOverlayActive = isOverlayActive,
                            onStartOverlay = {
                                checkAndStartOverlay {
                                    isOverlayActive = true
                                }
                            },
                            onStopOverlay = {
                                stopOverlayService()
                                isOverlayActive = false
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    private fun checkAndStartOverlay(onSuccess: () -> Unit) {
        // 1. Verify token with server before starting
        viewModel.verifyLocalTokenAsync { isValid ->
            if (!isValid) {
                stopOverlayService()
                Toast.makeText(this, "Phiên sử dụng đã hết hạn! Vui lòng nhập key mới.", Toast.LENGTH_LONG).show()
                return@verifyLocalTokenAsync
            }

            // 2. Check overlay draw permissions
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Vui lòng cấp quyền Phủ trên ứng dụng khác để mở bong bóng!", Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                // 3. Start Foreground service
                startOverlayService()
                onSuccess()
            }
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        stopService(intent)
    }
}
