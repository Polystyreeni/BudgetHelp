package com.poly.budgethelp.data

import androidx.annotation.WorkerThread
import com.poly.budgethelp.dao.ReceiptWithProductDao
import kotlinx.coroutines.flow.Flow

class ReceiptProductRepository (private val receiptProductDao: ReceiptWithProductDao) {

    val products: Flow<List<ReceiptWithProducts>> = receiptProductDao.getProducts()

    fun productsInReceipt(receiptId: List<Long>): Flow<List<ReceiptWithProducts>> {
        return receiptProductDao.getProductsInReceipt(receiptId)
    }

    @WorkerThread
    suspend fun insert(data: ReceiptProductCrossRef) {
        receiptProductDao.insert(data)
    }
}