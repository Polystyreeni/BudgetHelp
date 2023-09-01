package com.poly.budgethelp.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.poly.budgethelp.data.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM category WHERE categoryName = :categoryName")
    fun getCategoryByName(categoryName: String): Category

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category)

    @Insert
    fun insertAll(vararg categories: Category)

    @Query("DELETE FROM category WHERE categoryName = :categoryName")
    suspend fun delete(categoryName: String)

    @Query("DELETE FROM Category")
    suspend fun deleteAll()
}