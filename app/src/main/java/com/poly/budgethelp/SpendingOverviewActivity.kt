package com.poly.budgethelp

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.adapter.SpendingItemAdapter
import com.poly.budgethelp.config.UserConfig
import com.poly.budgethelp.data.CategoryPricePojo
import com.poly.budgethelp.data.DuplicateMapItem
import com.poly.budgethelp.data.Product
import com.poly.budgethelp.data.Receipt
import com.poly.budgethelp.data.ReceiptWithProducts
import com.poly.budgethelp.data.SpendingTimeBlock
import com.poly.budgethelp.utility.ActivityUtils
import com.poly.budgethelp.utility.DateUtils
import com.poly.budgethelp.viewmodel.ProductViewModel
import com.poly.budgethelp.viewmodel.ProductViewModelFactory
import com.poly.budgethelp.viewmodel.ReceiptProductViewModel
import com.poly.budgethelp.viewmodel.ReceiptProductViewModelFactory
import com.poly.budgethelp.viewmodel.ReceiptViewModel
import com.poly.budgethelp.viewmodel.ReceiptViewModelFactory
import java.util.Calendar
import java.util.Date
import kotlin.math.abs

class SpendingOverviewActivity : AppCompatActivity() {

    private val TAG = "SpendingOverviewActivity"
    private val BUNDLE_START_DATE = "startDate"
    private val BUNDLE_END_DATE = "endDate"
    private val BUNDLE_TIME_STEP = "timeStep"

    // UI Components
    private lateinit var fetchButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: SpendingItemAdapter
    private lateinit var warningTextView: TextView

    private var startDate: Long? = null
    private var endDate: Long? = null
    private val timeStepRange = hashMapOf(Pair("Viikko", 604800000L), Pair("Kuukausi", 2678400000L))
    private var timeStep: Long? = null

    private var currentPopup: PopupWindow? = null

    // Overview state
    private val spendingBlockList: ArrayList<SpendingTimeBlock> = arrayListOf()
    private val warningLimit: Long = 15778800000L

    // Viewmodels
    private val receiptProductViewModel: ReceiptProductViewModel by viewModels {
        ReceiptProductViewModelFactory((application as BudgetApplication).receiptProductRepository)
    }

    private val receiptViewModel: ReceiptViewModel by viewModels {
        ReceiptViewModelFactory((application as BudgetApplication).receiptRepository)
    }

    private val productViewModel: ProductViewModel by viewModels {
        ProductViewModelFactory((application as BudgetApplication).productRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spending_overview)

        // Assign UI components
        fetchButton = findViewById(R.id.spendingSearchButton)
        fetchButton.setOnClickListener {
            onFetchButtonPressed()
        }

        recyclerView = findViewById(R.id.spendingRecyclerView)
        recyclerViewAdapter = SpendingItemAdapter(spendingBlockList)
        recyclerViewAdapter.baseContext = this
        recyclerView.adapter = recyclerViewAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val timeStepSpinner: Spinner = findViewById(R.id.spendingTimeStepSpinner)
        ArrayAdapter.createFromResource(this,
            R.array.spending_timestep_array,
            android.R.layout.simple_spinner_item).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                timeStepSpinner.adapter = adapter
        }

        timeStepSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                if (position == 0)
                    timeStep = null
                else
                {
                    val item: String = timeStepSpinner.getItemAtPosition(position).toString()
                    timeStep = timeStepRange[item]!!
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        }

        val startEdit: EditText = findViewById(R.id.spendingStartDate)
        val endEdit: EditText = findViewById(R.id.spendingEndDate)

        startEdit.setOnClickListener {_ ->
            createPopup(true, startEdit)
        }

        endEdit.setOnClickListener {_ ->
            createPopup(false, endEdit)
        }

        val returnButton: View = findViewById(R.id.spendingReturnButton)
        returnButton.setOnClickListener {_ ->
            if (currentPopup == null) {
                finish()
            }
        }

        warningTextView = findViewById(R.id.spendingWarningTextView)
        warningTextView.isVisible = false

        // Dark mode
        if (ActivityUtils.isUsingNightModeResources(this)) {
            returnButton.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP)
        }

        // Back button functionality
        onBackPressedDispatcher.addCallback(this) {
            if (currentPopup != null) {
                currentPopup?.dismiss()
            } else {
                finish()
            }
        }

        // Load values from save state (when activity is rotated)
        if (savedInstanceState != null) {
            val newStartDate = savedInstanceState.getLong(BUNDLE_START_DATE, 0L)
            val newEndDate = savedInstanceState.getLong(BUNDLE_END_DATE, 0L)
            val newTimeStep = savedInstanceState.getLong(BUNDLE_TIME_STEP, 0L)

            if (newStartDate > 0L)
                startDate = newStartDate
            if (newEndDate > 0L)
                endDate = newEndDate
            if (newTimeStep > 0L)
                timeStep = newTimeStep
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (startDate != null)
            outState.putLong(BUNDLE_START_DATE, startDate!!)
        if (endDate != null)
            outState.putLong(BUNDLE_END_DATE, endDate!!)
        if (timeStep != null)
            outState.putLong(BUNDLE_TIME_STEP, timeStep!!)
    }

    private fun onFetchButtonPressed() {
        if (startDate == null) {
            Toast.makeText(baseContext, resources.getString(R.string.error_no_start_date), Toast.LENGTH_SHORT).show()
            return
        }

        if (endDate == null) {
            Toast.makeText(baseContext, resources.getString(R.string.error_no_end_date), Toast.LENGTH_SHORT).show()
            return
        }

        if (timeStep == null) {
            Toast.makeText(baseContext, resources.getString(R.string.error_no_timestep), Toast.LENGTH_SHORT).show()
            return
        }

        // Warning for long time (6 months)
        if (endDate!! - startDate!! > warningLimit) {
            createTimeWarningAlert()
        }

        else {
            beginDataFetch()
        }
    }

    private fun createTimeWarningAlert() {
        val builder = AlertDialog.Builder(this, R.style.AlertDialog)
        builder.setTitle(resources.getString(R.string.camera_reading_complete))
        builder.setCancelable(false)

        builder.setMessage(resources.getString(R.string.spending_time_warning))
        builder.setPositiveButton(resources.getString(R.string.spending_alert_positive)) {dialogInterface, index ->
            beginDataFetch()
            dialogInterface.dismiss()
        }

        builder.setNegativeButton(resources.getString(R.string.spending_alert_negative)) { dialogInterface, index ->
            dialogInterface.dismiss()
        }

        builder.show()
    }

    private fun beginDataFetch() {
        fetchButton.isClickable = false
        receiptViewModel.receiptsInRange(startDate!!, endDate!!).observe(this) {receipts ->
            getReceiptCrossRefData(receipts, timeStep!!)
        }
    }

    private fun createPopup(isStartDate: Boolean, parent: EditText) {
        currentPopup?.dismiss()
        val popupData = ActivityUtils.createPopup(R.layout.popup_calendar, this)
        popupData.second.isFocusable = true
        currentPopup = popupData.second

        val popupHeader: TextView = popupData.first.findViewById(R.id.calendarPopupHeader)
        val calendarView: CalendarView = popupData.first.findViewById(R.id.calendarPopupView)

        // Header text
        if (isStartDate)
            popupHeader.setText(R.string.receipt_list_start_date)
        else
            popupHeader.setText(R.string.receipt_list_end_date)

        // Set calendar range
        calendarView.maxDate = System.currentTimeMillis()
        calendarView.date = System.currentTimeMillis()

        popupData.second.setOnDismissListener { currentPopup = null }

        calendarView.setOnDateChangeListener {_, y, m, d ->
            val calendar: Calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, y)
            calendar.set(Calendar.MONTH, m)
            if (isStartDate)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
            else
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DATE))

            val date: Date = calendar.time
            if (isStartDate) {
                startDate = date.time
            }
            else {
                endDate = date.time
            }

            parent.setText(String.format("%s %d", resources.getStringArray(R.array.month_array)[m], y))

            currentPopup?.dismiss()
            currentPopup = null
        }
    }

    private fun getReceiptCrossRefData(receipts: List<Receipt>, timeStep: Long) {
        val receiptIds = arrayListOf<Long>()
        receipts.forEach {receipt -> receiptIds.add(receipt.receiptId)}

        Log.d(TAG, "Number of receipts:  ${receiptIds.size}")

        val liveData = receiptProductViewModel.productsInReceipt(receiptIds)
        liveData.observe(this, object: Observer<List<ReceiptWithProducts>> {
            override fun onChanged(t: List<ReceiptWithProducts>?) {
                liveData.removeObserver(this)
                if (t != null) {
                    populateSpendingData(t, timeStep)
                } else {
                    Toast.makeText(baseContext, resources.getString(R.string.error_spending_overview_create_failed), Toast.LENGTH_SHORT).show()
                    fetchButton.isClickable = true
                }
            }
        })
    }

    private fun populateSpendingData(crossRef: List<ReceiptWithProducts>, timeStep: Long) {
        // Clear previous data
        spendingBlockList.clear()

        val productBlocks: HashMap<String, ArrayList<Product>> = hashMapOf()

        var totalProducts = 0
        var totalPriceInReceipts = 0f
        var totalPriceOfProducts = 0f

        crossRef.forEach {elem ->
            totalProducts += elem.products.size
            totalPriceInReceipts += elem.receipt.receiptPrice
        }

        Log.d(TAG, "Total products: $totalProducts, price on receipt: $totalPriceInReceipts")

        val timeSteps: ArrayList<Long> =
            if (timeStep == timeStepRange["Viikko"]) DateUtils.getStartOfWeeksBetween(startDate!!, endDate!!)
            else DateUtils.getStartOfMonthsBetween(startDate!!, endDate!!)

        for (i in timeSteps.indices)
        {
            if (i == timeSteps.indices.last)
                continue

            val currentDate = timeSteps[i]
            val upperLimit = timeSteps[i + 1]

            // Generate map key based on times, will be used for fetching time range specific items
            val key = "${currentDate}/${upperLimit}"

            for (ref in crossRef) {
                if (ref.receipt.receiptDate in currentDate until upperLimit) {
                    if (productBlocks[key] == null) {
                        productBlocks.put(key, arrayListOf<Product>())
                        productBlocks[key]?.addAll(ref.products)
                    }
                    else {
                        productBlocks[key]?.addAll(ref.products)
                    }
                }
            }
        }

        var fetchedCount = 0

        // Duplicate map is used for products that appear multiple times in a single block
        // Database sum can't deal with duplicates, so these have to be added here manually
        val duplicateMap: HashMap<String, ArrayList<DuplicateMapItem>> = hashMapOf()
        for (kvp in productBlocks) {
            val params: List<String> = kvp.key.split("/")
            val begin = params[0].toLong()
            val end = params[1].toLong()

            val productIds: List<Long> = kvp.value.map { item -> item.productId }

            // Populate duplicate map
            val itemCounts: HashMap<Product, Int> = getProductCounts(kvp.value)
            for (itemCountPair in itemCounts) {
                if (itemCountPair.value > 1) {
                    val dupItem = DuplicateMapItem(itemCountPair.key, itemCountPair.value)
                    if (duplicateMap.containsKey(kvp.key)) {
                        duplicateMap[kvp.key]?.add(dupItem)
                    } else {
                        duplicateMap[kvp.key] = arrayListOf(dupItem)
                    }
                }
            }

            val liveData = productViewModel.pricesInCategory(productIds)
            liveData.observe(this, object: Observer<List<CategoryPricePojo>> {
                override fun onChanged(list: List<CategoryPricePojo>?) {
                    fetchedCount++
                    liveData.removeObserver(this)
                    if (list != null) {
                        list.let {
                            // Feed duplicates
                            val duplicates = duplicateMap[kvp.key]
                            val duplicateFixedList: List<CategoryPricePojo> = fixDuplicatesForBlock(it, duplicates)
                            val blockData = SpendingTimeBlock(begin, end, duplicateFixedList)

                            // Update total price
                            blockData.spending.forEach { pojo -> totalPriceOfProducts += pojo.totalPrice }

                            spendingBlockList.add(blockData)
                            if (fetchedCount >= productBlocks.count()) {
                                val sorted = spendingBlockList.sortedWith(Comparator {a: SpendingTimeBlock, b: SpendingTimeBlock -> b.startTime.compareTo(a.startTime) })
                                spendingBlockList.clear()
                                spendingBlockList.addAll(sorted)
                                recyclerViewAdapter.notifyDataSetChanged()
                                updateWarningText(totalPriceInReceipts, totalPriceOfProducts)
                            }
                        }
                    } else {
                        Toast.makeText(baseContext, resources.getString(R.string.error_spending_overview_create_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

        // Re-enable fetch button
        fetchButton.isClickable = true
    }

    private fun getProductCounts(productList: ArrayList<Product>): HashMap<Product, Int> {
        val map: HashMap<Product, Int> = hashMapOf()

        for (i in productList.indices) {
            if (map.containsKey(productList[i])) {
                val currentValue = map[productList[i]]
                map[productList[i]] = currentValue!! + 1
            } else {
                map[productList[i]] = 1
            }
        }

        return map
    }

    private fun fixDuplicatesForBlock(existing: List<CategoryPricePojo>, duplicates: ArrayList<DuplicateMapItem>?) : List<CategoryPricePojo> {
        if (duplicates == null)
            return existing

        duplicates.forEach { duplicate ->
            val category = duplicate.product.productCategory
            val priceAddition = duplicate.product.productPrice * (duplicate.count - 1)

            existing.forEach { pojo ->
                if (pojo.category == category)
                    pojo.totalPrice += priceAddition
            }
        }

        return existing
    }

    private fun updateWarningText(receiptPrice: Float, productPrice: Float) {
        if (abs(receiptPrice - productPrice) > 0.05f) {
            warningTextView.isVisible = true
            warningTextView.text = resources.getString(
                R.string.warning_overview_price_mismatch, productPrice, UserConfig.currency, receiptPrice)
        } else {
            warningTextView.isVisible = false
        }
    }
}