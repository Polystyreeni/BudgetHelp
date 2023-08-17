package com.poly.budgethelp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.poly.budgethelp.data.Product
import com.poly.budgethelp.data.Receipt
import com.poly.budgethelp.data.ReceiptRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReceiptViewModel (private val repository: ReceiptRepository) : ViewModel() {
    val allReceipts: LiveData<List<Receipt>> = repository.allReceipts.asLiveData()

    fun receiptsInRange(start: Long, end: Long): LiveData<List<Receipt>> =
        repository.receiptsInRange(start, end).asLiveData()

    /*fun insert(receipt: Receipt) = viewModelScope.launch {
        repository.insert(receipt)
    }*/
    suspend fun insert(receipt: Receipt): Long =
        withContext(viewModelScope.coroutineContext) {
            repository.insert(receipt)
        }

    suspend fun deleteReceiptWithId(id: Long) = withContext(viewModelScope.coroutineContext) {
        repository.deleteReceiptWithId(id)
    }
}

class ReceiptViewModelFactory(private val repository: ReceiptRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReceiptViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReceiptViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}