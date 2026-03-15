package com.rahees.quickscan.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahees.quickscan.data.local.ScanEntity
import com.rahees.quickscan.data.repository.ScanRepository
import com.rahees.quickscan.util.ContentType
import com.rahees.quickscan.util.detectContentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanResult(
    val content: String,
    val displayValue: String,
    val format: String,
    val type: ContentType
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: ScanRepository
) : ViewModel() {

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn.asStateFlow()

    private val _isMultiScanMode = MutableStateFlow(false)
    val isMultiScanMode: StateFlow<Boolean> = _isMultiScanMode.asStateFlow()

    private val _batchScans = MutableStateFlow<List<ScanResult>>(emptyList())
    val batchScans: StateFlow<List<ScanResult>> = _batchScans.asStateFlow()

    private val _lastScanResult = MutableStateFlow<ScanResult?>(null)
    val lastScanResult: StateFlow<ScanResult?> = _lastScanResult.asStateFlow()

    private var lastScanContent: String = ""
    private var lastScanTime: Long = 0L

    fun toggleFlash() {
        _isFlashOn.value = !_isFlashOn.value
    }

    fun toggleScanMode() {
        _isMultiScanMode.value = !_isMultiScanMode.value
        if (!_isMultiScanMode.value) {
            _batchScans.value = emptyList()
        }
    }

    fun onBarcodeScanned(content: String, displayValue: String, formatValue: String) {
        val now = System.currentTimeMillis()
        if (content == lastScanContent && (now - lastScanTime) < 2000L) {
            return
        }
        lastScanContent = content
        lastScanTime = now

        val type = detectContentType(content)
        val result = ScanResult(
            content = content,
            displayValue = displayValue,
            format = formatValue,
            type = type
        )

        viewModelScope.launch {
            repository.insert(
                ScanEntity(
                    content = content,
                    displayValue = displayValue,
                    format = formatValue,
                    type = type.name,
                    timestamp = now
                )
            )
        }

        if (_isMultiScanMode.value) {
            _batchScans.value = _batchScans.value + result
        } else {
            _lastScanResult.value = result
        }
    }

    fun clearLastResult() {
        _lastScanResult.value = null
    }

    fun clearBatch() {
        _batchScans.value = emptyList()
    }
}
