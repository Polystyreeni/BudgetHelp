package com.poly.budgethelp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.CalendarView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.adapter.ReceiptListAdapter
import com.poly.budgethelp.data.Receipt
import com.poly.budgethelp.viewmodel.ReceiptViewModel
import com.poly.budgethelp.viewmodel.ReceiptViewModelFactory
import java.util.Calendar
import java.util.Date

class ReceiptListActivity : AppCompatActivity() {

    private val TAG: String = "NewRecipeActivity"

    private lateinit var recyclerView: RecyclerView
    private lateinit var dateStartEdit: EditText
    private lateinit var dateEndEdit: EditText
    private lateinit var receiptPriceText: TextView
    private lateinit var adapter: ReceiptListAdapter

    private var dateStart: Long? = null
    private var dateEnd: Long? = null

    private var currentPopup: PopupWindow? = null

    private val receiptViewModel: ReceiptViewModel by viewModels {
        ReceiptViewModelFactory((application as BudgetApplication).receiptRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt_list)

        recyclerView = findViewById(R.id.receiptRecyclerView)
        receiptPriceText = findViewById(R.id.receiptListTotalPrice)
        adapter = ReceiptListAdapter()
        adapter.baseContext = this
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        receiptPriceText.text = resources.getString(R.string.receipt_list_total_price, 0f)

        receiptViewModel.allReceipts.observe(this) {receipts ->
            receipts.let {
                adapter.submitList(it)
                calculateReceiptTotalPrice(it)
            }
        }

        dateStartEdit = findViewById(R.id.receiptListStartEdit)
        dateEndEdit = findViewById(R.id.receiptListEndEdit)

        dateStartEdit.setOnClickListener {_ ->
            createPopup(true)
        }

        dateEndEdit.setOnClickListener {_ ->
            createPopup(false)
        }

        onBackPressedDispatcher.addCallback(this) {
            if (currentPopup != null) {
                currentPopup?.dismiss()
                currentPopup = null
            }
            else {
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentPopup?.dismiss()
    }

    private fun calculateReceiptTotalPrice(receipts: List<Receipt>) {
        var total = 0f
        for (receipt in receipts) {
            total += receipt.receiptPrice
        }

        receiptPriceText.text = resources.getString(R.string.receipt_list_total_price, total)
    }

    private fun createPopup(startDate: Boolean) {
        val popupView = LayoutInflater.from(this).inflate(R.layout.popup_calendar, null)
        val width: Int = LinearLayout.LayoutParams.WRAP_CONTENT
        val height: Int = LinearLayout.LayoutParams.WRAP_CONTENT

        val popupWindow = PopupWindow(popupView, width, height, true)
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

        val popupHeader: TextView = popupView.findViewById(R.id.calendarPopupHeader)
        val calendarView: CalendarView = popupView.findViewById(R.id.calendarPopupView)

        // Assign header text
        popupHeader.text = if (startDate) resources.getString(R.string.receipt_list_start_date)
            else resources.getString(R.string.receipt_list_end_date)

        // Set calendar range
        calendarView.maxDate = System.currentTimeMillis()
        calendarView.date = System.currentTimeMillis()

        calendarView.setOnDateChangeListener {_, y, m, d ->
            val calendar: Calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, y)
            calendar.set(Calendar.MONTH, m)
            calendar.set(Calendar.DAY_OF_MONTH, d)
            val date: Date = calendar.time
            if (startDate) {
                dateStartEdit.setText(String.format("%d.%d %d", d, m + 1, y))
                dateStart = date.time
            }
            else {
                dateEndEdit.setText(String.format("%d.%d %d", d, m + 1, y))
                dateEnd = date.time
            }

            currentPopup?.dismiss()
            currentPopup = null

            resetReceiptAdapter()
        }

        currentPopup = popupWindow
    }

    private fun resetReceiptAdapter() {
        if (dateStart == null)
            dateStart = 0
        if (dateEnd == null)
            dateEnd = System.currentTimeMillis()

        receiptViewModel.receiptsInRange(dateStart!!, dateEnd!!).observe(this) { receipts ->
            receipts.let {
                adapter.submitList(it)
            }
        }
    }

    fun startReceiptInfoActivity(receipt: Receipt) {
        val intent = Intent(this, ReceiptInfoActivity::class.java)
        intent.putExtra(EXTRA_MESSAGE, receipt.receiptId.toString())
        startActivity(intent)
    }

    companion object {
        const val EXTRA_MESSAGE = "ReceiptListSelectedProduct"
    }
}