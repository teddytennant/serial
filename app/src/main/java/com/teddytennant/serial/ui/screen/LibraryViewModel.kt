package com.teddytennant.serial.ui.screen

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teddytennant.serial.SerialApp
import com.teddytennant.serial.data.BookEntity
import com.teddytennant.serial.epub.EpubParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<SerialApp>()
    private val dao get() = app.database.bookDao()
    private val parser = EpubParser(application)

    val recentBooks: StateFlow<List<BookEntity>> = dao.getRecentBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importingBook = MutableStateFlow(false)
    val importingBook: StateFlow<Boolean> = _importingBook.asStateFlow()

    private val _importedBookId = MutableStateFlow<Long?>(null)
    val importedBookId: StateFlow<Long?> = _importedBookId.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun importBook(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _importingBook.value = true
            _error.value = null
            try {
                // Copy file to internal storage for reliable access
                val fileName = "book_${System.currentTimeMillis()}.epub"
                val destFile = java.io.File(app.filesDir, "epubs/$fileName")
                destFile.parentFile?.mkdirs()

                app.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw Exception("Could not read file")

                val localUri = Uri.fromFile(destFile)

                // Check if we already have this book
                val existing = dao.getBookByPath(destFile.absolutePath)
                if (existing != null) {
                    dao.update(existing.copy(lastOpenedAt = System.currentTimeMillis()))
                    _importedBookId.value = existing.id
                } else {
                    val epub = parser.parse(localUri)
                    val entity = BookEntity(
                        title = epub.title,
                        author = epub.author,
                        filePath = destFile.absolutePath,
                        coverPath = epub.coverPath,
                        totalChapters = epub.chapters.size
                    )
                    val id = dao.insert(entity)
                    _importedBookId.value = id
                }
            } catch (e: Exception) {
                _error.value = "Failed to open book: ${e.message}"
            } finally {
                _importingBook.value = false
            }
        }
    }

    fun onBookImportHandled() {
        _importedBookId.value = null
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(book.id)
            // Clean up file
            java.io.File(book.filePath).delete()
            book.coverPath?.let { java.io.File(it).delete() }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
