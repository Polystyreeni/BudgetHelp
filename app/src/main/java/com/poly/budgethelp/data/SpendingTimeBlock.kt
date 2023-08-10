package com.poly.budgethelp.data

data class SpendingTimeBlock (
    val startTime: Long,
    val endTime: Long,
    val spending: List<CategoryPricePojo>
)
