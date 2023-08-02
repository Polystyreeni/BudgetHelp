package com.poly.budgethelp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.poly.budgethelp.data.WordToIgnore
import com.poly.budgethelp.data.WordToIgnoreRepository
import kotlinx.coroutines.launch

class WordToIgnoreViewModel (private val repository: WordToIgnoreRepository) : ViewModel() {
    val allWords: LiveData<List<WordToIgnore>> = repository.allWords.asLiveData()

    suspend fun insert(word: WordToIgnore) = viewModelScope.launch {
        repository.insert(word)
    }

    suspend fun delete(word: String) = viewModelScope.launch {
        repository.delete(word)
    }
}

class WordToIgnoreViewModelFactory(private val repository: WordToIgnoreRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WordToIgnoreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WordToIgnoreViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}