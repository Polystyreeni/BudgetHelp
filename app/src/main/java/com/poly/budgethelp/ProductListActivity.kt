package com.poly.budgethelp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.adapter.ProductListAdapter
import com.poly.budgethelp.adapter.ReceiptListAdapter
import com.poly.budgethelp.data.Product
import com.poly.budgethelp.viewmodel.CategoryViewModel
import com.poly.budgethelp.viewmodel.CategoryViewModelFactory
import com.poly.budgethelp.viewmodel.ProductViewModel
import com.poly.budgethelp.viewmodel.ProductViewModelFactory
import kotlinx.coroutines.launch

class ProductListActivity : AppCompatActivity() {

    private lateinit var selectedCategories: BooleanArray

    private lateinit var recyclerView: RecyclerView
    private val categoryList: ArrayList<Int> = arrayListOf()
    private lateinit var categoryArray: Array<String>
    private lateinit var categoryArrayList: ArrayList<String>
    private lateinit var productAdapter: ProductListAdapter
    lateinit var categoryAdapter: ArrayAdapter<String>

    var activePopup: PopupWindow? = null

    // Viewmodels
    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModelFactory((application as BudgetApplication).productRepository)
    }

    private val categoryViewModel: CategoryViewModel by viewModels {
        CategoryViewModelFactory((application as BudgetApplication).categoryRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_list)

        recyclerView = findViewById(R.id.productRecyclerView)
        categoryArray = arrayOf()
        categoryArrayList = arrayListOf()
        val categorySelectView: TextView = findViewById(R.id.productListCategorySelect)

        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryArrayList)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Get categories from DB
        categoryViewModel.allCategories.observe(this) {categories ->
            categories.let {
                categoryArray = Array(it.count()) { i -> it[i].categoryName }
                // it.forEach { category -> categoryList.add(category.categoryName) }
                selectedCategories = BooleanArray(it.count())
                it.forEach {category -> categoryArrayList.add(category.categoryName)}
                Log.d("ProductListActivity", "CategoryAdapter count: " + categoryAdapter.count)
            }
        }

        // Category dropdown
        categorySelectView.setOnClickListener {view ->
            val builder = AlertDialog.Builder(this)
            builder.setTitle(resources.getString(R.string.product_list_limit_category))
            builder.setCancelable(false)

            builder.setMultiChoiceItems(categoryArray, selectedCategories) { _, index, value ->
                if (value) {
                    categoryList.add(index)
                    categoryList.sort()
                }
                else {
                    categoryList.remove(index)
                }
            }

            builder.setPositiveButton(resources.getString(R.string.generic_reply_positive)) {_, _ ->
                val stringBuilder = StringBuilder()
                for (i in 0 until categoryList.size) {
                    stringBuilder.append(categoryArray[categoryList[i]])
                    if (i != categoryList.size - 1) {
                        stringBuilder.append(", ")
                    }
                }
                categorySelectView.text = stringBuilder.toString()
                getProducts()
            }
            builder.setNegativeButton(resources.getString(R.string.generic_reply_negative)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            builder.setNeutralButton(resources.getString(R.string.generic_reply_clear)) { _, _ ->
                for (i in selectedCategories.indices) {
                    selectedCategories[i] = false
                    categoryList.clear()
                    categorySelectView.text = ""
                }
            }

            builder.show()
        }

        productAdapter = ProductListAdapter()
        productAdapter.baseContext = this

        recyclerView.adapter = productAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        onBackPressedDispatcher.addCallback(this) {
            if (activePopup != null) {
                activePopup?.dismiss()
                activePopup = null
            } else {
                finish()
            }
        }

        val returnButton: View = findViewById(R.id.productListReturnButton)
        returnButton.setOnClickListener {_ ->
            if (activePopup == null) {
                finish()
            }
        }
    }

    private fun getProducts() {
        // Get all selected categories
        val products: MutableList<String> = mutableListOf()
        for (i in categoryArray.indices) {
            if (selectedCategories[i])
                products.add(categoryArray[i])
        }

        // Remove previous observers
        productViewModel.productsInCategories(products).removeObservers(this)

        // Start new observe
        productViewModel.productsInCategories(products).observe(this) { productsInCategory ->
            productsInCategory.let {
                Log.d("ProductListActivity", "Refresh products list")
                productAdapter.submitList(productsInCategory)
            }
        }
    }

    fun saveProduct(newProduct: Product) {
        lifecycleScope.launch {
            productViewModel.updateProduct(newProduct)
            productAdapter.notifyDataSetChanged()
        }
    }

    fun requestDeleteProduct(toDelete: Product) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.delete_product_header, toDelete.productName))
        builder.setCancelable(false)

        builder.setMessage(resources.getString(R.string.delete_product_message))
        builder.setPositiveButton(resources.getString(R.string.generic_reply_positive)) {dialogInterface, _ ->
            deleteProduct(toDelete)
            dialogInterface.dismiss()
        }

        builder.setNegativeButton(resources.getString(R.string.generic_reply_negative)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }

        builder.show()
    }

    private fun deleteProduct(toDelete: Product) {
        lifecycleScope.launch {
            productViewModel.deleteProduct(toDelete.productId)
            productAdapter.notifyDataSetChanged()
        }
    }
}