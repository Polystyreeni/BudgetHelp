package com.poly.budgethelp.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.poly.budgethelp.data.Category
import com.poly.budgethelp.data.WordToIgnore
import kotlinx.coroutines.flow.Flow

@Dao
interface WordToIgnoreDao {
    @Query("SELECT * FROM wordtoignore")
    fun getAll(): Flow<List<WordToIgnore>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(wordToIgnore: WordToIgnore)

    @Query("DELETE FROM wordtoignore WHERE word = :word")
    suspend fun delete(word: String)

    @Query("DELETE FROM wordtoignore")
    suspend fun deleteAll()
}