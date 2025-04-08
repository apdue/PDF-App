package com.mypdf.ocrpdfapp.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mypdf.ocrpdfapp.model.PdfFile
import com.mypdf.ocrpdfapp.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    LARGEST_FIRST,
    SMALLEST_FIRST,
    NAME_ASC,
    NAME_DESC
}

class PdfViewModel : ViewModel() {
    private val _pdfFiles = MutableStateFlow<List<PdfFile>>(emptyList())
    val pdfFiles: StateFlow<List<PdfFile>> = _pdfFiles

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedPdf = MutableStateFlow<PdfFile?>(null)
    val selectedPdf: StateFlow<PdfFile?> = _selectedPdf

    private val _isInPdfView = MutableStateFlow(false)
    val isInPdfView: StateFlow<Boolean> = _isInPdfView

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filteredPdfFiles = MutableStateFlow<List<PdfFile>>(emptyList())
    val filteredPdfFiles: StateFlow<List<PdfFile>> = _filteredPdfFiles

    private var currentSortOrder = SortOrder.NEWEST_FIRST
    private var hasPermission = false
    private lateinit var repository: PdfRepository

    fun initialize(context: Context) {
        repository = PdfRepository(context)
        if (hasPermission) {
            loadPdfFiles()
        }
    }

    fun updatePermissionStatus(granted: Boolean) {
        Log.d("PdfViewModel", "Permission status updated: $granted")
        hasPermission = granted
        if (granted) {
            refreshPdfFiles() // Use refresh instead of load to ensure fresh data
        } else {
            _pdfFiles.value = emptyList()
            _filteredPdfFiles.value = emptyList()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterPdfFiles()
    }

    private fun filterPdfFiles() {
        val query = _searchQuery.value.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            _pdfFiles.value
        } else {
            _pdfFiles.value.filter {
                it.name.lowercase().contains(query)
            }
        }
        _filteredPdfFiles.value = sortPdfFiles(filtered)
    }

    fun loadPdfFiles() {
        if (!hasPermission) {
            Log.d("PdfViewModel", "Skipping loadPdfFiles - no permission")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                val pdfList = repository.getAllPdfFiles()
                Log.d("PdfViewModel", "Loaded ${pdfList.size} PDF files")
                _pdfFiles.value = sortPdfFiles(pdfList)
                filterPdfFiles() // Update filtered results when loading new files
            } catch (e: Exception) {
                Log.e("PdfViewModel", "Error loading PDF files", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshPdfFiles() {
        if (!hasPermission) {
            Log.d("PdfViewModel", "Skipping refreshPdfFiles - no permission")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.clearCache()
                _isLoading.value = true
                val pdfList = repository.getAllPdfFiles()
                Log.d("PdfViewModel", "Refreshed ${pdfList.size} PDF files")
                _pdfFiles.value = sortPdfFiles(pdfList)
                filterPdfFiles()
            } catch (e: Exception) {
                Log.e("PdfViewModel", "Error refreshing PDF files", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectPdf(pdf: PdfFile) {
        _selectedPdf.value = pdf
        _isInPdfView.value = true
        Log.d("TAGPDFF", "selectPdf: ")
    }

    fun clearSelectedPdf() {
        _selectedPdf.value = null
        _isInPdfView.value = false
        Log.d("TAGPDFF", "clearSelectedPdf: ")

    }

    fun setSortOrder(order: SortOrder) {
        if (currentSortOrder != order) {
            currentSortOrder = order
            _pdfFiles.value = sortPdfFiles(_pdfFiles.value)
            _filteredPdfFiles.value = sortPdfFiles(_filteredPdfFiles.value)
        }
    }

    private fun sortPdfFiles(files: List<PdfFile>): List<PdfFile> {
        return when (currentSortOrder) {
            SortOrder.NEWEST_FIRST -> files.sortedByDescending { it.lastModified }
            SortOrder.OLDEST_FIRST -> files.sortedBy { it.lastModified }
            SortOrder.LARGEST_FIRST -> files.sortedByDescending { it.size }
            SortOrder.SMALLEST_FIRST -> files.sortedBy { it.size }
            SortOrder.NAME_ASC -> files.sortedBy { it.name.lowercase() }
            SortOrder.NAME_DESC -> files.sortedByDescending { it.name.lowercase() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.clearCache()
    }

    fun openPdfFromUri(uri: android.net.Uri, fileName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Create a temporary PdfFile object for the URI
                val tempPdfFile = PdfFile(
                    name = fileName,
                    path = uri.toString(),
                    size = 0, // Size unknown for content URIs
                    lastModified = System.currentTimeMillis(),
                    file = null,
                    uri = uri
                )

                // Select the PDF to view it
                selectPdf(tempPdfFile)
            } finally {
                _isLoading.value = false
            }
        }
    }
} 