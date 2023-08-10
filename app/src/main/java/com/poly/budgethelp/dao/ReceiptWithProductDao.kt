package com.poly.budgethelp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.poly.budgethelp.data.CategoryPricePojo
import com.poly.budgethelp.data.ReceiptProductCrossRef
import com.poly.budgethelp.data.ReceiptWithProducts
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptWithProductDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(join: ReceiptProductCrossRef)

    @Transaction
    @Query("SELECT * FROM receipt")
    fun getProducts(): Flow<List<ReceiptWithProducts>>

    @Transaction
    @Query("SELECT * FROM receipt WHERE receiptId IN (:receiptIds)")
    fun getProductsInReceipt(receiptIds: List<Long>): Flow<List<ReceiptWithProducts>>

    // @Transaction
    // @Query("SELECT productCategory AS category, SUM(productPrice) AS totalPrice FROM (SELECT products FROM receipt WHERE receiptId IN (:receiptIds)) GROUP BY productCategory")
    // fun getTotalPriceOfCategories(receiptIds: List<Long>): Flow<List<CategoryPricePojo>>

    @Query("DELETE FROM receiptProductRef")
    fun deleteAll()
}