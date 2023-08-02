package com.poly.budgethelp.data

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "receipt")
data class Receipt (
    @PrimaryKey(autoGenerate = true) var receiptId: Long,
    @ColumnInfo(name = "receiptName") var receiptName: String,
    @ColumnInfo(name = "receiptDate") var receiptDate: Long,
    @ColumnInfo(name = "receiptPrice") var receiptPrice: Float
) {
    constructor(name: String, date: Long, price: Float) : this(0, name, date, price)
}