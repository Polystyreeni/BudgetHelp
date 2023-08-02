package com.poly.budgethelp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.poly.budgethelp.data.ReceiptProductCrossRef
import com.poly.budgethelp.data.ReceiptProductRepository
import com.poly.budgethelp.data.ReceiptWithProducts
import kotlinx.coroutines.launch

class ReceiptProductViewModel (private val repository: ReceiptProductRepository) : ViewModel() {
    suspend fun insert(receiptProductCrossRef: ReceiptProductCrossRef) = viewModelScope.launch {
        repository.insert(receiptProductCrossRef)
    }

    val products: LiveData<List<ReceiptWithProducts>> = repository.products.asLiveData()

    fun productsInReceipt(receiptId: List<Long>): LiveData<List<ReceiptWithProducts>> {
        return repository.productsInReceipt(receiptId).asLiveData()
    }
}

class ReceiptProductViewModelFactory(private val repository: ReceiptProductRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReceiptProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReceiptProductViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}