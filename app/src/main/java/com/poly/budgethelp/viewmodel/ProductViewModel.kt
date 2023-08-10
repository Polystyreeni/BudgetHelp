package com.poly.budgethelp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.poly.budgethelp.data.CategoryPricePojo
import com.poly.budgethelp.data.Product
import com.poly.budgethelp.data.ProductRepository
import kotlinx.coroutines.withContext

class ProductViewModel (private val repository: ProductRepository) : ViewModel() {
    val allProducts: LiveData<List<Product>> = repository.allProducts.asLiveData()

    fun productWithName(name: String): Product = repository.getProductByName(name)

    fun productsWithNames(names: List<String>): LiveData<List<Product>> =
        repository.getProductsWithNames(names).asLiveData()

    fun productsInCategories(categories: List<String>): LiveData<List<Product>> =
        repository.getProductsInCategories(categories).asLiveData()

    fun pricesInCategory(productIds: List<Long>): LiveData<List<CategoryPricePojo>> =
        repository.getPricesInCategory(productIds).asLiveData()

    suspend fun insert(product: Product): Long =
        withContext(viewModelScope.coroutineContext) {
            val toAdd = Product(product.productName.uppercase(), product.productCategory.uppercase(), product.productPrice)
            repository.insert(toAdd)
        }

    suspend fun updateProduct(product: Product) = repository.updateProduct(product)
}

class ProductViewModelFactory (private val repository: ProductRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}