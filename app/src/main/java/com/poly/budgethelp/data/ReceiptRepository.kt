package com.poly.budgethelp.data

import androidx.annotation.WorkerThread
import com.poly.budgethelp.dao.ReceiptDao
import kotlinx.coroutines.flow.Flow


class ReceiptRepository (private val receiptDao: ReceiptDao) {
    val allReceipts: Flow<List<Receipt>> = receiptDao.getAllReceipts()

    fun receiptsInRange(lower: Long, upper: Long): Flow<List<Receipt>> {
        return receiptDao.getReceiptsInRange(lower, upper)
    }

    @WorkerThread
    suspend fun insert(receipt: Receipt): Long {
        return receiptDao.insertReceipt(receipt)
    }
}