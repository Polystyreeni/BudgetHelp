package com.poly.budgethelp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product")
data class Product (
        @PrimaryKey(autoGenerate = true) val productId: Long,
        @ColumnInfo(name = "productName") val productName: String,
        @ColumnInfo(name = "productCategory") val productCategory: String,
        @ColumnInfo(name = "productPrice") val productPrice: Float
) {
        constructor(name: String, category: String, price: Float) : this(0, name, category, price)
}