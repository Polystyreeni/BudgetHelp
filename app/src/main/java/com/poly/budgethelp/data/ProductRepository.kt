package com.poly.budgethelp.data

import androidx.annotation.WorkerThread
import com.poly.budgethelp.dao.ProductDao
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {
    val allProducts: Flow<List<Product>> = productDao.getAll()

    val allProductsSorted: Flow<List<Product>> = productDao.getAllSorted()

    fun getProductById(id: Long): Product = productDao.getProductById(id)

    fun getProductsWithNames(productNames: List<String>): Flow<List<Product>>
        = productDao.getProductsWithNames(productNames)

    fun getProductByName(name: String): Product = productDao.getProductByName(name)

    fun getProductsStartingWith(startStr: String): Flow<List<Product>>
        = productDao.getProductsStartingWith(startStr)

    fun getProductsStartingWithEither(first: String, second: String): Flow<List<Product>>
        = productDao.getProductsStartingWithEither(first, second)

    fun getProductNamesContaining(nameWords: List<String>): Flow<List<Product>> {
        val params: MutableList<String> = mutableListOf(nameWords[0], nameWords[0], nameWords[0])

        for (i in nameWords.indices) {
            if (i >= params.size)
                break
            params[i] = nameWords[i]
        }

        return productDao.getProductsNameContaining(params[0], params[1], params[2])
    }

    fun getProductsInCategories(categoryNames: List<String>): Flow<List<Product>> =
        productDao.getProductsInCategories(categoryNames)

    fun getProductsInCategoriesSorted(categoryNames: List<String>): Flow<List<Product>> =
        productDao.getProductsInCategoriesSorted(categoryNames)
    
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