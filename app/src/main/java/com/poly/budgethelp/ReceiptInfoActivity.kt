package com.poly.budgethelp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.adapter.ProductAdapter
import com.poly.budgethelp.viewmodel.CategoryViewModel
import com.poly.budgethelp.viewmodel.CategoryViewModelFactory
import com.poly.budgethelp.viewmodel.ReceiptProductViewModel
import com.poly.budgethelp.viewmodel.ReceiptProductViewModelFactory

class ReceiptInfoActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    lateinit var categoryAdapter: ArrayAdapter<String>
    private var receiptId: Long? = null

    private lateinit var receiptNameView: TextView
    private lateinit var receiptDateView: TextView
    private lateinit var receiptPriceView: TextView

    // View models
    private val categoryViewModel: CategoryViewModel by viewModels {
        CategoryViewModelFactory((application as BudgetApplication).categoryRepository)
    }

    private val receiptProductViewModel: ReceiptProductViewModel by viewModels {
        ReceiptProductViewModelFactory((application as BudgetApplication).receiptProductRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt_info)

        receiptNameView = findViewById(R.id.receiptInfoNameText)
        receiptDateView = findViewById(R.id.receiptInfoDateText)
        receiptPriceView = findViewById(R.id.receiptInfoPriceText)
        // TODO: Add return and delete buttons

        productAdapter = ProductAdapter()
        productAdapter.context = this

        val categoryList: ArrayList<String> = ArrayList()
        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryList)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoryViewModel.allCategories.observe(this) { categories ->
            categories.let {
                it.forEach { category -> categoryList.add(category.categoryName) }
                displayProductsInReceipt()
            }
        }

        recyclerView = findViewById(R.id.receiptItemsRecyclerView)
        recyclerView.adapter = productAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val productStr: String? = intent.extras?.getString(ReceiptListActivity.EXTRA_MESSAGE)
        receiptId = productStr?.toLong()
    }

    private fun displayProductsInReceipt() {
        if (receiptId == null)
            return

        receiptProductViewModel.productsInReceipt(listOf( receiptId!!)).observe(this) { receiptWProduct ->
            receiptWProduct.forEach {r ->
                productAdapter.submitList(r.products)
                receiptNameView.text = r.receipt.receiptName
                receiptDateView.text = r.receipt.receiptDate.toString()
                receiptPriceView.text = resources.getString(R.string.receipt_total_price, r.receipt.receiptPrice.toString())
            }
        }
    }
}