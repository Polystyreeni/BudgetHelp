package com.poly.budgethelp

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.adapter.ProductAdapter
import com.poly.budgethelp.config.UserConfig
import com.poly.budgethelp.utility.ActivityUtils
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
    private lateinit var returnButton: View
    private lateinit var deleteButton: Button
    private lateinit var copyButton: View

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
        copyButton = findViewById(R.id.receiptInfoCopyButton)

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
        copyButton.setOnClickListener { requestReceiptCopy() }

        if (ActivityUtils.isUsingNightModeResources(this)) {
            returnButton.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP)
        }
    }

    private fun displayProductsInReceipt() {
        if (receiptId == null)
            return

        receiptProductViewModel.productsInReceipt(listOf( receiptId!!)).observe(this) { receiptWProduct ->
            receiptWProduct.forEach {r ->
                productAdapter.submitList(r.products)
                receiptNameView.text = r.receipt.receiptName
                receiptDateView.text = DateUtils.longToDateString(r.receipt.receiptDate)
                receiptPriceView.text = resources.getString(R.string.receipt_total_price, r.receipt.receiptPrice, UserConfig.currency)
            }
        }
    }

    private fun requestReceiptDelete() {
        val builder = AlertDialog.Builder(this, R.style.AlertDialog)
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

    private fun requestReceiptCopy() {
        val builder = AlertDialog.Builder(this, R.style.AlertDialog)
        builder.setTitle(resources.getString(R.string.copy_receipt_products_header))
        builder.setCancelable(false)

        builder.setMessage(resources.getString(R.string.copy_receipt_products_message))
        builder.setPositiveButton(resources.getString(R.string.generic_reply_positive)) {dialogInterface, _ ->
            startNewReceiptActivity()
            dialogInterface.dismiss()
        }

        builder.setNegativeButton(resources.getString(R.string.generic_reply_negative)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }

        builder.show()
    }

    private fun startNewReceiptActivity() {
        val productData = StringBuilder()
        val productList = productAdapter.currentList

        productList.forEach { product ->
            productData.append(product.productName)
                .append(NewReceiptActivity.saveFileDelimiter)
                .append(product.productPrice)
                .append(System.lineSeparator())
        }

        val intent = Intent(this, NewReceiptActivity::class.java)
        intent.putExtra(CameraActivity.EXTRA_MESSAGE, productData.toString())
        startActivity(intent)
        finish()
    }

    private fun deleteReceipt() {
        if (receiptId == null) return

        lifecycleScope.launch {
            receiptViewModel.deleteReceiptWithId(receiptId!!)
            finish()
        }
    }
}