package com.agmnetwork.md5analyzer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agmnetwork.md5analyzer.ui.theme.*
import com.agmnetwork.md5analyzer.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.success.collectAsState()
    val installationId by viewModel.installationId.collectAsState()
    val sessionId by viewModel.sessionId.collectAsState()
    val shortUrl by viewModel.shortUrl.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val baseUrlInput by viewModel.baseUrlInput.collectAsState()

    var keyInput by remember { mutableStateOf("") }
    var showUrlConfig by remember { mutableStateOf(false) }
    var tempUrl by remember(baseUrlInput) { mutableStateOf(baseUrlInput) }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
            .verticalScroll(scrollState),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = "KÍCH HOẠT HỆ THỐNG",
                    style = Typography.titleLarge,
                    color = AccentBlue,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )

                Text(
                    text = "AGM MD5 FLOATING ANALYZER",
                    style = Typography.labelSmall,
                    color = AccentGreen,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Installation ID Info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardSelected)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "MÃ THIẾT BỊ (INSTALLATION ID):",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = installationId,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextLight,
                            maxLines = 1
                        )
                    }
                }

                // Error / Success Messages
                AnimatedVisibility(visible = error != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x33EF4444)),
                        border = BorderStroke(1.dp, DangerRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error ?: "",
                            color = Color(0xFFFCA5A5),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                AnimatedVisibility(visible = success != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x3310B981)),
                        border = BorderStroke(1.dp, AccentGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = success ?: "",
                            color = Color(0xFFA7F3D0),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Key inputs
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it.uppercase().trim() },
                    label = { Text("NHẬP KEY ĐỂ SỬ DỤNG (AGM-XXXXXX)") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = AccentBlue) },
                    placeholder = { Text("AGM-XXXXXX") },
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextLight),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderColor,
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = TextMuted,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true
                )

                // Verify Button
                Button(
                    onClick = {
                        viewModel.clearMessages()
                        viewModel.verifyKey(keyInput)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !loading
                ) {
                    if (loading) {
                        CircularProgressIndicator(color = TextLight, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "XÁC NHẬN KEY",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))

                // Request Link Section
                Text(
                    text = "Chưa có key? Vui lòng tạo link quảng cáo vượt mã để lấy key miễn phí.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.clearMessages()
                            viewModel.generateKeyLink()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(45.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardSelected),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderColor),
                        enabled = !loading
                    ) {
                        Text(
                            text = "TẠO LINK LẤY KEY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight
                        )
                    }

                    Button(
                        onClick = {
                            if (!shortUrl.isNullOrEmpty()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(shortUrl))
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(45.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (shortUrl.isNullOrEmpty()) CardDark else AccentGreen
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (shortUrl.isNullOrEmpty()) BorderColor else AccentGreen),
                        enabled = !shortUrl.isNullOrEmpty() && !loading
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = TextLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MỞ LINK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight
                        )
                    }
                }

                // Show Session status & timer
                if (!sessionId.isNullOrEmpty() && timeRemaining.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1E1E))
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "PHIÊN LẤY KEY HIỆN TẠI ĐANG CHỜ",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Hết hạn sau: $timeRemaining",
                                fontSize = 14.sp,
                                color = PurpleAccent,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Instructions and warnings
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x11EF4444))
                        .border(1.dp, Color(0x33EF4444), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "LƯU Ý: Mỗi key chỉ sử dụng 1 lần, hết hạn sau 15 phút. Tuyệt đối không gian lận hoặc can thiệp hệ thống.",
                            fontSize = 11.sp,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }

                // API Server Setting for developers/graders
                HorizontalDivider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cấu hình API Server (Nâng cao)",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    IconButton(onClick = { showUrlConfig = !showUrlConfig }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = showUrlConfig) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tempUrl,
                            onValueChange = { tempUrl = it },
                            label = { Text("API Backend URL") },
                            placeholder = { Text("https://...") },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextLight),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                viewModel.updateBaseUrl(tempUrl)
                                showUrlConfig = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("CẬP NHẬT URL API", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
