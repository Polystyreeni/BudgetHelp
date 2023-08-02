package com.poly.budgethelp.adapter

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.NewReceiptActivity
import com.poly.budgethelp.R
import com.poly.budgethelp.data.Product
import com.poly.budgethelp.utility.ActivityUtils

const val VIEW_TYPE_ITEM = 0
const val VIEW_TYPE_NEW = 1

open class RecyclerViewItem
class AddItem(val context: NewReceiptActivity) : RecyclerViewItem()
class ContentItem(val product: Product, val context: NewReceiptActivity) : RecyclerViewItem()

class ReceiptProductAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var data = listOf<RecyclerViewItem>()

    override fun getItemViewType(position: Int): Int {
        if (data[position] is ContentItem)
            return VIEW_TYPE_ITEM
        return VIEW_TYPE_NEW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_ITEM) {
            return ContentViewHolder (
                LayoutInflater.from(parent.context).inflate(R.layout.product_list_item, parent, false)
            )
        } else {
            return AddItemViewHolder (
                LayoutInflater.from(parent.context).inflate(R.layout.product_list_new_item, parent, false)
            )
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = data[position]
        if (holder is ContentViewHolder && item is ContentItem) {
            holder.bind(item, position)
        }
        else if (holder is AddItemViewHolder && item is AddItem) {
            holder.bind(item)
        }
    }

    internal inner class ContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: ContentItem, position: Int) {
            val nameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
            val categoryTextView: TextView = itemView.findViewById(R.id.productCategoryTextView)
            val priceTextView: TextView = itemView.findViewById(R.id.productPriceTextView)
            val editButton: TextView = itemView.findViewById(R.id.productEditButton)
            val deleteButton: TextView = itemView.findViewById(R.id.productDeleteButton)

            nameTextView.text = item.product.productName
            categoryTextView.text = item.product.productCategory
            priceTextView.text = item.product.productPrice.toString()

            // Button functionalities
            deleteButton.setOnClickListener { item.context.removeProduct(item.product) }
            editButton.setOnClickListener {
                val popupData = ActivityUtils.createPopup(R.layout.popup_add_item, item.context)
                val nameEditText: EditText = popupData.first.findViewById(R.id.addProductNameEditText)
                val categorySpinner: Spinner = popupData.first.findViewById(R.id.addProductCategorySpinner)
                val priceEditText: EditText = popupData.first.findViewById(R.id.addProductPriceEditText)
                val confirmButton: Button = popupData.first.findViewById(R.id.addProductConfirmButton)

                item.context.currentPopups.add(popupData.second)
                popupData.second.isFocusable = true
                popupData.second.update()

                nameEditText.setText(item.product.productName)
                categorySpinner.adapter = item.context.categoryAdapter
                categorySpinner.setSelection(item.context.categoryAdapter.getPosition(item.product.productCategory))
                priceEditText.setText(item.product.productPrice.toString())

                categorySpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position == categorySpinner.adapter.count - 1) {
                            val categoryPopupData = ActivityUtils.createPopup(R.layout.popup_add_category, item.context)
                            val categoryConfirmButton: Button = categoryPopupData.first.findViewById(R.id.addCategoryConfirmButton)
                            val categoryText: EditText = categoryPopupData.first.findViewById(R.id.addCategoryNameEditText)
                            item.context.currentPopups.add(categoryPopupData.second)
                            categoryPopupData.second.isFocusable = true
                            categoryPopupData.second.update()
                            categoryConfirmButton.setOnClickListener {_ ->
                                val categoryName = categoryText.text.toString()
                                item.context.addNewCategory(categoryName)
                                item.context.removePopup(categoryPopupData.second)

                                // Refresh category spinner
                                categorySpinner.setSelection(0)
                            }
                        }
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                        if (categorySpinner.selectedItemPosition >= categorySpinner.adapter.count - 1) {
                            categorySpinner.setSelection(0)
                        }
                    }
                }

                confirmButton.setOnClickListener {
                    if (nameEditText.text.isBlank()) {
                        Toast.makeText(item.context,
                            item.context.resources.getString(R.string.error_empty_product),
                            Toast.LENGTH_SHORT).show()
                    }
                    else if (priceEditText.text.toString().toFloatOrNull() == null) {
                        Toast.makeText(item.context,
                            item.context.resources.getString(R.string.error_no_price),
                            Toast.LENGTH_SHORT).show()
                    }
                    else {
                        val name: String = nameEditText.text.toString()
                        val price: Float? = priceEditText.text.toString().toFloatOrNull()
                        val category: String = categorySpinner.selectedItem as String

                        if (price != null) {
                            val newData = Product(name, category, price)
                            item.context.modifyItemAtPosition(position, newData)
                        }
                        else {
                            Toast.makeText(item.context,
                                item.context.resources.getString(R.string.error_invalid_price),
                                Toast.LENGTH_SHORT).show()
                        }
                    }

                    item.context.removePopup(popupData.second)
                    //item.context.currentPopups.remove(popupData.second)
                    //popupData.second.dismiss()
                }
            }
        }
    }

    internal inner class AddItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: AddItem) {
            val newButton: TextView = itemView.findViewById(R.id.productNameTextView)
            newButton.setOnClickListener {
                val popupData = ActivityUtils.createPopup(R.layout.popup_add_item, item.context)
                popupData.second.isFocusable = true
                popupData.second.update()

                item.context.currentPopups.add(popupData.second)

                // Set popupView item stuff
                val nameEditText: EditText = popupData.first.findViewById(R.id.addProductNameEditText)
                val categorySpinner: Spinner = popupData.first.findViewById(R.id.addProductCategorySpinner)
                val priceEditText: EditText = popupData.first.findViewById(R.id.addProductPriceEditText)
                val confirmButton: Button = popupData.first.findViewById(R.id.addProductConfirmButton)

                item.context.categoryAdapter.setDropDownViewResource(R.layout.spinner_item)
                categorySpinner.adapter = item.context.categoryAdapter

                categorySpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position == categorySpinner.adapter.count - 1) {
                            val categoryPopupData = ActivityUtils.createPopup(R.layout.popup_add_category, item.context)
                            val categoryConfirmButton: Button = categoryPopupData.first.findViewById(R.id.addCategoryConfirmButton)
                            val categoryText: EditText = categoryPopupData.first.findViewById(R.id.addCategoryNameEditText)
                            item.context.currentPopups.add(categoryPopupData.second)
                            categoryPopupData.second.isFocusable = true
                            categoryPopupData.second.update()
                            categoryConfirmButton.setOnClickListener {_ ->
                                val categoryName = categoryText.text.toString()
                                item.context.addNewCategory(categoryName)
                                item.context.removePopup(categoryPopupData.second)

                                // Refresh category spinner
                                categorySpinner.setSelection(0)
                            }
                        }
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                        if (categorySpinner.selectedItemPosition >= categorySpinner.adapter.count - 1) {
                            categorySpinner.setSelection(0)
                        }
                    }
                }

                confirmButton.setOnClickListener {
                    if (nameEditText.text.isBlank()) {
                        Toast.makeText(item.context,
                            item.context.resources.getString(R.string.error_empty_product),
                            Toast.LENGTH_SHORT).show()
                    }
                    else if (priceEditText.text.toString().toFloatOrNull() == null) {
                        Toast.makeText(item.context,
                            item.context.resources.getString(R.string.error_no_price),
                            Toast.LENGTH_SHORT).show()
                    }
                    else {
                        val name: String = nameEditText.text.toString()
                        val price: Float? = priceEditText.text.toString().toFloatOrNull()
                        val category: String = categorySpinner.selectedItem as String
                        if (price != null) {
                            val product = Product(name, category, price)
                            item.context.addNewProduct(product)
                        }
                        else {
                            Toast.makeText(item.context,
                                item.context.resources.getString(R.string.error_invalid_price),
                                Toast.LENGTH_SHORT).show()
                        }
                    }

                    // item.context.currentPopups.remove(popupWindow)
                    // popupWindow.dismiss()
                    item.context.removePopup(popupData.second)
                }
            }
        }
    }
}