package com.teddytennant.serial.ui.screen

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teddytennant.serial.SerialApp
import com.teddytennant.serial.data.BookEntity
import com.teddytennant.serial.epub.EpubBook
import com.teddytennant.serial.epub.EpubChapter
import com.teddytennant.serial.epub.EpubParser
import com.teddytennant.serial.rsvp.RsvpEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<SerialApp>()
    private val dao get() = app.database.bookDao()
    private val parser = EpubParser(application)
    val engine = RsvpEngine()

    private val _book = MutableStateFlow<BookEntity?>(null)
    val book: StateFlow<BookEntity?> = _book.asStateFlow()

    private val _chapters = MutableStateFlow<List<EpubChapter>>(emptyList())
    val chapters: StateFlow<List<EpubChapter>> = _chapters.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex: StateFlow<Int> = _currentChapterIndex.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _wpm = MutableStateFlow(300)
    val wpm: StateFlow<Int> = _wpm.asStateFlow()

    val settings = app.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var playJob: Job? = null
    private var epubBook: EpubBook? = null

    fun loadBook(bookId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            try {
                val entity = dao.getBookById(bookId) ?: run {
                    _error.value = "Book not found"
                    _loading.value = false
                    return@launch
                }
                _book.value = entity

                val globalSettings = app.settings.settings.first()
                val effectiveWpm = if (entity.wpm > 0) entity.wpm else globalSettings.wpm
                _wpm.value = effectiveWpm
                engine.setWpm(effectiveWpm)
                engine.setSentencePause(globalSettings.sentencePause)

                val uri = Uri.fromFile(java.io.File(entity.filePath))
                val epub = parser.parse(uri)
                epubBook = epub
                _chapters.value = epub.chapters

                val chapterIndex = entity.currentChapterIndex.coerceIn(0, (epub.chapters.size - 1).coerceAtLeast(0))
                _currentChapterIndex.value = chapterIndex

                if (epub.chapters.isNotEmpty()) {
                    engine.load(epub.chapters[chapterIndex].words, entity.currentWordIndex)
                }

                // Update book metadata if needed
                if (entity.totalChapters != epub.chapters.size) {
                    dao.update(entity.copy(totalChapters = epub.chapters.size))
                }

                _loading.value = false

                // Auto-start if enabled
                if (globalSettings.autoStart) {
                    play()
                }
            } catch (e: Exception) {
                _error.value = "Failed to load: ${e.message}"
                _loading.value = false
            }
        }
    }

    fun play() {
        playJob?.cancel()
        playJob = viewModelScope.launch {
            engine.play()
            // When play finishes (end of chapter), auto-advance
            if (engine.wordIndex.value >= engine.totalWords) {
                nextChapter()
            }
        }
    }

    fun pause() {
        engine.pause()
        playJob?.cancel()
        saveProgress()
    }

    fun togglePlayPause() {
        if (engine.isPlaying.value) pause() else play()
    }

    fun setWpm(newWpm: Int) {
        _wpm.value = newWpm
        engine.setWpm(newWpm)
        // Save per-book WPM
        viewModelScope.launch(Dispatchers.IO) {
            _book.value?.let { dao.update(it.copy(wpm = newWpm)) }
        }
    }

    fun goToChapter(index: Int) {
        val chapters = _chapters.value
        if (index !in chapters.indices) return
        val wasPlaying = engine.isPlaying.value
        engine.pause()
        playJob?.cancel()

        _currentChapterIndex.value = index
        engine.load(chapters[index].words, 0)
        saveProgress()

        if (wasPlaying) play()
    }

    fun nextChapter() {
        goToChapter(_currentChapterIndex.value + 1)
    }

    fun previousChapter() {
        // If we're past the first few words, restart current chapter
        if (engine.wordIndex.value > 20) {
            goToChapter(_currentChapterIndex.value)
        } else {
            goToChapter(_currentChapterIndex.value - 1)
        }
    }

    fun seekToWord(index: Int) {
        engine.seekTo(index)
    }

    private fun saveProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            _book.value?.let { book ->
                dao.updateProgress(
                    id = book.id,
                    chapter = _currentChapterIndex.value,
                    word = engine.wordIndex.value
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.pause()
        // Final save - runs on IO synchronously in onCleared context
        _book.value?.let { book ->
            val dao = app.database.bookDao()
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                dao.updateProgress(
                    id = book.id,
                    chapter = _currentChapterIndex.value,
                    word = engine.wordIndex.value
                )
            }
        }
    }
}
