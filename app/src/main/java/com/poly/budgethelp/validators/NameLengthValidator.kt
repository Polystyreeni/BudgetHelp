package com.poly.budgethelp.validators

class NameLengthValidator : IProductValidator {
    override fun isValidItem(name: String, price: Float): Boolean {
        // TODO: Make this parameter user assignable
        return name.length >= 5
    }
}