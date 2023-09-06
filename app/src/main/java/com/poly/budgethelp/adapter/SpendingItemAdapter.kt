package com.poly.budgethelp.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.R
import com.poly.budgethelp.config.UserConfig
import com.poly.budgethelp.data.CategoryPricePojo
import com.poly.budgethelp.data.SpendingTimeBlock
import com.poly.budgethelp.utility.DateUtils

class SpendingItemAdapter (private var dataSet: List<SpendingTimeBlock>) : RecyclerView.Adapter<SpendingItemAdapter.ViewHolder>() {

    lateinit var baseContext: AppCompatActivity

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Define views used in the element
        val rangeTextView: TextView
        val totalPriceTextView: TextView
        val categoryLayout: LinearLayout

        init {
            rangeTextView = view.findViewById(R.id.spendingItemRange)
            totalPriceTextView = view.findViewById(R.id.spendingItemTotalPrice)
            categoryLayout = view.findViewById(R.id.spendingItemCategoryLayout)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_spending_overview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.rangeTextView.text = String.format("%s - %s",
            DateUtils.longToDateString(dataSet[position].startTime), DateUtils.longToDateString(dataSet[position].endTime))
        val totalSum: Float = dataSet[position].spending.map { item -> item.totalPrice }.sum()
        holder.totalPriceTextView.text = String.format("%.2f %s", totalSum, UserConfig.currency)
        holder.categoryLayout.removeAllViews()
        for (category in dataSet[position].spending) {
            val textView: TextView = LayoutInflater.from(baseContext).inflate(R.layout.item_spending_category, null) as TextView
            textView.text = String.format("%s: %.2f %s", category.category, category.totalPrice, UserConfig.currency)
            textView.setTextColor(Color.BLACK)
            holder.categoryLayout.addView(textView)
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}