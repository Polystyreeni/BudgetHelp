package com.poly.budgethelp

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
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.adapter.SpendingItemAdapter
import com.poly.budgethelp.data.CategoryPricePojo
import com.poly.budgethelp.data.Product
import com.poly.budgethelp.data.Receipt
import com.poly.budgethelp.data.ReceiptWithProducts
import com.poly.budgethelp.data.SpendingTimeBlock
import com.poly.budgethelp.utility.ActivityUtils
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
    private val spendingBlockList: ArrayList<SpendingTimeBlock> = arrayListOf()

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
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        }

        val startEdit: EditText = findViewById(R.id.spendingStartDate)
        val endEdit: EditText = findViewById(R.id.spendingEndDate)

        startEdit.setOnClickListener {view ->
            createPopup(true, startEdit)
        }

        endEdit.setOnClickListener {view ->
            createPopup(false, endEdit)
        }
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

        // TODO: Warning for long time

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
        Log.d(TAG, "Selected timestep is: $selectedTimeStep")
        timeSteps.clear()

        if (startDate == null || endDate == null)
            return

        val diff: Long = endDate!! - startDate!!
        val add: Long = timeStepRange[selectedTimeStep]!!
        val step: Long = diff / add
        timeStep = step

        Log.d(TAG, "Step is: $step")
    }

    private fun getReceiptCrossRefData(receipts: List<Receipt>, timeStep: Long) {
        val receiptIds = arrayListOf<Long>()
        receipts.forEach {receipt -> receiptIds.add(receipt.receiptId)}

        Log.d(TAG, "Number of receipts:  ${receiptIds.size}")

        receiptProductViewModel.productsInReceipt(receiptIds).observe(this) { crossRef ->
            populateSpendingData(crossRef, timeStep)
        }
    }

    private fun populateSpendingData(crossRef: List<ReceiptWithProducts>, timeStep: Long) {
        //val products: ArrayList<Product> = arrayListOf()
        // crossRef.forEach {ref -> products.addAll(ref.products)}

        // Log.d(TAG, "Number of products:  ${products.size}")

        // Clear previous data
        spendingBlockList.clear()

        val productBlocks: HashMap<Int, ArrayList<Product>> = hashMapOf()
        var blockIndex = 0
        var currentDate: Long = startDate!!
        while (currentDate < endDate!!)
        {
            val upperLimit = currentDate + (endDate!! - startDate!!) / timeStep
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

        Log.d(TAG, "Product map contains ${productBlocks.size} elements")

        for (kvp in productBlocks) {
            val begin = startDate!! + ((endDate!! - startDate!!) / timeStep) * kvp.key
            var end = startDate!! + ((endDate!! - startDate!!) / timeStep) * (kvp.key + 1)
            if (end > endDate!!)
                end = endDate!!

            val productIds: List<Long> = kvp.value.map { item -> item.productId }
            productViewModel.pricesInCategory(productIds).observe(this) { list ->
                list.let {
                    val blockData = SpendingTimeBlock(begin, end, it)
                    spendingBlockList.add(blockData)
                    recyclerViewAdapter.notifyDataSetChanged()
                }
            }
        }

        // TODO: Temporary implementation, need to handle timestep as well
        /*val productIds = products.map { product -> product.productId }
        productViewModel.pricesInCategory(productIds).observe(this) { priceCategoryPojoList ->
             priceCategoryPojoList.let {
                 it.forEach {elem -> Log.d(TAG, "Category: ${elem.category}, total price: ${elem.totalPrice}")}
                 fetchButton.isClickable = true
             }
        }*/
    }
}