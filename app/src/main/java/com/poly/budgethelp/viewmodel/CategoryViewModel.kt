package com.poly.budgethelp.viewmodel

import androidx.compose.ui.text.toUpperCase
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.poly.budgethelp.data.Category
import com.poly.budgethelp.data.CategoryRepository
import com.poly.budgethelp.utility.TextUtils
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

class CategoryViewModel(private val repository: CategoryRepository): ViewModel() {
    val allCategories: LiveData<List<Category>> = repository.allCategories.asLiveData()

    // fun categoryWithId(id: Int): Category = repository.categoryWithId(id)

    fun insert(category: Category) = viewModelScope.launch {
        val toAdd = Category(TextUtils.sanitizeText(category.categoryName.uppercase()))
        repository.insert(toAdd)
    }

    fun delete(categoryName: String) = viewModelScope.launch {
        repository.delete(categoryName)
    }
}

class CategoryViewModelFactory (private val repository: CategoryRepository) : ViewModelProvider.Factory {
    override fun <T: ViewModel> create(modelClass : Class<T>) : T {
        if (modelClass.isAssignableFrom(CategoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}