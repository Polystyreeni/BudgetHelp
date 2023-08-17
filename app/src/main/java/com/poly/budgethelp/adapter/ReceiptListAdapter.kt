package com.poly.budgethelp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.R
import com.poly.budgethelp.ReceiptListActivity
import com.poly.budgethelp.data.Receipt
import com.poly.budgethelp.utility.DateUtils

class ReceiptListAdapter : ListAdapter<Receipt, ReceiptListAdapter.ReceiptViewHolder>(ReceiptsComparator()) {

    lateinit var baseContext: ReceiptListActivity
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        return ReceiptViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, baseContext)
    }

    class ReceiptViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val receiptListItemLayout: LinearLayout = itemView.findViewById(R.id.receiptListItemLayout)
        private val receiptNameView: TextView = itemView.findViewById(R.id.receiptListName)
        private val receiptDateView: TextView = itemView.findViewById(R.id.receiptListDate)
        private val receiptPriceView: TextView = itemView.findViewById(R.id.receiptListPrice)
        fun bind(receipt: Receipt, baseContext: ReceiptListActivity) {
            receiptNameView.text = receipt.receiptName
            receiptDateView.text = DateUtils.longToDateString(receipt.receiptDate)
            receiptPriceView.text = String.format("%.2f €", receipt.receiptPrice)

            receiptListItemLayout.setOnClickListener {_ ->
                baseContext.startReceiptInfoActivity(receipt)
            }
        }

        companion object {
            fun create(parent: ViewGroup): ReceiptViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.receipt_list_item, parent, false)
                return ReceiptViewHolder(view)
            }
        }
    }

    class ReceiptsComparator : DiffUtil.ItemCallback<Receipt>() {
        override fun areItemsTheSame(oldItem: Receipt, newItem: Receipt): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Receipt, newItem: Receipt): Boolean {
            return oldItem.receiptId == newItem.receiptId
        }
    }
}