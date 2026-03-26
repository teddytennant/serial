package com.teddytennant.serial

import android.app.Application
import com.teddytennant.serial.data.BookDatabase
import com.teddytennant.serial.data.SettingsRepository

class SerialApp : Application() {

    val database: BookDatabase by lazy { BookDatabase.create(this) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
}
