package com.poly.budgethelp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("DELETE FROM receiptProductRef")
    fun deleteAll()
}