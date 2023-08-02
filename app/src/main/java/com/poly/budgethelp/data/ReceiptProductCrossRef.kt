package com.poly.budgethelp.data

import androidx.room.Entity

@Entity(tableName = "receiptProductRef", primaryKeys = ["receiptId", "productId"])
data class ReceiptProductCrossRef (
    val receiptId: Long,
    val productId: Long
)