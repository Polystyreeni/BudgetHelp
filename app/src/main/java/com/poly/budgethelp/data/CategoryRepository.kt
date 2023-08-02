package com.poly.budgethelp.data

import androidx.annotation.WorkerThread
import com.poly.budgethelp.dao.CategoryDao
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {
    val allCategories: Flow<List<Category>> = categoryDao.getAll()

    @WorkerThread
    suspend fun insert(category: Category) {
        categoryDao.insert(category)
    }
}