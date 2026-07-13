package com.agmnetwork.md5analyzer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Service
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.agmnetwork.md5analyzer.MainActivity
import com.agmnetwork.md5analyzer.data.DataStoreManager
import com.agmnetwork.md5analyzer.data.model.HistoryEntry
import com.agmnetwork.md5analyzer.ui.theme.*
import com.agmnetwork.md5analyzer.util.MD5Analyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    companion object {
        private const val CHANNEL_ID = "agm_md5_overlay_channel"
        private const val NOTIFICATION_ID = 2026
        const val ACTION_STOP_SERVICE = "STOP_OVERLAY_SERVICE"
        const val ACTION_HIDE_BUBBLE = "HIDE_OVERLAY_BUBBLE"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val myViewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = myViewModelStore

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private lateinit var dataStoreManager: DataStoreManager
    private var composeView: ComposeView? = null
    private lateinit var layoutParams: WindowManager.LayoutParams

    private var isExpandedState = false
    private var lastX = 100
    private var lastY = 300

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        savedStateRegistryController.performRestore(null)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        dataStoreManager = DataStoreManager(this)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent != null) {
            when (intent.action) {
                ACTION_STOP_SERVICE -> {
                    stopSelf()
                    return START_NOT_STICKY
                }
                ACTION_HIDE_BUBBLE -> {
                    removeOverlayView()
                    Toast.makeText(this, "Đã ẩn bong bóng nổi. Mở ứng dụng để bật lại.", Toast.LENGTH_SHORT).show()
                    return START_STICKY
                }
            }
        }

        // Create persistent foreground notification
        val stopIntent = Intent(this, OverlayService::class.java).apply { action = ACTION_STOP_SERVICE }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val hideIntent = Intent(this, OverlayService::class.java).apply { action = ACTION_HIDE_BUBBLE }
        val hidePendingIntent = PendingIntent.getService(this, 1, hideIntent, PendingIntent.FLAG_IMMUTABLE)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val notificationPendingIntent = PendingIntent.getActivity(this, 2, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AGM MD5 FLOATING ANALYZER")
            .setContentText("AGM MD5 Analyzer đang hiển thị bong bóng")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(notificationPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Ẩn bong bóng", hidePendingIntent)
            .addAction(android.R.drawable.ic_delete, "Dừng dịch vụ", stopPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Show floating bubble overlay
        showOverlay()

        return START_STICKY
    }

    private fun showOverlay() {
        if (composeView != null) return

        serviceScope.launch {
            val savedPos = dataStoreManager.bubblePositionFlow.first()
            if (savedPos.first != -1 && savedPos.second != -1) {
                lastX = savedPos.first
                lastY = savedPos.second
            }

            withContext(Dispatchers.Main) {
                setupWindowManager()
            }
        }
    }

    private fun setupWindowManager() {
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = lastX
            y = lastY
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)

            setContent {
                AGMMD5Theme {
                    OverlayViewContent()
                }
            }
        }

        try {
            windowManager.addView(composeView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeOverlayView() {
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            composeView = null
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
        serviceScope.launch {
            dataStoreManager.saveBubblePosition(lastX, lastY)
            serviceJob.cancel()
        }
        removeOverlayView()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AGM MD5 Overlay Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for AGM MD5 analyzer background overlay service"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateWindowFocus(focusable: Boolean) {
        if (composeView == null) return
        isExpandedState = focusable
        layoutParams.flags = if (focusable) {
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }
        try {
            windowManager.updateViewLayout(composeView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Composable
    private fun OverlayViewContent() {
        var isExpanded by remember { mutableStateOf(false) }
        var md5Input by remember { mutableStateOf("") }
        var result by remember { mutableStateOf<MD5Analyzer.AnalysisResult?>(null) }
        var validationError by remember { mutableStateOf("") }
        var isRolling by remember { mutableStateOf(false) }

        val clipboardManager = LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val localContext = LocalContext.current

        fun vibrateDevice() {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = localContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    manager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    localContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                if (vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(100)
                    }
                }
            } catch (e: Exception) {
                // Ignore haptic errors
            }
        }

        Box(modifier = Modifier.wrapContentSize()) {
            if (!isExpanded) {
                // Collapsed Circle Bubble
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AccentBlue)
                        .border(2.dp, AccentGreen, CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    // Snap bubble smoothly to nearest side
                                    val screenWidth = localContext.resources.displayMetrics.widthPixels
                                    val bubbleWidthPx = 56 * localContext.resources.displayMetrics.density
                                    val snapThreshold = screenWidth / 2
                                    lastX = if (layoutParams.x + bubbleWidthPx / 2 < snapThreshold) {
                                        0
                                    } else {
                                        (screenWidth - bubbleWidthPx).toInt()
                                    }
                                    layoutParams.x = lastX
                                    try {
                                        windowManager.updateViewLayout(composeView, layoutParams)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    serviceScope.launch {
                                        dataStoreManager.saveBubblePosition(lastX, lastY)
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    layoutParams.x += dragAmount.x.toInt()
                                    layoutParams.y += dragAmount.y.toInt()
                                    lastX = layoutParams.x
                                    lastY = layoutParams.y
                                    try {
                                        windowManager.updateViewLayout(composeView, layoutParams)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            )
                        }
                        .clickable {
                            isExpanded = true
                            updateWindowFocus(true)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AGM",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            } else {
                // Expanded Overlay Panel
                Card(
                    modifier = Modifier
                        .width(300.dp)
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentGreen))
                                Text(
                                    text = "AGM MD5 ANALYZER",
                                    color = TextLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = {
                                        isExpanded = false
                                        updateWindowFocus(false)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Minimize, contentDescription = "Thu nhỏ", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        stopSelf()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Đóng", tint = DangerRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Text Field
                        OutlinedTextField(
                            value = md5Input,
                            onValueChange = {
                                md5Input = it.trim()
                                validationError = ""
                                result = null
                            },
                            label = { Text("Nhập hoặc dán MD5", fontSize = 11.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextLight),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true
                        )

                        // Validation error
                        if (validationError.isNotEmpty()) {
                            Text(validationError, color = DangerRed, fontSize = 11.sp, lineHeight = 13.sp)
                        }

                        // Action Buttons: DÁN, PHÂN TÍCH, XÓA
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // DÁN button
                            Button(
                                onClick = {
                                    try {
                                        val clip = clipboardManager.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            val text = clip.getItemAt(0).text
                                            if (!text.isNullOrEmpty()) {
                                                md5Input = text.toString().trim()
                                                validationError = ""
                                                result = null
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Ignore clipboard errors
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CardSelected),
                                border = BorderStroke(1.dp, BorderColor),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, tint = TextLight, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("DÁN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // PHÂN TÍCH button
                            Button(
                                onClick = {
                                    if (MD5Analyzer.isValidMD5(md5Input)) {
                                        serviceScope.launch {
                                            isRolling = true
                                            result = null
                                            delay(700) // 700ms simulation roll
                                            val computedResult = MD5Analyzer.analyze(md5Input)
                                            result = computedResult
                                            isRolling = false
                                            
                                            // Vibrate device
                                            vibrateDevice()

                                            // Save to localized history
                                            val shortMd5 = if (md5Input.length > 10) {
                                                md5Input.substring(0, 5) + "..." + md5Input.substring(md5Input.length - 5)
                                            } else {
                                                md5Input
                                            }
                                            dataStoreManager.addHistoryEntry(
                                                HistoryEntry(
                                                    id = UUID.randomUUID().toString(),
                                                    md5 = shortMd5,
                                                    dice1 = computedResult.dice1,
                                                    dice2 = computedResult.dice2,
                                                    dice3 = computedResult.dice3,
                                                    total = computedResult.total,
                                                    result = computedResult.result,
                                                    timestamp = System.currentTimeMillis()
                                                )
                                            )
                                        }
                                    } else {
                                        validationError = "MD5 không hợp lệ. Vui lòng nhập đúng 32 ký tự hexadecimal."
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp),
                                enabled = md5Input.isNotEmpty() && !isRolling
                            ) {
                                if (isRolling) {
                                    CircularProgressIndicator(color = TextLight, modifier = Modifier.size(16.dp))
                                } else {
                                    Text("PHÂN TÍCH", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // XÓA button
                            Button(
                                onClick = {
                                    md5Input = ""
                                    validationError = ""
                                    result = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CardSelected),
                                border = BorderStroke(1.dp, BorderColor),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("XÓA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Output Block
                        AnimatedVisibility(visible = result != null) {
                            val res = result
                            if (res != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkBg)
                                        .padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Dice Icons
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf(res.dice1, res.dice2, res.dice3).forEach { d ->
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(DiceColor)
                                                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = d.toString(),
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Tổng: ${res.total}",
                                            color = TextLight,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )

                                        Text(
                                            text = res.result,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 20.sp,
                                            color = if (res.result == "TÀI") AccentBlue else AccentGreen
                                        )

                                        IconButton(
                                            onClick = {
                                                try {
                                                    val copyText = "MD5: $md5Input\nXúc xắc: ${res.dice1} - ${res.dice2} - ${res.dice3}\nTổng: ${res.total}\nKết quả mô phỏng: ${res.result}"
                                                    clipboardManager.setPrimaryClip(ClipData.newPlainText("AGM MD5", copyText))
                                                    Toast.makeText(localContext, "Đã sao chép kết quả", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    // Ignore copy failure
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Sao chép", tint = AccentBlue, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Small warning string
                        Text(
                            text = "Kết quả mô phỏng, không bảo đảm thắng trò chơi.",
                            fontSize = 9.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
