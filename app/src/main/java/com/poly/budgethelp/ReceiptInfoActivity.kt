package com.poly.budgethelp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.adapter.ProductAdapter
import com.poly.budgethelp.utility.DateUtils
import com.poly.budgethelp.viewmodel.CategoryViewModel
import com.poly.budgethelp.viewmodel.CategoryViewModelFactory
import com.poly.budgethelp.viewmodel.ReceiptProductViewModel
import com.poly.budgethelp.viewmodel.ReceiptProductViewModelFactory
import com.poly.budgethelp.viewmodel.ReceiptViewModel
import com.poly.budgethelp.viewmodel.ReceiptViewModelFactory
import kotlinx.coroutines.launch

class ReceiptInfoActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    lateinit var categoryAdapter: ArrayAdapter<String>
    private var receiptId: Long? = null

    private lateinit var receiptNameView: TextView
    private lateinit var receiptDateView: TextView
    private lateinit var receiptPriceView: TextView
    private lateinit var returnButton: TextView
    private lateinit var deleteButton: Button

    // View models
    private val categoryViewModel: CategoryViewModel by viewModels {
        CategoryViewModelFactory((application as BudgetApplication).categoryRepository)
    }

    private val receiptProductViewModel: ReceiptProductViewModel by viewModels {
        ReceiptProductViewModelFactory((application as BudgetApplication).receiptProductRepository)
    }

    private val receiptViewModel: ReceiptViewModel by viewModels {
        ReceiptViewModelFactory((application as BudgetApplication).receiptRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt_info)

        receiptNameView = findViewById(R.id.receiptInfoNameText)
        receiptDateView = findViewById(R.id.receiptInfoDateText)
        receiptPriceView = findViewById(R.id.receiptInfoPriceText)
        returnButton = findViewById(R.id.receiptInfoReturnButton)
        deleteButton = findViewById(R.id.receiptInfoDeleteButton)

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

        returnButton.setOnClickListener { finish() }

        deleteButton.setOnClickListener { requestReceiptDelete() }
    }

    private fun displayProductsInReceipt() {
        if (receiptId == null)
            return

        receiptProductViewModel.productsInReceipt(listOf( receiptId!!)).observe(this) { receiptWProduct ->
            receiptWProduct.forEach {r ->
                productAdapter.submitList(r.products)
                receiptNameView.text = r.receipt.receiptName
                receiptDateView.text = DateUtils.longToDateString(r.receipt.receiptDate)
                receiptPriceView.text = resources.getString(R.string.receipt_total_price, r.receipt.receiptPrice.toString())
            }
        }
    }

    private fun requestReceiptDelete() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.delete_receipt_header))
        builder.setCancelable(false)

        builder.setMessage(resources.getString(R.string.delete_receipt_message))
        builder.setPositiveButton(resources.getString(R.string.generic_reply_positive)) {dialogInterface, _ ->
            deleteReceipt()
            dialogInterface.dismiss()
        }

        builder.setNegativeButton(resources.getString(R.string.generic_reply_negative)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }

        builder.show()
    }

    private fun deleteReceipt() {
        if (receiptId == null) return

        lifecycleScope.launch {
            receiptViewModel.deleteReceiptWithId(receiptId!!)
            finish()
        }
    }
}