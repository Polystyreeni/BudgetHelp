package com.poly.budgethelp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.adapter.AddItem
import com.poly.budgethelp.adapter.ContentItem
import com.poly.budgethelp.adapter.ReceiptProductAdapter
import com.poly.budgethelp.adapter.RecyclerViewItem
import com.poly.budgethelp.data.Category
import com.poly.budgethelp.data.Product
import com.poly.budgethelp.data.Receipt
import com.poly.budgethelp.data.ReceiptProductCrossRef
import com.poly.budgethelp.db.AppRoomDatabase
import com.poly.budgethelp.utility.ActivityUtils
import com.poly.budgethelp.viewmodel.CategoryViewModel
import com.poly.budgethelp.viewmodel.CategoryViewModelFactory
import com.poly.budgethelp.viewmodel.ProductViewModel
import com.poly.budgethelp.viewmodel.ProductViewModelFactory
import com.poly.budgethelp.viewmodel.ReceiptProductViewModel
import com.poly.budgethelp.viewmodel.ReceiptProductViewModelFactory
import com.poly.budgethelp.viewmodel.ReceiptViewModel
import com.poly.budgethelp.viewmodel.ReceiptViewModelFactory
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import kotlin.math.abs
import androidx.activity.addCallback
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.snackbar.Snackbar
import com.poly.budgethelp.config.UserConfig

class NewReceiptActivity : AppCompatActivity() {

    private val TAG: String = "NewRecipeActivity"

    private var receiptName: String = ""
    private var receiptDate: Long = 0L
    private var receiptPrice: Float = 0f
    private val productsInReceipt: ArrayList<Product> = ArrayList()
    private val dataSet = arrayListOf<RecyclerViewItem>()

    private lateinit var itemListAdapter: ReceiptProductAdapter
    lateinit var categoryAdapter: ArrayAdapter<String>

    private lateinit var recyclerView: RecyclerView
    private lateinit var totalPriceText: TextView
    private lateinit var receiptNameEdit: EditText

    private lateinit var receiptDateButton: EditText

    val currentPopups = arrayListOf<PopupWindow>()

    // Save state
    private val existingProductsCheck: CompletableJob = Job()
    private val existingProducts = arrayListOf<Product>()

    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModelFactory((application as BudgetApplication).productRepository)
    }

    private val categoryViewModel: CategoryViewModel by viewModels {
        CategoryViewModelFactory((application as BudgetApplication).categoryRepository)
    }

    private val receiptViewModel: ReceiptViewModel by viewModels {
        ReceiptViewModelFactory((application as BudgetApplication).receiptRepository)
    }

    private val receiptProductViewModel: ReceiptProductViewModel by viewModels {
        ReceiptProductViewModelFactory((application as BudgetApplication).receiptProductRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_receipt)

        recyclerView = findViewById(R.id.productRecyclerView)
        totalPriceText = findViewById(R.id.receiptPriceTextView)

        receiptNameEdit = findViewById(R.id.newReceiptNameEditText)
        receiptDateButton = findViewById(R.id.newReceiptDateButton)

        receiptNameEdit.setOnFocusChangeListener {_, _ ->
            Log.d(TAG, "Receipt name is: " + receiptNameEdit.text.toString())
            receiptName = receiptNameEdit.text.toString()
        }

        receiptDateButton.setOnClickListener {_ ->
            // Some hackery for hiding soft keyboard
            // https://stackoverflow.com/questions/1109022/how-to-close-hide-the-android-soft-keyboard-programmatically
            disableSoftKeyboard()
            createCalendarPopup()
        }

        itemListAdapter = ReceiptProductAdapter()
        val categoryList: ArrayList<String> = ArrayList()
        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryList)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoryViewModel.allCategories.observe(this) { categories ->
            categoryList.clear()
            categories.let {
                it.forEach { category -> categoryList.add(category.categoryName) }
                // Sum character = add new category button
                categoryList.add("+")
                itemListAdapter.notifyDataSetChanged()
            }
        }

        // val dataSet = arrayListOf<RecyclerViewItem>()
        itemListAdapter.data = dataSet
        recyclerView.adapter = itemListAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        dataSet.add(AddItem(this))

        // Swipe to delete functionality
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                if (dataSet[viewHolder.adapterPosition] is AddItem) return 0
                return super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
               // Don't delete the add button
                if (dataSet[viewHolder.adapterPosition] is AddItem) {
                    return
                }

                val position = viewHolder.adapterPosition
                val toDelete: ContentItem = dataSet[viewHolder.adapterPosition] as ContentItem

                removeProductAtPosition(toDelete.product, position)

                // Possibility to undo this delete action
                Snackbar.make(recyclerView, resources.getString(R.string.delete_from_new_receipt, toDelete.product.productName), Snackbar.LENGTH_LONG)
                    .setAction(resources.getString(R.string.generic_reply_undo), View.OnClickListener {
                        addProductAtPosition(toDelete.product, position)
                    }).show()
            }
        }).attachToRecyclerView(recyclerView)

        val text: String? = intent.extras?.getString(CameraActivity.EXTRA_MESSAGE)
        parseProductsFromCamera(text)

        val saveButton: Button = findViewById(R.id.newReceiptSaveButton)
        saveButton.setOnClickListener {_ ->
            lifecycleScope.launch {saveReceipt()}
        }

        onBackPressedDispatcher.addCallback(this) {
            if (currentPopups.size > 0) {
                val popup = currentPopups[currentPopups.size - 1]
                currentPopups.remove(popup)
                popup.dismiss()
                Log.d(TAG, "Popups size: " + currentPopups.size)
            } else {
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        for (popup in currentPopups) {
            popup.dismiss()
        }
        currentPopups.clear()
    }

    fun addNewProduct(product: Product) {
        productsInReceipt.add(product)
        dataSet.clear()
        productsInReceipt.forEach {prod -> dataSet.add(ContentItem(prod, this))}
        dataSet.add(AddItem(this))
        itemListAdapter.notifyDataSetChanged()
        calculateTotalPrice()
    }

    fun addProductAtPosition(product: Product, position: Int) {
        productsInReceipt.add(position, product)
        dataSet.add(position, ContentItem(product, this))
        itemListAdapter.notifyItemInserted(position)
        calculateTotalPrice()
    }

    fun removeProduct(product: Product) {
        productsInReceipt.remove(product)
        dataSet.clear()
        productsInReceipt.forEach {prod -> dataSet.add(ContentItem(prod, this))}
        dataSet.add(AddItem(this))
        itemListAdapter.notifyDataSetChanged()
        calculateTotalPrice()
    }

    fun removeProductAtPosition(product: Product, position: Int) {
        productsInReceipt.remove(product)
        dataSet.removeAt(position)
        itemListAdapter.notifyItemRemoved(position)
        calculateTotalPrice()
    }

    fun modifyItemAtPosition(position: Int, newData: Product) {
        productsInReceipt[position] = newData
        dataSet.clear()
        productsInReceipt.forEach {prod -> dataSet.add(ContentItem(prod, this))}
        dataSet.add(AddItem(this))
        itemListAdapter.notifyItemChanged(position)
        // itemListAdapter.notifyDataSetChanged()
        calculateTotalPrice()
    }

    fun addNewCategory(categoryName: String) {
        if (categoryName.isEmpty() || categoryName.isBlank() || categoryName == "+") {
            Toast.makeText(this, resources.getString(R.string.error_invalid_category), Toast.LENGTH_SHORT).show()
            return
        }
        val category = Category(categoryName)
        categoryViewModel.insert(category)
    }

    fun removePopup(popup: PopupWindow) {
        val latest = currentPopups[currentPopups.size - 1]
        latest.dismiss()
        currentPopups.remove(latest)
    }

    fun disableSoftKeyboard() {
        // Some hackery for hiding soft keyboard
        // https://stackoverflow.com/questions/1109022/how-to-close-hide-the-android-soft-keyboard-programmatically
        this.currentFocus?.let { view ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
        this.currentFocus?.clearFocus()
    }

    private fun calculateTotalPrice() {
        var price = 0f
        for (product in productsInReceipt) {
            price += product.productPrice
        }

        totalPriceText.text = resources.getString(R.string.receipt_total_price, price, UserConfig.currency)
        receiptPrice = price
    }

    private suspend fun saveReceipt() {
        checkInput()

        createLoadPopup()

        Log.d(TAG, "Begin receipt save")

        val receipt = Receipt(receiptName, receiptDate, receiptPrice)
        val receiptId = receiptViewModel.insert(receipt)

        Log.d(TAG, "Receipt successfully created")

        val itemNames: List<String> = productsInReceipt.map { product -> product.productName }

        checkExistingProducts(itemNames)

        joinAll(existingProductsCheck)

        val job = lifecycleScope.launch {
            Log.d(TAG, "Add products coroutine started")
            val newProducts: ArrayList<Product> = arrayListOf()
            newProducts.addAll(productsInReceipt)
            for (product in existingProducts) {
                // Check for existing product
                val toAdd: Product? = productsInReceipt.find { it.productName == product.productName }
                if (toAdd != null && abs(toAdd.productPrice - product.productPrice) <= 0.01f) {
                    Log.d(TAG, "Database already contains product " + product.productName)
                    newProducts.remove(toAdd)
                    val crossRef = ReceiptProductCrossRef(receiptId, product.productId)
                    receiptProductViewModel.insert(crossRef)
                }
                else {
                    Log.d(TAG, "Inserting new product " + product.productName)
                }
            }

            for (product in newProducts) {
                Log.d(TAG, "Adding new item to database: " + product.productName)
                val productId = productViewModel.insert(product)
                val crossRef = ReceiptProductCrossRef(receiptId, productId)
                receiptProductViewModel.insert(crossRef)
            }
        }

        job.join()

        Log.d(TAG, "Add products coroutine finished")

        Toast.makeText(this, resources.getString(R.string.receipt_save_successful), Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun checkExistingProducts(itemNames: List<String>) {
        productViewModel.productsWithNames(itemNames).observe(this) {
                it.let { products ->
                    existingProducts.addAll(products)
                    existingProductsCheck.complete()
                    Log.d(TAG, "Existing product job done, found " + products.size + " products")
                }
        }
    }

    private fun checkInput() {
        receiptName = receiptNameEdit.text.toString()
        if (receiptName.isBlank())
            receiptName = "Kuitti"
        if (receiptDate <= 0)
            receiptDate = System.currentTimeMillis()
    }

    private fun parseProductsFromCamera(text: String?) {
        if (text == null) {
            // Initialize price amount to 0
            calculateTotalPrice()
            return
        }

        Log.d(TAG, "Parsing products from camera")
        val pairs: List<String> = text.split(System.lineSeparator())

        val productNames: ArrayList<String> = ArrayList()
        val prices: ArrayList<Float> = ArrayList()
        val nameWithCategory: HashMap<String, String> = HashMap()

        for (pair in pairs) {
            val nameAndPrice: List<String> = pair.split(":")
            if (nameAndPrice.size != 2)
                continue

            val name = nameAndPrice[0]
            val price: Float = nameAndPrice[1].toFloat()

            productNames.add(name)
            prices.add(price)
        }

        productViewModel.productsWithNames(productNames).observe(this) {products ->
            products.let {
                for (product in it) {
                    if (productNames.contains(product.productName)) {
                        nameWithCategory[product.productName] = product.productCategory
                    }
                }

                // Populate products list
                for (i in 0 until productNames.size) {
                    var category: String? = nameWithCategory[productNames[i]]
                    if (category == null) category = AppRoomDatabase.DEFAULT_CATEGORY
                    val product = Product(productNames[i], category, prices[i])
                    Log.d(TAG, "Adding product from camera: " + product.productName)
                    addNewProduct(product)
                }
            }
        }
    }

    private fun createLoadPopup() {
        for (popup in currentPopups) {
            popup.dismiss()
        }
        currentPopups.clear()
        val popupData = ActivityUtils.createPopup(R.layout.popup_loading, this)
        val loadTextView: TextView = popupData.first.findViewById(R.id.loadPopupDescription)
        loadTextView.setText(R.string.load_save_receipt)
        currentPopups.add(popupData.second)
    }

    private fun createCalendarPopup() {
        val popupView = LayoutInflater.from(this).inflate(R.layout.popup_calendar, null)
        val width: Int = LinearLayout.LayoutParams.WRAP_CONTENT
        val height: Int = LinearLayout.LayoutParams.WRAP_CONTENT

        val popupWindow = PopupWindow(popupView, width, height, true)
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

        val popupHeader: TextView = popupView.findViewById(R.id.calendarPopupHeader)
        val calendarView: CalendarView = popupView.findViewById(R.id.calendarPopupView)

        // Assign header text
        popupHeader.text = resources.getString(R.string.hint_receipt_date)

        // Set calendar range
        calendarView.maxDate = System.currentTimeMillis()
        calendarView.date = System.currentTimeMillis()

        calendarView.setOnDateChangeListener {_, y, m, d ->
            val calendar: Calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, y)
            calendar.set(Calendar.MONTH, m)
            calendar.set(Calendar.DAY_OF_MONTH, d)
            val date: Date = calendar.time
            receiptDate = date.time
            receiptDateButton.setText(String.format("%d.%d %d", d, m + 1, y))
            popupWindow.dismiss()
        }

        popupWindow.setOnDismissListener {
            currentPopups.remove(popupWindow)
        }

        currentPopups.add(popupWindow)
    }
}