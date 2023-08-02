package com.poly.budgethelp.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ReceiptWithProducts (
    @Embedded var receipt: Receipt,
    @Relation (
        parentColumn = "receiptId",
        entity = Product::class,
        entityColumn = "productId",
        associateBy = Junction(ReceiptProductCrossRef::class)
    )
    var products: List<Product>
)