package com.teddytennant.serial.rsvp

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RsvpWord(
    val text: String,
    val orpIndex: Int, // index of the pivot character
    val beforeOrp: String,
    val orpChar: Char,
    val afterOrp: String,
    val index: Int
)

class RsvpEngine {

    private val _currentWord = MutableStateFlow<RsvpWord?>(null)
    val currentWord: StateFlow<RsvpWord?> = _currentWord.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _wordIndex = MutableStateFlow(0)
    val wordIndex: StateFlow<Int> = _wordIndex.asStateFlow()

    private var words: List<String> = emptyList()
    private var wpm: Int = 300
    private var sentencePauseMultiplier: Float = 1.5f

    fun load(wordList: List<String>, startIndex: Int = 0) {
        words = wordList
        _wordIndex.value = startIndex.coerceIn(0, (words.size - 1).coerceAtLeast(0))
        if (words.isNotEmpty()) {
            _currentWord.value = buildRsvpWord(words[_wordIndex.value], _wordIndex.value)
        }
    }

    fun setWpm(newWpm: Int) {
        wpm = newWpm.coerceIn(50, 3000)
    }

    fun setSentencePause(level: String) {
        sentencePauseMultiplier = when (level) {
            "none" -> 1.0f
            "short" -> 1.3f
            "medium" -> 1.8f
            "long" -> 2.5f
            else -> 1.5f
        }
    }

    suspend fun play() {
        if (words.isEmpty()) return
        _isPlaying.value = true
        while (_isPlaying.value && _wordIndex.value < words.size) {
            val word = words[_wordIndex.value]
            _currentWord.value = buildRsvpWord(word, _wordIndex.value)

            val baseDelay = 60_000L / wpm
            val extraDelay = calculateExtraDelay(word, baseDelay)
            delay(baseDelay + extraDelay)

            if (_isPlaying.value) {
                _wordIndex.value++
            }
        }
        _isPlaying.value = false
    }

    fun pause() {
        _isPlaying.value = false
    }

    fun seekTo(index: Int) {
        val clamped = index.coerceIn(0, (words.size - 1).coerceAtLeast(0))
        _wordIndex.value = clamped
        if (words.isNotEmpty()) {
            _currentWord.value = buildRsvpWord(words[clamped], clamped)
        }
    }

    fun skipForward(count: Int = 10) {
        seekTo(_wordIndex.value + count)
    }

    fun skipBackward(count: Int = 10) {
        seekTo(_wordIndex.value - count)
    }

    val totalWords: Int get() = words.size

    private fun calculateExtraDelay(word: String, baseDelay: Long): Long {
        var extra = 0L

        // Longer words get more time
        if (word.length > 8) extra += baseDelay / 4
        if (word.length > 12) extra += baseDelay / 4

        // Sentence-ending punctuation
        val lastChar = word.lastOrNull()
        if (lastChar == '.' || lastChar == '!' || lastChar == '?') {
            extra += (baseDelay * sentencePauseMultiplier).toLong()
        }
        // Mid-sentence punctuation
        else if (lastChar == ',' || lastChar == ';' || lastChar == ':') {
            extra += (baseDelay * 0.5f).toLong()
        }
        // Paragraph break (indicated by words ending with newline-related content handled in tokenization)
        if (word.endsWith("—") || word.endsWith("–")) {
            extra += (baseDelay * 0.3f).toLong()
        }

        return extra
    }

    companion object {
        fun calculateOrp(word: String): Int {
            val clean = word.replace(Regex("[^\\p{L}\\p{N}]"), "")
            val len = clean.length
            return when {
                len <= 1 -> 0
                len <= 5 -> 1
                len <= 9 -> 2
                len <= 13 -> 3
                else -> 4
            }
        }

        fun buildRsvpWord(text: String, index: Int): RsvpWord {
            val orpIndex = calculateOrp(text)
            val safeOrp = orpIndex.coerceIn(0, (text.length - 1).coerceAtLeast(0))
            return RsvpWord(
                text = text,
                orpIndex = safeOrp,
                beforeOrp = if (safeOrp > 0) text.substring(0, safeOrp) else "",
                orpChar = if (text.isNotEmpty()) text[safeOrp] else ' ',
                afterOrp = if (safeOrp < text.length - 1) text.substring(safeOrp + 1) else "",
                index = index
            )
        }
    }
}
