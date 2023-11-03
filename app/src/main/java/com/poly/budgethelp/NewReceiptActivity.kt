package com.poly.budgethelp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
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
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.snackbar.Snackbar
import com.poly.budgethelp.config.UserConfig
import com.poly.budgethelp.utility.DateUtils
import com.poly.budgethelp.utility.TextUtils
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader

class NewReceiptActivity : AppCompatActivity() {

    private val BUNDLE_PRODUCT_DATA: String = "productData"
    private val BUNDLE_RECEIPT_DATE: String = "receiptDate"

    private var receiptName: String = ""
    private var receiptDate: Long = 0L
    private var receiptPrice: Float = 0f
    private val productsInReceipt: ArrayList<Product> = ArrayList()
    private val dataSet = arrayListOf<RecyclerViewItem>()

    private lateinit var itemListAdapter: ReceiptProductAdapter
    lateinit var categoryArray: Array<String>

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

        receiptDateButton.setOnClickListener {_ ->
            disableSoftKeyboard()
            createCalendarPopup()
        }

        itemListAdapter = ReceiptProductAdapter()

        categoryViewModel.allCategories.observe(this) { categories ->
            categories.let {
                categoryArray = Array(it.count()) { i -> it[i].categoryName }
                itemListAdapter.notifyDataSetChanged()
            }
        }

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
                    .setAction(resources.getString(R.string.generic_reply_undo)) {
                        addProductAtPosition(toDelete.product, position)
                    }.show()
            }
        }).attachToRecyclerView(recyclerView)

        // Parse products from camera/file only if state is null (otherwise we'll lose all product edits)
        if (savedInstanceState == null) {
            val loadTempFile: Boolean? = intent.extras?.getBoolean(EXTRA_LOAD_PRODUCTS)
            if (loadTempFile!!) {
                val products = loadTemporaryReceipt(this)
                if (products.isEmpty()) {
                    Toast.makeText(this, resources.getString(R.string.error_loading_temporary_receipt),
                        Toast.LENGTH_SHORT).show()
                } else {
                    productsInReceipt.addAll(products)
                    refreshProductList()
                }
            }

            val text: String? = intent.extras?.getString(CameraActivity.EXTRA_MESSAGE)
            parseProductsFromCamera(text)
        }

        val saveButton: Button = findViewById(R.id.newReceiptSaveButton)
        saveButton.setOnClickListener {_ ->
            if (productsInReceipt.size <= 0) {
                Toast.makeText(this, resources.getString(R.string.error_no_products_in_receipt), Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {saveReceipt()}
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (currentPopups.size > 0) {
                val popup = currentPopups[currentPopups.size - 1]
                currentPopups.remove(popup)
                popup.dismiss()
            } else {
                if (productsInReceipt.size > 0)
                    requestActivityFinish()
                else
                    finish()
            }
        }

        val returnButton: View = findViewById(R.id.newReceiptReturnButton)
        returnButton.setOnClickListener {_ ->
            if (currentPopups.size <= 0) {
                if (productsInReceipt.size > 0)
                    requestActivityFinish()
                else
                    finish()
            }
        }

        val cameraButton: View = findViewById(R.id.newReceiptCameraActivity)
        cameraButton.setOnClickListener { _ ->
            if (currentPopups.size <= 0) {
                requestCameraActivity()
            }
        }
        
        val fillCategoryButton: View = findViewById(R.id.newReceiptMoreOptions)
        fillCategoryButton.setOnClickListener { _ ->
            if (currentPopups.size <= 0) {
                createMorePopup()
            }
        }

        if (ActivityUtils.isUsingNightModeResources(this)) {
            returnButton.background.colorFilter =
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP)
        }

        // Read data from save bundle:
        if (savedInstanceState != null) {
            val productData: String? = savedInstanceState.getString(BUNDLE_PRODUCT_DATA)
            val products = parseProductSaveString(productData!!)
            productsInReceipt.addAll(products)
            refreshProductList()

            receiptDate = savedInstanceState.getLong(BUNDLE_PRODUCT_DATA)
            receiptDateButton.setText(DateUtils.longToDateString(receiptDate))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val productText: String = generateProductBundleString()
        outState.putString(BUNDLE_PRODUCT_DATA, productText)
        outState.putLong(BUNDLE_RECEIPT_DATE, receiptDate)
    }

    override fun onDestroy() {
        super.onDestroy()
        for (popup in currentPopups) {
            popup.dismiss()
        }
        currentPopups.clear()
    }

    private fun createMorePopup() {
        val builder = AlertDialog.Builder(this, R.style.AlertDialog)
        builder.setCancelable(false)

        builder.setTitle(resources.getString(R.string.new_receipt_fix_options_header))

        val selectionItems = arrayOf(
            resources.getString(R.string.new_receipt_fix_similarity),
            resources.getString(R.string.new_receipt_fix_guess_category))
        val actionArray = arrayOf(::fixProducts, ::autoFillCategories)

        builder.setItems(selectionItems) {dialog, index ->
            actionArray[index]()
            dialog.dismiss()
        }

        builder.setNegativeButton(resources.getString(R.string.generic_reply_negative)) { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun generateProductBundleString(): String {
        val builder: StringBuilder = StringBuilder()
        for ( i in productsInReceipt.indices) {
            builder.append(productsInReceipt[i].productName).append(saveFileDelimiter)
                .append(productsInReceipt[i].productCategory).append(saveFileDelimiter)
                .append(productsInReceipt[i].productPrice)

            if (i < productsInReceipt.size - 1)
                builder.append(System.lineSeparator())
        }

        return builder.toString()
    }

    private fun requestActivityFinish() {
        val builder = AlertDialog.Builder(this, R.style.AlertDialog)
        builder.setTitle(resources.getString(R.string.new_receipt_request_exit_header))
        builder.setCancelable(false)

        builder.setMessage(resources.getString(R.string.new_receipt_request_exit_message, productsInReceipt.size))
        builder.setPositiveButton(resources.getString(R.string.generic_reply_positive)) {dialogInterface, _ ->
            finish()
            dialogInterface.dismiss()
        }

        builder.setNegativeButton(resources.getString(R.string.generic_reply_negative)) { dialogInterface, _ ->
            dialogInterface.dismiss()
        }

        builder.setNeutralButton(resources.getString(R.string.new_receipt_create_temp_receipt)) { dialogInterface, _ ->
            val success: Boolean = saveTemporaryReceipt(this, productsInReceipt,
                TextUtils.sanitizeText(receiptNameEdit.text.toString()), receiptDate)
            dialogInterface.dismiss()

            if (success) {
                Toast.makeText(this, resources.getString(R.string.new_receipt_temp_receipt_created), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, resources.getString(R.string.error_creating_temporary_receipt), Toast.LENGTH_SHORT).show()
            }
        }

        builder.show()
    }

    private fun requestCameraActivity() {
        val builder = AlertDialog.Builder(this, R.style.AlertDialog)
        builder.setTitle(R.string.new_receipt_continue_with_camera_header)
        builder.setMessage(R.string.new_receipt_continue_with_camera_message)
        builder.setPositiveButton(R.string.new_product_confirm) {dialogInterface, _ ->
            saveTemporaryReceipt(this, productsInReceipt,
                TextUtils.sanitizeText(receiptNameEdit.text.toString()), receiptDate)
            dialogInterface.dismiss()

            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra(EXTRA_LOAD_PRODUCTS, true)
            this.startActivity(intent)
            finish()
        }
        builder.setNegativeButton(R.string.generic_reply_negative) {dialogInterface, _ ->
            dialogInterface.dismiss()
        }

        builder.show()
    }

    fun addNewProduct(product: Product) {
        productsInReceipt.add(product)
        dataSet.clear()
        productsInReceipt.forEach {prod -> dataSet.add(ContentItem(prod, this))}
        dataSet.add(AddItem(this))
        itemListAdapter.notifyDataSetChanged()
        calculateTotalPrice()
    }

    private fun refreshProductList() {
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
        itemListAdapter.notifyItemRangeChanged(position, dataSet.size)
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
        itemListAdapter.notifyItemRangeChanged(position, dataSet.size)
        calculateTotalPrice()
    }

    fun modifyItemAtPosition(position: Int, newData: Product) {
        productsInReceipt[position] = newData
        dataSet.clear()
        productsInReceipt.forEach {prod -> dataSet.add(ContentItem(prod, this))}
        dataSet.add(AddItem(this))
        itemListAdapter.notifyItemChanged(position)
        calculateTotalPrice()
    }

    fun addNewCategory(categoryName: String) {
        if (categoryName.isEmpty() || categoryName.isBlank() || categoryName == AppRoomDatabase.ADD_CATEGORY_TEXT) {
            Toast.makeText(this, resources.getString(R.string.error_invalid_category), Toast.LENGTH_SHORT).show()
            return
        }
        val category = Category(categoryName)
        categoryViewModel.insert(category)
        Toast.makeText(this, resources.getString(R.string.new_category_added), Toast.LENGTH_SHORT).show()
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
            val newProducts: ArrayList<Product> = arrayListOf()
            newProducts.addAll(productsInReceipt)
            for (product in existingProducts) {
                // Check for existing product
                val toAdd: Product? = productsInReceipt.find { it.productName == product.productName }
                if (toAdd != null && abs(toAdd.productPrice - product.productPrice) <= 0.01f) {
                    newProducts.remove(toAdd)
                    val crossRef = ReceiptProductCrossRef(receiptId, product.productId)
                    receiptProductViewModel.insert(crossRef)
                }
            }

            for (product in newProducts) {
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
        receiptName = TextUtils.sanitizeText(receiptNameEdit.text.toString())
        if (receiptName.isBlank())
            receiptName = resources.getString(R.string.receipt_default_name,
                DateUtils.longToDateString(System.currentTimeMillis()))
        if (receiptDate <= 0)
            receiptDate = System.currentTimeMillis()
    }

    private fun parseProductsFromCamera(text: String?) {
        if (text == null) {
            // Initialize price amount to 0
            calculateTotalPrice()
            return
        }

        val previousProducts: List<String> = productsInReceipt.map {product ->
            product.productName
        }

        Log.d(TAG, "Parsing products from camera")
        val pairs: List<String> = text.split(System.lineSeparator())

        val productNames: ArrayList<String> = ArrayList()
        val prices: ArrayList<Float> = ArrayList()
        val nameWithCategory: HashMap<String, String> = HashMap()

        for (pair in pairs) {
            val nameAndPrice: List<String> = pair.split(saveFileDelimiter)
            if (nameAndPrice.size != 2)
                continue

            val name = nameAndPrice[0]
            val price: Float = nameAndPrice[1].toFloat()

            productNames.add(name)
            prices.add(price)
        }

        val liveData = productViewModel.productsWithNames(productNames)
        liveData.observe(this, object: Observer<List<Product>> {
            override fun onChanged(products: List<Product>?) {
                liveData.removeObserver(this)
                if (products == null)
                    return
                products.let {
                    for (product in it) {
                        if (productNames.contains(product.productName)) {
                            nameWithCategory[product.productName] = product.productCategory
                        }
                    }

                    // Populate products list
                    for (i in 0 until productNames.size) {

                        // Don't re-add product if it was already added before (from save state)
                        if (previousProducts.contains(productNames[i]))
                            continue

                        var category: String? = nameWithCategory[productNames[i]]
                        if (category == null) category = AppRoomDatabase.DEFAULT_CATEGORY
                        val product = Product(productNames[i], category, prices[i])
                        Log.d(TAG, "Adding product from camera: " + product.productName)

                        addNewProduct(product)
                    }
                }
            }
        })
    }

    private fun createLoadPopup(loadText: String = resources.getString(R.string.load_save_receipt)) {
        clearPopups()
        val popupData = ActivityUtils.createPopup(R.layout.popup_loading, this)
        val loadTextView: TextView = popupData.first.findViewById(R.id.loadPopupDescription)
        loadTextView.text = loadText
        currentPopups.add(popupData.second)
    }

    private fun clearPopups() {
        for (popup in currentPopups) {
            popup.dismiss()
        }
        currentPopups.clear()
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
            receiptDateButton.setText(DateUtils.longToDateString(receiptDate))
            popupWindow.dismiss()
        }

        popupWindow.setOnDismissListener {
            currentPopups.remove(popupWindow)
        }

        currentPopups.add(popupWindow)
    }

    private fun fixProducts() {
        if (productsInReceipt.isEmpty()) {
            Toast.makeText(this, resources.getString(R.string.error_no_products_to_process), Toast.LENGTH_SHORT).show()
            return
        }

        createLoadPopup(resources.getString(R.string.load_fix_products))
        val checkMax = productsInReceipt.size
        var checkedCount = 0
        for (i in productsInReceipt.indices) {
            // Don't modify already user edited products
            if (productsInReceipt[i].productCategory != AppRoomDatabase.DEFAULT_CATEGORY) {
                checkedCount++
                continue
            }

            val searchTerm = "${productsInReceipt[i].productName.first()}%"
            val liveData = productViewModel.getProductsStartingWith(searchTerm)
            liveData.observe(this, object: Observer<List<Product>> {
                override fun onChanged(t: List<Product>?) {
                    checkedCount++
                    liveData.removeObserver(this)
                    if (t != null) {
                        estimateSimilarity(i, t)
                    }

                    // Disable load popup
                    if (checkedCount >= checkMax) {
                        clearPopups()
                        Toast.makeText(applicationContext, resources.getString(R.string.new_receipt_fix_similarity_complete), Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun estimateSimilarity(index: Int, products: List<Product>) {
        val toCheck = productsInReceipt[index]

        var bestProduct = toCheck
        var bestScore = UserConfig.similarityRequirement
        Log.d(TAG, "Similarity requirement is: $bestScore")

        for (product in products) {
            val toCheckName = toCheck.productName.replace(" ", "")
            val productNameTrimmed = product.productName.replace(" ", "")

            // Extremely big differences in length should never be equal
            if (abs(toCheckName.length - productNameTrimmed.length) > 10)
                continue

            // Calculate similarity using Jaro algorithm
            val similarity = TextUtils.jaroDistance(toCheckName, productNameTrimmed)

            if (similarity > bestScore) {
                bestProduct = product
                bestScore = similarity
                if (bestScore >= 0.99)
                    break
            }
        }

        if (bestProduct == toCheck)
            return

        // Create new product based on similarity info, but keep price intact
        val newProduct = Product(bestProduct.productName, bestProduct.productCategory, toCheck.productPrice)
        productsInReceipt[index] = newProduct
        modifyItemAtPosition(index, newProduct)
    }

    private fun autoFillCategories() {
        if (productsInReceipt.isEmpty()) {
            Toast.makeText(this, resources.getString(R.string.error_no_products_to_process), Toast.LENGTH_SHORT).show()
            return
        }

        for (i in productsInReceipt.indices) {
            // Only update products that have not had their category set
            if (productsInReceipt[i].productCategory != AppRoomDatabase.DEFAULT_CATEGORY)
                continue

            val words: List<String> = productsInReceipt[i].productName.split(" ", "-")
            val searchTerms = words.map { term -> "%$term%" }

            if (searchTerms.isNotEmpty())
                guessProductCategory(i, searchTerms)
        }
    }

    private fun guessProductCategory(index: Int, searchTerms: List<String>) {
        val liveData = productViewModel.productsWithNameContaining(searchTerms)
        liveData.observe(this, object: Observer<List<Product>> {
            override fun onChanged(t: List<Product>?) {
                liveData.removeObserver(this)

                if (!t.isNullOrEmpty()) {
                    Log.d(TAG, "Found $t.size products similar to this")
                    // Map categories
                    val categoryMap: HashMap<String, Int> = hashMapOf()
                    for (product in t) {
                        val category = product.productCategory
                        if (category == AppRoomDatabase.DEFAULT_CATEGORY)
                            continue

                        if (categoryMap.containsKey(category)) {
                            val currentVal = categoryMap[category]!!
                            categoryMap[category] = currentVal + 1
                        } else {
                            categoryMap[category] = 1
                        }
                    }

                    var bestCategory = AppRoomDatabase.DEFAULT_CATEGORY
                    var bestCount = 0
                    for (category in categoryMap.keys) {
                        if (categoryMap[category]!! > bestCount) {
                            bestCategory = category
                            bestCount = categoryMap[category]!!
                        }
                    }

                    if (bestCategory != AppRoomDatabase.DEFAULT_CATEGORY) {
                        val refProduct = productsInReceipt[index]
                        val newProduct = Product(refProduct.productName, bestCategory, refProduct.productPrice)
                        modifyItemAtPosition(index, newProduct)
                    }
                }
            }
        })
    }

    companion object {
        private const val TAG = "NewReceiptActivity"
        const val EXTRA_LOAD_PRODUCTS: String = "NewReceiptSavedProducts"
        private const val tempSaveFileName: String = "tempReceipt"
        const val saveFileDelimiter: String = "|"
        fun saveTemporaryReceipt(context: AppCompatActivity, products: List<Product>, name: String, date: Long): Boolean {
            val outputStream: FileOutputStream
            try {
                outputStream = context.openFileOutput(tempSaveFileName, Context.MODE_PRIVATE)
                val settingsBuilder = StringBuilder()

                // Receipt file header: Storing receipt name and date
                settingsBuilder.append(name).append(saveFileDelimiter).append(date).append(System.lineSeparator())

                // Save products
                for (i in products.indices) {
                    settingsBuilder.append(products[i].productName).append(saveFileDelimiter)
                        .append(products[i].productCategory).append(saveFileDelimiter)
                        .append(products[i].productPrice)
                        if (i < products.size - 1)
                            settingsBuilder.append(System.lineSeparator())
                }

                outputStream.write(settingsBuilder.toString().encodeToByteArray())
                outputStream.close()
                Log.d(TAG, "Saved temporary receipt successfully")
                return true
            }
            catch(ex: Exception) {
                ex.printStackTrace()
                return false
            }
        }

        fun loadTemporaryReceipt(context: AppCompatActivity): List<Product> {
            val products: ArrayList<Product> = arrayListOf()
            val inputStream: FileInputStream
            try {
                inputStream = context.openFileInput(tempSaveFileName)
                val inputReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputReader)

                val fileContent = bufferedReader.readLines()
                for (i in fileContent.indices) {
                    if (i == 0) {
                        // Line 0 -> Header for receipt
                        val lineData = fileContent[i].split(saveFileDelimiter)
                        if (lineData.size == 2) {
                            val name = lineData[0]
                            val date: Long? = lineData[1].toLongOrNull()

                            val activity = context as NewReceiptActivity
                            activity.receiptNameEdit.setText(name)
                            if (date != null && date > 0) {
                                activity.receiptDate = date
                                activity.receiptDateButton.setText(DateUtils.longToDateString(date))
                            }
                        }

                    } else {
                        // Line 1..n -> Products on receipt
                        val lineData = fileContent[i].split(saveFileDelimiter)
                        if (lineData.size == 3) {
                            val productName = lineData[0]
                            val productCategory = lineData[1]
                            val productPrice = lineData[2].toFloat()
                            products.add(Product(productName, productCategory, productPrice))
                        }
                    }
                }

                // Delete temporary file on load
                inputStream.close()
                context.deleteFile(tempSaveFileName)

            } catch (ex: Exception) {
                ex.printStackTrace()
            }

            Log.d(TAG, "Load temporary receipt complete, found ${products.size} products")
            return products
        }

        private fun parseProductSaveString(productData: String): List<Product> {
            // save data is in format product|category|price, one product per line
            val products: ArrayList<Product> = arrayListOf()
            val lines = productData.split(System.lineSeparator())
            for (line in lines) {
                val lineContents = line.split(saveFileDelimiter)
                if (lineContents.size == 3) {
                    val productName = lineContents[0]
                    val productCategory = lineContents[1]
                    val productPrice = lineContents[2].toFloat()
                    products.add(Product(productName, productCategory, productPrice))
                }
            }

            return products
        }
    }
}