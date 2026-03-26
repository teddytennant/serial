package com.teddytennant.serial.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val WPM = intPreferencesKey("wpm")
        val ORP_COLOR = longPreferencesKey("orp_color")
        val TEXT_SIZE = intPreferencesKey("text_size")
        val THEME = stringPreferencesKey("theme")
        val SENTENCE_PAUSE = stringPreferencesKey("sentence_pause")
        val AUTO_START = booleanPreferencesKey("auto_start")
    }

    data class Settings(
        val wpm: Int = 300,
        val orpColor: Long = 0xFFFF4444,
        val textSize: Int = 48,
        val theme: String = "dark",
        val sentencePause: String = "medium",
        val autoStart: Boolean = false
    )

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            wpm = prefs[Keys.WPM] ?: 300,
            orpColor = prefs[Keys.ORP_COLOR] ?: 0xFFFF4444,
            textSize = prefs[Keys.TEXT_SIZE] ?: 48,
            theme = prefs[Keys.THEME] ?: "dark",
            sentencePause = prefs[Keys.SENTENCE_PAUSE] ?: "medium",
            autoStart = prefs[Keys.AUTO_START] ?: false
        )
    }

    suspend fun updateWpm(wpm: Int) {
        context.dataStore.edit { it[Keys.WPM] = wpm }
    }

    suspend fun updateOrpColor(color: Long) {
        context.dataStore.edit { it[Keys.ORP_COLOR] = color }
    }

    suspend fun updateTextSize(size: Int) {
        context.dataStore.edit { it[Keys.TEXT_SIZE] = size }
    }

    suspend fun updateTheme(theme: String) {
        context.dataStore.edit { it[Keys.THEME] = theme }
    }

    suspend fun updateSentencePause(pause: String) {
        context.dataStore.edit { it[Keys.SENTENCE_PAUSE] = pause }
    }

    suspend fun updateAutoStart(auto: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_START] = auto }
    }
}
