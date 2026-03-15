package com.rahees.quickscan.ui.history

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FilterType {
    ALL, QR, BARCODE, FAVORITES
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ScanRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(FilterType.ALL)
    val selectedFilter: StateFlow<FilterType> = _selectedFilter.asStateFlow()

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
}
