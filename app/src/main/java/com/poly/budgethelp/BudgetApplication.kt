package com.poly.budgethelp

import android.app.Application
import com.poly.budgethelp.data.CategoryRepository
import com.poly.budgethelp.data.ProductRepository
import com.poly.budgethelp.data.ReceiptProductRepository
import com.poly.budgethelp.data.ReceiptRepository
import com.poly.budgethelp.data.WordToIgnoreRepository
import com.poly.budgethelp.db.AppRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class BudgetApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob())

    // Using lazy so the database and repositories is only created when they're needed
    val database by lazy {AppRoomDatabase.getDatabase(this, applicationScope)}
    val categoryRepository by lazy {CategoryRepository(database.categoryDao())}
    val productRepository by lazy {ProductRepository(database.productDao())}
    val receiptRepository by lazy {ReceiptRepository(database.receiptDao())}
    val receiptProductRepository by lazy {ReceiptProductRepository(database.receiptProductDao())}
    val wordToIgnoreRepository by lazy {WordToIgnoreRepository(database.wordToIgnoreDao())}
}