package com.poly.budgethelp

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.adapter.CategoryAdapter
import com.poly.budgethelp.data.Category
import com.poly.budgethelp.data.Product
import com.poly.budgethelp.db.AppRoomDatabase
import com.poly.budgethelp.utility.ActivityUtils
import com.poly.budgethelp.utility.TextUtils
import com.poly.budgethelp.viewmodel.CategoryViewModel
import com.poly.budgethelp.viewmodel.CategoryViewModelFactory
import com.poly.budgethelp.viewmodel.ProductViewModel
import com.poly.budgethelp.viewmodel.ProductViewModelFactory
import kotlinx.coroutines.launch

class CategoryActivity : AppCompatActivity() {

    private var activePopup: PopupWindow? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter

    // viewModels
    private val categoryViewModel: CategoryViewModel by viewModels {
        CategoryViewModelFactory((application as BudgetApplication).categoryRepository)
    }

    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModelFactory((application as BudgetApplication).productRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        recyclerView = findViewById(R.id.categoryRecyclerView)
        categoryAdapter = CategoryAdapter()
        categoryAdapter.baseContext = this

        recyclerView.adapter = categoryAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        categoryViewModel.allCategories.observe(this) {categories ->
            categories.let {
                categoryAdapter.submitList(it)
            }
        }

        // Add category functionality
        val addCategoryButton: Button = findViewById(R.id.categoryAddButton)
        addCategoryButton.setOnClickListener {_ ->
            createCategoryAddPopup()
        }

        // Return arrow functionality
        val returnButton: View = findViewById(R.id.categoryListReturnButton)
        returnButton.setOnClickListener { _ ->
            if (activePopup == null)
                finish()
        }

        if (ActivityUtils.isUsingNightModeResources(this)) {
            returnButton.setBackgroundColor(Color.WHITE)
        }
    }

    override fun onStop() {
        super.onStop()
        activePopup?.dismiss()
    }

    fun createEditCategoryPopup(category: Category, position: Int) {
        if (category.categoryName == AppRoomDatabase.DEFAULT_CATEGORY) {
            Toast.makeText(this, resources.getString(R.string.error_modify_default_category), Toast.LENGTH_SHORT).show()
            return
        }

        val popupData = ActivityUtils.createPopup(R.layout.popup_add_category, this)
        val headerTextView: TextView = popupData.first.findViewById(R.id.addCategoryHeaderTextView)
        val categoryNameEdit: EditText = popupData.first.findViewById(R.id.addCategoryNameEditText)
        val confirmButton: Button = popupData.first.findViewById(R.id.addCategoryConfirmButton)
        headerTextView.text = resources.getString(R.string.header_modify_category)
        categoryNameEdit.setText(category.categoryName)
        confirmButton.setOnClickListener {
            if (categoryNameEdit.text.toString() == category.categoryName) {
                popupData.second.dismiss()
            }
            else {
                popupData.second.dismiss()
                requestCategoryEdit(category, categoryNameEdit.text.toString(), position)
            }
        }

        activePopup = popupData.second
        popupData.second.setOnDismissListener { activePopup = null }
        popupData.second.isFocusable = true
        popupData.second.update()
    }

    private fun requestCategoryEdit(category: Category, newName: String, position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.category_edit_header))
        builder.setCancelable(false)

        builder.setMessage(
            TextUtils.getSpannedText(resources.getString(R.string.category_edit_warning,
            category.categoryName, newName.uppercase())))

        builder.setPositiveButton(resources.getString(R.string.generic_reply_positive)) {dialogInterface, _ ->
            modifyCategory(category, newName.uppercase())
            dialogInterface.dismiss()
        }

        builder.setNegativeButton(resources.getString(R.string.generic_reply_negative)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }

        builder.show()
    }

    fun requestCategoryDelete(category: Category, position: Int) {
        if (category.categoryName == AppRoomDatabase.DEFAULT_CATEGORY) {
            Toast.makeText(this, resources.getString(R.string.error_delete_default_category), Toast.LENGTH_SHORT).show()
            return
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.category_delete_header))
        builder.setCancelable(false)

        builder.setMessage(resources.getString(R.string.category_delete_warning,
            category.categoryName, AppRoomDatabase.DEFAULT_CATEGORY))
        builder.setPositiveButton(resources.getString(R.string.generic_reply_positive)) {dialogInterface, _ ->
            deleteCategory(category)
            dialogInterface.dismiss()
        }

        builder.setNegativeButton(resources.getString(R.string.generic_reply_negative)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }

        builder.show()
    }

    private fun modifyCategory(category: Category, newName: String) {
        activePopup?.dismiss()
        val loadPopup = ActivityUtils.createPopup(R.layout.popup_loading, this)
        val headerText: TextView = loadPopup.first.findViewById(R.id.loadPopupDescription)
        headerText.text = resources.getString(R.string.load_modify_category)
        loadPopup.second.setOnDismissListener { activePopup = null }
        activePopup = loadPopup.second

        lifecycleScope.launch {
            categoryViewModel.delete(category.categoryName)
            categoryViewModel.insert(Category(newName))
        }

        headerText.text = resources.getString(R.string.load_update_products)

        // Modify existing products to new category, observe once
        val liveData = productViewModel.productsInCategories(listOf(category.categoryName))
        liveData.observe(this, object: Observer<List<Product>> {
            override fun onChanged(products: List<Product>?) {
                liveData.removeObserver(this)
                products.let {
                    lifecycleScope.launch {
                        it?.forEach { product ->
                            val newProduct = Product(
                                product.productId,
                                product.productName,
                                newName,
                                product.productPrice
                            )
                            productViewModel.updateProduct(newProduct)
                            Log.d(
                                "CategoryActivity",
                                "Changed category of product: " + product.productName
                            )
                        }

                        activePopup?.dismiss()
                    }
                }
            }
        })
    }

    private fun deleteCategory(category: Category) {
        activePopup?.dismiss()
        val loadPopup = ActivityUtils.createPopup(R.layout.popup_loading, this)
        val headerText: TextView = loadPopup.first.findViewById(R.id.loadPopupDescription)
        headerText.text = resources.getString(R.string.load_update_products)
        loadPopup.second.setOnDismissListener { activePopup = null }
        activePopup = loadPopup.second

        val liveData = productViewModel.productsInCategories(listOf(category.categoryName))
        liveData.observe(this, object: Observer<List<Product>> {
            override fun onChanged(products: List<Product>?) {
                liveData.removeObserver(this)
                products.let {
                    lifecycleScope.launch {
                        it?.forEach { product ->
                            val newProduct = Product(
                                product.productId,
                                product.productName,
                                AppRoomDatabase.DEFAULT_CATEGORY,
                                product.productPrice
                            )
                            productViewModel.updateProduct(newProduct)
                            Log.d(
                                "CategoryActivity",
                                "Changed category of product: " + product.productName
                            )
                        }

                        // Delete category from database
                        categoryViewModel.delete(category.categoryName)
                        activePopup?.dismiss()
                    }
                }
            }
        })
    }

    private fun addCategory(categoryName: String) {
        lifecycleScope.launch {
            categoryViewModel.insert(Category(categoryName))
        }
    }

    private fun createCategoryAddPopup() {
        if (activePopup != null)
            return

        val popupData = ActivityUtils.createPopup(R.layout.popup_add_category, this)
        val categoryEditText: EditText = popupData.first.findViewById(R.id.addCategoryNameEditText)
        val confirmButton: Button = popupData.first.findViewById(R.id.addCategoryConfirmButton)

        confirmButton.setOnClickListener {
            val newCategoryName: String = categoryEditText.text.toString()
            if (newCategoryName == AppRoomDatabase.ADD_CATEGORY_TEXT
                || newCategoryName.isBlank()) {
                Toast.makeText(this, resources.getString(R.string.error_invalid_category), Toast.LENGTH_SHORT).show()
            } else {
                addCategory(newCategoryName.uppercase())
                popupData.second.dismiss()
            }
        }

        activePopup = popupData.second
        popupData.second.setOnDismissListener { activePopup = null }
        popupData.second.isFocusable = true
        popupData.second.update()
    }
}