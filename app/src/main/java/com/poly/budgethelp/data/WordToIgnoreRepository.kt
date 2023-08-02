package com.poly.budgethelp.data

import androidx.annotation.WorkerThread
import com.poly.budgethelp.dao.WordToIgnoreDao

class WordToIgnoreRepository (private val wordToIgnoreDao: WordToIgnoreDao) {
    val allWords = wordToIgnoreDao.getAll()

    @WorkerThread
    suspend fun insert(word: WordToIgnore) {
        wordToIgnoreDao.insert(word)
    }

    @WorkerThread
    suspend fun delete(word: String) {
        wordToIgnoreDao.delete(word)
    }
}