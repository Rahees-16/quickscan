package com.rahees.quickscan.ui.history

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahees.quickscan.data.local.ScanEntity
import com.rahees.quickscan.data.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class FilterType {
    ALL, QR, BARCODE, FAVORITES
}

data class ScanStats(
    val totalScans: Int = 0,
    val qrCount: Int = 0,
    val barcodeCount: Int = 0,
    val favoriteCount: Int = 0,
    val mostScannedType: String = "-"
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ScanRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(FilterType.ALL)
    val selectedFilter: StateFlow<FilterType> = _selectedFilter.asStateFlow()

    private val allScans: StateFlow<List<ScanEntity>> = repository.getAllScans()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val scanStats: StateFlow<ScanStats> = allScans.map { scans ->
        if (scans.isEmpty()) {
            ScanStats()
        } else {
            val qrCount = scans.count { it.format == "QR_CODE" }
            val barcodeCount = scans.size - qrCount
            val favoriteCount = scans.count { it.isFavorite }
            val mostScannedType = scans.groupBy { it.type }
                .maxByOrNull { it.value.size }?.key ?: "-"
            ScanStats(
                totalScans = scans.size,
                qrCount = qrCount,
                barcodeCount = barcodeCount,
                favoriteCount = favoriteCount,
                mostScannedType = mostScannedType
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScanStats()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val scans: StateFlow<List<ScanEntity>> = combine(
        _searchQuery,
        _selectedFilter
    ) { query, filter ->
        Pair(query, filter)
    }.flatMapLatest { (query, filter) ->
        when {
            query.isNotBlank() -> repository.searchScans(query)
            filter == FilterType.FAVORITES -> repository.getFavorites()
            filter == FilterType.QR -> repository.getByType("QR_CODE")
            filter == FilterType.BARCODE -> repository.getAllScans()
            else -> repository.getAllScans()
        }
    }.combine(_selectedFilter) { scans, filter ->
        when (filter) {
            FilterType.BARCODE -> scans.filter { it.format != "QR_CODE" }
            else -> scans
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun filterByType(filter: FilterType) {
        _selectedFilter.value = filter
        _searchQuery.value = ""
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun deleteScan(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun exportToCsv(context: Context) {
        viewModelScope.launch {
            val scansList = allScans.value
            if (scansList.isEmpty()) return@launch

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val csvBuilder = StringBuilder()
            csvBuilder.appendLine("Content,Format,Type,Timestamp,Favorite")

            scansList.forEach { scan ->
                val escapedContent = scan.content.replace("\"", "\"\"")
                val timestamp = dateFormat.format(Date(scan.timestamp))
                csvBuilder.appendLine("\"$escapedContent\",\"${scan.format}\",\"${scan.type}\",\"$timestamp\",\"${scan.isFavorite}\"")
            }

            val fileName = "QuickScan_Export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"

            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let { fileUri ->
                resolver.openOutputStream(fileUri)?.use { outputStream ->
                    outputStream.write(csvBuilder.toString().toByteArray())
                }

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Share scan history")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
