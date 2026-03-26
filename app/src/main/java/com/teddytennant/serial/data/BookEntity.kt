package com.teddytennant.serial.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val filePath: String,
    val coverPath: String? = null,
    val lastOpenedAt: Long = System.currentTimeMillis(),
    val currentChapterIndex: Int = 0,
    val currentWordIndex: Int = 0,
    val totalChapters: Int = 0,
    val wpm: Int = 0, // 0 = use global default
    val theme: String = "" // empty = use global default
)
