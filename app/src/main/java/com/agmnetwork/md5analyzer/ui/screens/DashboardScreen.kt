package com.agmnetwork.md5analyzer.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agmnetwork.md5analyzer.data.model.HistoryEntry
import com.agmnetwork.md5analyzer.ui.theme.*
import com.agmnetwork.md5analyzer.ui.viewmodel.MainViewModel
import com.agmnetwork.md5analyzer.util.MD5Analyzer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    isOverlayActive: Boolean,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val tokenExpiry by viewModel.tokenExpiry.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.success.collectAsState()

    var inAppMd5Input by remember { mutableStateOf("") }
    var inAppResult by remember { mutableStateOf<MD5Analyzer.AnalysisResult?>(null) }
    var inAppValidationMsg by remember { mutableStateOf("") }
    var isAnalyzingInApp by remember { mutableStateOf(false) }

    // Scroll state for the dashboard content since we have lists too
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Title Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AGM MD5 FLOATING ANALYZER",
                    style = Typography.titleLarge,
                    color = AccentGreen,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(AccentGreen)
                    )
                    Text(
                        text = "Đã kích hoạt thiết bị",
                        color = TextLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (timeRemaining.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hiệu lực còn lại: $timeRemaining",
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Overlay Bubble Control Buttons
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "BONG BÓNG NỔI (OVERLAY BUBBLE)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStartOverlay,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOverlayActive) CardSelected else AccentBlue
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TextLight)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BẬT BONG BÓNG", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onStopOverlay,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOverlayActive) DangerRed else CardSelected
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isOverlayActive) DangerRed else BorderColor)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = TextLight)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TẮT BONG BÓNG", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Text(
                    text = "Trạng thái: ${if (isOverlayActive) "Đang hoạt động" else "Đang đóng"}",
                    color = if (isOverlayActive) AccentGreen else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // In-App Analyzer Section (Convenient Simulation)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "PHÂN TÍCH TRONG ỨNG DỤNG",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )

                OutlinedTextField(
                    value = inAppMd5Input,
                    onValueChange = {
                        inAppMd5Input = it.trim()
                        inAppValidationMsg = ""
                        inAppResult = null
                    },
                    label = { Text("Nhập chuỗi MD5 32 ký tự") },
                    placeholder = { Text("e.g. 71b3e8c9735d64cbb0f6d62a34b288e4") },
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

                if (inAppValidationMsg.isNotEmpty()) {
                    Text(inAppValidationMsg, color = DangerRed, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        viewModel.clearMessages()
                        if (MD5Analyzer.isValidMD5(inAppMd5Input)) {
                            coroutineScope.launch {
                                isAnalyzingInApp = true
                                inAppResult = null
                                delay(700) // Simulate rolling
                                val res = MD5Analyzer.analyze(inAppMd5Input)
                                inAppResult = res
                                isAnalyzingInApp = false
                                viewModel.addHistoryEntry(inAppMd5Input, res)
                            }
                        } else {
                            inAppValidationMsg = "MD5 không hợp lệ. Vui lòng nhập đúng 32 ký tự hexadecimal."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(8.dp),
                    enabled = inAppMd5Input.isNotEmpty() && !isAnalyzingInApp
                ) {
                    if (isAnalyzingInApp) {
                        CircularProgressIndicator(color = TextLight, modifier = Modifier.size(20.dp))
                    } else {
                        Text("PHÂN TÍCH", fontWeight = FontWeight.Bold)
                    }
                }

                // In-app Result Output
                AnimatedVisibility(visible = inAppResult != null) {
                    val res = inAppResult
                    if (res != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardSelected)
                                .padding(12.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("KẾT QUẢ MÔ PHỎNG", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Dice displays
                                    listOf(res.dice1, res.dice2, res.dice3).forEach { d ->
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(DiceColor)
                                                .border(1.dp, Color.Gray, RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = d.toString(),
                                                color = Color.Black,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "TỔNG ĐIỂM: ${res.total}",
                                    fontWeight = FontWeight.Bold,
                                    color = TextLight,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = res.result,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (res.result == "TÀI") AccentBlue else AccentGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // Simulation Algorithm Explanation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "GIẢI THÍCH THUẬT TOÁN ĐỊNH HƯỚNG",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue
                )
                Text(
                    text = "1. Chuỗi MD5 được chuẩn hóa viết thường.\n" +
                            "2. Lấy 3 cặp ký tự hex đầu tiên đại diện cho 3 viên xúc xắc:\n" +
                            "   - Xúc xắc 1: Ký tự 0–1 (Ví dụ: `71` -> 113 % 6 + 1 = 6)\n" +
                            "   - Xúc xắc 2: Ký tự 2–3 (Ví dụ: `b3` -> 179 % 6 + 1 = 6)\n" +
                            "   - Xúc xắc 3: Ký tự 4–5 (Ví dụ: `e8` -> 232 % 6 + 1 = 5)\n" +
                            "3. Tổng điểm = Xúc xắc 1 + 2 + 3.\n" +
                            "4. Phân loại kết quả:\n" +
                            "   - Tổng >= 11: TÀI\n" +
                            "   - Tổng < 11: XỈU",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        }

        // Warnings and disclaimers (Crucial)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x11EF4444))
                .border(1.dp, Color(0x33EF4444), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "CẢNH BÁO: Kết quả chỉ là ánh xạ toán học từ chuỗi MD5, không phải dự đoán kết quả máy chủ và không bảo đảm thắng. Nghiêm cấm sử dụng ứng dụng vào mục đích cờ bạc.",
                fontSize = 11.sp,
                color = Color(0xFFFCA5A5),
                lineHeight = 15.sp
            )
        }

        // History Log Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LỊCH SỬ PHÂN TÍCH (MAX 20)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    if (historyList.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearHistory() },
                            colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Xóa", fontSize = 11.sp)
                        }
                    }
                }

                if (historyList.isEmpty()) {
                    Text(
                        text = "Chưa có lịch sử phân tích.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        historyList.forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkBg)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.md5,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = TextLight
                                    )
                                    Text(
                                        text = "Xúc xắc: ${entry.dice1}-${entry.dice2}-${entry.dice3} (Tổng ${entry.total})",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                                Text(
                                    text = entry.result,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (entry.result == "TÀI") AccentBlue else AccentGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // Logout Button
        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = CardSelected),
            border = BorderStroke(1.dp, DangerRed),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = DangerRed)
            Spacer(modifier = Modifier.width(6.dp))
            Text("ĐĂNG XUẤT THIẾT BỊ", color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
