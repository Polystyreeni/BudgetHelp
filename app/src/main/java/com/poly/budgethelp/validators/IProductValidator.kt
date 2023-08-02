package com.poly.budgethelp.validators

interface IProductValidator {
    fun isValidItem(name: String, price: Float): Boolean
}