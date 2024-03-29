package com.poly.budgethelp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.poly.budgethelp.data.CategoryPricePojo
import com.poly.budgethelp.data.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM product")
    fun getAll(): Flow<List<Product>>

    @Query("SELECT * FROM product ORDER BY productName")
    fun getAllSorted(): Flow<List<Product>>

    @Query("SELECT * FROM product WHERE productId == (:id)")
    fun getProductById(id: Long): Product

    @Query("SELECT * FROM product WHERE productName LIKE :qName")
    fun getProductByName(qName: String): Product

    @Query("SELECT * FROM product WHERE productName IN (:productNames)")
    fun getProductsWithNames(productNames: List<String>): Flow<List<Product>>

    @Query("SELECT * FROM product WHERE productCategory IN (:categoryNames)")
    fun getProductsInCategories(categoryNames: List<String>): Flow<List<Product>>

    @Query("SELECT * FROM product WHERE productCategory IN (:categoryNames) ORDER BY productName")
    fun getProductsInCategoriesSorted(categoryNames: List<String>): Flow<List<Product>>

    @Query("SELECT * FROM product WHERE productName LIKE :startStr")
    fun getProductsStartingWith(startStr: String): Flow<List<Product>>

    @Query("SELECT * FROM product WHERE productName LIKE :firstStr OR productName LIKE :secondStr")
    fun getProductsStartingWithEither(firstStr: String, secondStr: String): Flow<List<Product>>

    @Query("SELECT * FROM product WHERE productName LIKE :key1 OR productName LIKE :key2 OR productName LIKE :key3")
    fun getProductsNameContaining(key1: String, key2: String, key3: String): Flow<List<Product>>

    @Insert
    fun insertAll(vararg products: Product)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(product: Product): Long

    @Query("DELETE FROM product WHERE productId = :productId")
    suspend fun delete(productId: Long)

    @Query("SELECT productCategory AS category, SUM(productPrice) AS totalPrice FROM product WHERE productId IN (:productIds) GROUP BY productCategory")
    fun getPricesInCategory(productIds: List<Long>): Flow<List<CategoryPricePojo>>

    @Query("DELETE FROM product")
    suspend fun deleteAll()

    @Update
    suspend fun updateProduct(product: Product)
}