package com.poly.budgethelp

import android.content.Intent
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
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.adapter.SpendingItemAdapter
import com.poly.budgethelp.data.CategoryPricePojo
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

class SpendingOverviewActivity : AppCompatActivity() {

    private val TAG = "SpendingOverviewActivity"
    private val BUNDLE_START_DATE = "startDate"
    private val BUNDLE_END_DATE = "endDate"
    private val BUNDLE_TIME_STEP = "timeStep"

    // UI Components
    private lateinit var fetchButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: SpendingItemAdapter

    private var startDate: Long? = null
    private var endDate: Long? = null
    private val timeSteps: ArrayList<Pair<Long, Long>> = arrayListOf()
    private val timeStepRange = hashMapOf(Pair("Viikko", 604800000L), Pair("Kuukausi", 2678400000L))
    private var timeStep: Long? = null

    private var currentPopup: PopupWindow? = null

    // Overview state
    private var currentTimeStepIndex: Int = 0
    private val spendingBlockList: ArrayList<SpendingTimeBlock> = arrayListOf()
    private val warningLimit: Long = 7889400000L

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
                    generateTimeSteps(timeStepSpinner.getItemAtPosition(position).toString())
                currentTimeStepIndex = position
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

        // Warning for long time (3 months)
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

    private fun generateTimeSteps(selectedTimeStep: String) {
        timeSteps.clear()

        if (startDate == null || endDate == null)
            return

        // val diff: Long = endDate!! - startDate!!
        val add: Long = timeStepRange[selectedTimeStep]!!
        // val step: Long = diff / add
        timeStep = add

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

        /*receiptProductViewModel.productsInReceipt(receiptIds).observe(this) { crossRef ->
            populateSpendingData(crossRef, timeStep)
        }*/
    }

    private fun populateSpendingData(crossRef: List<ReceiptWithProducts>, timeStep: Long) {
        // Clear previous data
        spendingBlockList.clear()

        val productBlocks: HashMap<Int, ArrayList<Product>> = hashMapOf()
        var blockIndex = 0
        var currentDate: Long = startDate!!
        while (currentDate < endDate!!)
        {
            val upperLimit =
                if (currentTimeStepIndex == 1) DateUtils.getFirstDayOfWeek(currentDate + timeStep)
                else DateUtils.getFirstDayOfMonth(currentDate + timeStep)

            for (ref in crossRef) {
                if (ref.receipt.receiptDate in currentDate until upperLimit) {
                    if (productBlocks[blockIndex] == null) {
                        productBlocks.put(blockIndex, arrayListOf<Product>())
                        productBlocks[blockIndex]?.addAll(ref.products)
                    }
                    else {
                        productBlocks[blockIndex]?.addAll(ref.products)
                    }
                    // Log.d(TAG, "Added products to block with index: ${blockIndex}")
                    Log.d(TAG, "Block with index ${blockIndex} contains ${productBlocks[blockIndex]?.size} products")
                }
            }

            currentDate = upperLimit
            blockIndex++
        }

        var fetchedCount = 0

        for (kvp in productBlocks) {
            val begin =
                if (currentTimeStepIndex == 1) DateUtils.getFirstDayOfWeek(startDate!! + timeStep * kvp.key)
                else DateUtils.getFirstDayOfMonth(startDate!! + timeStep * kvp.key)
            var end =
                if (currentTimeStepIndex == 1) DateUtils.getFirstDayOfWeek(startDate!! + timeStep * (kvp.key + 1))
                else DateUtils.getFirstDayOfMonth(startDate!! + timeStep * (kvp.key + 1))
            if (end > endDate!!)
                end = endDate!!

            val productIds: List<Long> = kvp.value.map { item -> item.productId }
            val liveData = productViewModel.pricesInCategory(productIds)
            liveData.observe(this, object: Observer<List<CategoryPricePojo>> {
                override fun onChanged(list: List<CategoryPricePojo>?) {
                    fetchedCount++
                    liveData.removeObserver(this)
                    if (list != null) {
                        list.let {
                            val blockData = SpendingTimeBlock(begin, end, it)
                            spendingBlockList.add(blockData)

                            if (fetchedCount >= productBlocks.count()) {
                                // Log.d(TAG, "$fetchedCount > prodictBlocks count ${productBlocks.count()}")
                                val sorted = spendingBlockList.sortedWith(Comparator {a: SpendingTimeBlock, b: SpendingTimeBlock -> b.startTime.compareTo(a.startTime) })
                                spendingBlockList.clear()
                                spendingBlockList.addAll(sorted)
                                recyclerViewAdapter.notifyDataSetChanged()
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
}