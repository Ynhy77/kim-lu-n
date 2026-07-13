package com.agmnetwork.md5analyzer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agmnetwork.md5analyzer.data.DataStoreManager
import com.agmnetwork.md5analyzer.data.api.RetrofitClient
import com.agmnetwork.md5analyzer.data.model.*
import com.agmnetwork.md5analyzer.util.MD5Analyzer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.Duration
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)
    private val apiService = RetrofitClient.getService()

    private val _installationId = MutableStateFlow("")
    val installationId: StateFlow<String> = _installationId.asStateFlow()

    private val _sessionId = MutableStateFlow<String?>(null)
    val sessionId: StateFlow<String?> = _sessionId.asStateFlow()

    private val _shortUrl = MutableStateFlow<String?>(null)
    val shortUrl: StateFlow<String?> = _shortUrl.asStateFlow()

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    private val _tokenExpiry = MutableStateFlow<String?>(null)
    val tokenExpiry: StateFlow<String?> = _tokenExpiry.asStateFlow()

    private val _historyList = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val historyList: StateFlow<List<HistoryEntry>> = _historyList.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()

    private val _isActivated = MutableStateFlow(false)
    val isActivated: StateFlow<Boolean> = _isActivated.asStateFlow()

    private val _timeRemaining = MutableStateFlow("")
    val timeRemaining: StateFlow<String> = _timeRemaining.asStateFlow()

    private val _baseUrlInput = MutableStateFlow(RetrofitClient.getBaseUrl())
    val baseUrlInput: StateFlow<String> = _baseUrlInput.asStateFlow()

    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            _installationId.value = dataStoreManager.getInstallationId()
            
            // Collect persistent variables
            launch {
                dataStoreManager.accessTokenFlow.collect { token ->
                    _accessToken.value = token
                    checkActivationState()
                }
            }
            launch {
                dataStoreManager.tokenExpiryFlow.collect { expiry ->
                    _tokenExpiry.value = expiry
                    checkActivationState()
                    startCountdownTimer(expiry)
                }
            }
            launch {
                dataStoreManager.sessionIdFlow.collect { sessId ->
                    _sessionId.value = sessId
                }
            }
            launch {
                dataStoreManager.shortUrlFlow.collect { url ->
                    _shortUrl.value = url
                }
            }
            launch {
                dataStoreManager.historyFlow.collect { list ->
                    _historyList.value = list
                }
            }
        }
    }

    fun updateBaseUrl(newUrl: String) {
        _baseUrlInput.value = newUrl
        RetrofitClient.setBaseUrl(newUrl)
    }

    private fun checkActivationState() {
        val token = _accessToken.value
        val expiry = _tokenExpiry.value
        if (!token.isNullOrEmpty() && !expiry.isNullOrEmpty()) {
            try {
                val expiryInstant = Instant.parse(expiry)
                _isActivated.value = Instant.now().isBefore(expiryInstant)
            } catch (e: Exception) {
                _isActivated.value = false
            }
        } else {
            _isActivated.value = false
        }
    }

    private fun startCountdownTimer(expiryString: String?) {
        countdownJob?.cancel()
        if (expiryString.isNullOrEmpty()) {
            _timeRemaining.value = ""
            return
        }

        countdownJob = viewModelScope.launch {
            try {
                val expiryInstant = Instant.parse(expiryString)
                while (true) {
                    val now = Instant.now()
                    if (now.isAfter(expiryInstant)) {
                        _timeRemaining.value = "Hết hạn"
                        _isActivated.value = false
                        break
                    }
                    val duration = Duration.between(now, expiryInstant)
                    val hours = duration.toHours()
                    val minutes = duration.toMinutes() % 60
                    val seconds = duration.getSeconds() % 60
                    _timeRemaining.value = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    delay(1000)
                }
            } catch (e: Exception) {
                _timeRemaining.value = ""
            }
        }
    }

    fun generateKeyLink() {
        _loading.value = true
        _error.value = null
        _success.value = null

        viewModelScope.launch {
            try {
                val id = _installationId.value
                val response = apiService.createKey(CreateKeyRequest(id))
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    dataStoreManager.saveSessionData(
                        sessionId = body.sessionId ?: "",
                        shortUrl = body.shortUrl ?: "",
                        expiry = body.expiresAt ?: ""
                    )
                    _success.value = "Tạo link thành công! Vui lòng bấm MỞ LINK để lấy key."
                } else {
                    _error.value = response.body()?.message ?: "Lỗi máy chủ khi tạo link."
                }
            } catch (e: Exception) {
                _error.value = "Không thể kết nối máy chủ: ${e.localizedMessage ?: "Vui lòng kiểm tra internet và URL API."}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun verifyKey(key: String) {
        val sId = _sessionId.value
        if (sId.isNullOrEmpty()) {
            _error.value = "Vui lòng tạo link lấy key trước!"
            return
        }
        if (key.trim().isEmpty()) {
            _error.value = "Vui lòng nhập key!"
            return
        }

        _loading.value = true
        _error.value = null
        _success.value = null

        viewModelScope.launch {
            try {
                val instId = _installationId.value
                val response = apiService.verifyKey(VerifyKeyRequest(sId, instId, key.trim()))
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    dataStoreManager.saveAuthData(
                        accessToken = body.accessToken ?: "",
                        expiry = body.expiresAt ?: ""
                    )
                    _success.value = "Kích hoạt thiết bị thành công!"
                } else {
                    _error.value = response.body()?.message ?: "Key không hợp lệ hoặc đã hết hạn."
                }
            } catch (e: Exception) {
                _error.value = "Lỗi kết nối: ${e.localizedMessage}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun verifyLocalTokenAsync(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val token = _accessToken.value
            val instId = _installationId.value
            if (token.isNullOrEmpty()) {
                onResult(false)
                return@launch
            }
            try {
                val response = apiService.checkToken("Bearer $token", CheckTokenRequest(instId))
                if (response.isSuccessful && response.body()?.success == true) {
                    onResult(true)
                } else {
                    // Token is invalid/expired on server
                    dataStoreManager.clearAuth()
                    _isActivated.value = false
                    onResult(false)
                }
            } catch (e: Exception) {
                // Network error, assume valid if local expiry hasn't passed
                val expiry = _tokenExpiry.value
                if (!expiry.isNullOrEmpty()) {
                    try {
                        val expiryInstant = Instant.parse(expiry)
                        onResult(Instant.now().isBefore(expiryInstant))
                    } catch (ex: Exception) {
                        onResult(false)
                    }
                } else {
                    onResult(false)
                }
            }
        }
    }

    fun logout() {
        _loading.value = true
        viewModelScope.launch {
            try {
                val token = _accessToken.value
                val instId = _installationId.value
                if (!token.isNullOrEmpty()) {
                    apiService.logout("Bearer $token", CheckTokenRequest(instId))
                }
            } catch (e: Exception) {
                // Ignore failure on logout network call
            } finally {
                dataStoreManager.clearAuth()
                _isActivated.value = false
                _loading.value = false
                _success.value = "Đã đăng xuất khỏi thiết bị."
            }
        }
    }

    fun addHistoryEntry(md5: String, analysis: MD5Analyzer.AnalysisResult) {
        viewModelScope.launch {
            val shortMd5 = if (md5.length > 10) {
                md5.substring(0, 5) + "..." + md5.substring(md5.length - 5)
            } else {
                md5
            }
            val entry = HistoryEntry(
                id = UUID.randomUUID().toString(),
                md5 = shortMd5,
                dice1 = analysis.dice1,
                dice2 = analysis.dice2,
                dice3 = analysis.dice3,
                total = analysis.total,
                result = analysis.result,
                timestamp = System.currentTimeMillis()
            )
            dataStoreManager.addHistoryEntry(entry)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dataStoreManager.clearHistory()
        }
    }

    fun clearMessages() {
        _error.value = null
        _success.value = null
    }
}
