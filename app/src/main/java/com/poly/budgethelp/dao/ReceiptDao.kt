package com.poly.budgethelp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.poly.budgethelp.data.Receipt
import com.poly.budgethelp.data.ReceiptWithProducts
import kotlinx.coroutines.flow.Flow


@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipt ORDER BY receiptDate DESC")
    fun getAllReceipts(): Flow<List<Receipt>>

    @Query("SELECT * FROM receipt WHERE receiptDate >= :lower AND receiptDate <= :upper ORDER BY receiptDate DESC")
    fun getReceiptsInRange(lower: Long, upper: Long): Flow<List<Receipt>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReceipt(receipt: Receipt): Long

    @Query("DELETE FROM receipt")
    suspend fun deleteAllReceipts()

    @Query("DELETE FROM receipt WHERE receiptId = :id")
    suspend fun deleteReceiptWithId(id: Long)

    @Query("DELETE FROM Receipt")
    suspend fun deleteAll()
}