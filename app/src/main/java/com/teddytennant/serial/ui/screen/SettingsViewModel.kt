package com.teddytennant.serial.ui.screen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.teddytennant.serial.SerialApp
import com.teddytennant.serial.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as SerialApp).settings

    val settings: StateFlow<SettingsRepository.Settings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.Settings())

    fun updateWpm(wpm: Int) = viewModelScope.launch { repo.updateWpm(wpm) }
    fun updateTextSize(size: Int) = viewModelScope.launch { repo.updateTextSize(size) }
    fun updateTheme(theme: String) = viewModelScope.launch { repo.updateTheme(theme) }
    fun updateSentencePause(pause: String) = viewModelScope.launch { repo.updateSentencePause(pause) }
    fun updateAutoStart(auto: Boolean) = viewModelScope.launch { repo.updateAutoStart(auto) }
    fun updateOrpColor(color: Long) = viewModelScope.launch { repo.updateOrpColor(color) }
}
