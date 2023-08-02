package com.poly.budgethelp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wordtoignore")
data class WordToIgnore (
    @PrimaryKey val word: String
    )
