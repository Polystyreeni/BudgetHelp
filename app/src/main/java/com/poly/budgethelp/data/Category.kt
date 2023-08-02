package com.poly.budgethelp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category")
data class Category (
    @PrimaryKey val categoryName: String
    )