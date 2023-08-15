package com.poly.budgethelp.data

import androidx.annotation.WorkerThread
import com.poly.budgethelp.dao.ProductDao
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {
    val allProducts: Flow<List<Product>> = productDao.getAll()

    fun getProductById(id: Long): Product = productDao.getProductById(id)

    fun getProductsWithNames(productNames: List<String>): Flow<List<Product>>
        = productDao.getProductsWithNames(productNames)

    fun getProductByName(name: String): Product = productDao.getProductByName(name)

    fun getProductsInCategories(categoryNames: List<String>): Flow<List<Product>> =
        productDao.getProductsInCategories(categoryNames)
    
    fun getPricesInCategory(productIds: List<Long>): Flow<List<CategoryPricePojo>> =
        productDao.getPricesInCategory(productIds)

    @WorkerThread
    suspend fun insert(product: Product): Long {
        return productDao.insert(product)
    }

    @WorkerThread
    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)

    @WorkerThread
    suspend fun deleteProduct(productId: Long) = productDao.delete(productId)
}