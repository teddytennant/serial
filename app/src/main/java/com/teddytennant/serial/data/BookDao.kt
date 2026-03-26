package com.teddytennant.serial.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY lastOpenedAt DESC LIMIT 10")
    fun getRecentBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE filePath = :path LIMIT 1")
    suspend fun getBookByPath(path: String): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE books SET currentChapterIndex = :chapter, currentWordIndex = :word, lastOpenedAt = :time WHERE id = :id")
    suspend fun updateProgress(id: Long, chapter: Int, word: Int, time: Long = System.currentTimeMillis())
}
