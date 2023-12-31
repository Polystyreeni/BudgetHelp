package com.poly.budgethelp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.NewReceiptActivity
import com.poly.budgethelp.R
import com.poly.budgethelp.config.UserConfig
import com.poly.budgethelp.data.Product
import com.poly.budgethelp.db.AppRoomDatabase
import com.poly.budgethelp.utility.ActivityUtils
import com.poly.budgethelp.utility.TextUtils.Companion.sanitizeText

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
        return if (viewType == VIEW_TYPE_ITEM) {
            ContentViewHolder (
                LayoutInflater.from(parent.context).inflate(R.layout.product_list_item, parent, false)
            )
        } else {
            AddItemViewHolder (
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
            val editButton: View = itemView.findViewById(R.id.productEditButton)

            nameTextView.text = item.product.productName
            categoryTextView.text = item.product.productCategory
            priceTextView.text = String.format("%.2f %s", item.product.productPrice, UserConfig.currency)

            // Button functionalities
            editButton.setOnClickListener {
                val popupData = ActivityUtils.createPopup(R.layout.popup_add_item, item.context)
                val headerText: TextView = popupData.first.findViewById(R.id.addItemPopupHeader)
                val nameEditText: EditText = popupData.first.findViewById(R.id.addProductNameEditText)
                val categoryTextEdit: TextView = popupData.first.findViewById(R.id.addProductCategoryTextView)
                val priceEditText: EditText = popupData.first.findViewById(R.id.addProductPriceEditText)
                val confirmButton: Button = popupData.first.findViewById(R.id.addProductConfirmButton)

                item.context.currentPopups.add(popupData.second)
                popupData.second.isFocusable = true
                popupData.second.update()

                popupData.second.setOnDismissListener { item.context.removePopup(popupData.second) }

                headerText.text = item.context.resources.getString(R.string.edit_product_header)
                nameEditText.setText(item.product.productName)
                categoryTextEdit.text = item.product.productCategory
                priceEditText.setText(item.product.productPrice.toString())

                categoryTextEdit.setOnClickListener { _ ->
                    val builder = AlertDialog.Builder(item.context, R.style.AlertDialog)
                    builder.setTitle(item.context.resources.getString(R.string.new_product_category))
                    builder.setCancelable(true)

                    builder.setMultiChoiceItems(item.context.categoryArray,
                        BooleanArray(item.context.categoryArray.size)) {  dialogInterface, index, value ->
                        if (value) {
                            categoryTextEdit.text = item.context.categoryArray[index]
                            dialogInterface.dismiss()
                        }
                    }

                    builder.setPositiveButton(item.context.resources.getString(R.string.header_new_category)) { dialogInterface, _ ->
                        val categoryPopupData = ActivityUtils.createPopup(R.layout.popup_add_category, item.context)
                        val categoryConfirmButton: Button = categoryPopupData.first.findViewById(R.id.addCategoryConfirmButton)
                        val categoryText: EditText = categoryPopupData.first.findViewById(R.id.addCategoryNameEditText)

                        categoryPopupData.second.setOnDismissListener {
                            item.context.removePopup(categoryPopupData.second)
                        }

                        item.context.currentPopups.add(categoryPopupData.second)
                        categoryPopupData.second.isFocusable = true
                        categoryPopupData.second.update()
                        categoryConfirmButton.setOnClickListener {_ ->
                            categoryPopupData.second.dismiss()
                            val categoryName = categoryText.text.toString()
                            item.context.addNewCategory(categoryName)
                        }
                        dialogInterface.dismiss()
                    }

                    builder.setNegativeButton(item.context.resources.getString(R.string.generic_reply_negative)) {dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }

                    builder.show()
                }

                confirmButton.text = item.context.resources.getString(R.string.edit_product_confirm)
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
                        val name: String = sanitizeText(nameEditText.text.toString().uppercase())
                        val price: Float? = priceEditText.text.toString().toFloatOrNull()
                        val category: String = categoryTextEdit.text.toString()

                        if (price != null) {
                            val newData = Product(name, category, price)
                            item.context.modifyItemAtPosition(position, newData)
                            popupData.second.dismiss()
                        }
                        else {
                            Toast.makeText(item.context,
                                item.context.resources.getString(R.string.error_invalid_price),
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    internal inner class AddItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: AddItem) {
            val newButton: TextView = itemView.findViewById(R.id.productNameTextView)
            newButton.setOnClickListener {
                item.context.disableSoftKeyboard()
                val popupData = ActivityUtils.createPopup(R.layout.popup_add_item, item.context)
                popupData.second.isFocusable = true
                popupData.second.update()

                item.context.currentPopups.add(popupData.second)

                popupData.second.setOnDismissListener { item.context.removePopup(popupData.second) }

                // Set popupView item stuff
                val nameEditText: EditText = popupData.first.findViewById(R.id.addProductNameEditText)
                val categoryTextView: TextView = popupData.first.findViewById(R.id.addProductCategoryTextView)

                val priceEditText: EditText = popupData.first.findViewById(R.id.addProductPriceEditText)
                val confirmButton: Button = popupData.first.findViewById(R.id.addProductConfirmButton)

                categoryTextView.text = AppRoomDatabase.DEFAULT_CATEGORY
                categoryTextView.setOnClickListener { _ ->
                    val builder = AlertDialog.Builder(item.context, R.style.AlertDialog)
                    builder.setTitle(item.context.resources.getString(R.string.new_product_category))
                    builder.setCancelable(true)

                    builder.setMultiChoiceItems(item.context.categoryArray,
                        BooleanArray(item.context.categoryArray.size)) {  dialogInterface, index, value ->
                        if (value) {
                            categoryTextView.text = item.context.categoryArray[index]
                            dialogInterface.dismiss()
                        }
                    }

                    builder.setPositiveButton(item.context.resources.getString(R.string.header_new_category)) { dialogInterface, _ ->
                        val categoryPopupData = ActivityUtils.createPopup(R.layout.popup_add_category, item.context)
                        val categoryConfirmButton: Button = categoryPopupData.first.findViewById(R.id.addCategoryConfirmButton)
                        val categoryText: EditText = categoryPopupData.first.findViewById(R.id.addCategoryNameEditText)

                        categoryPopupData.second.setOnDismissListener {
                            item.context.removePopup(categoryPopupData.second)
                        }

                        item.context.currentPopups.add(categoryPopupData.second)
                        categoryPopupData.second.isFocusable = true
                        categoryPopupData.second.update()
                        categoryConfirmButton.setOnClickListener {_ ->
                            categoryPopupData.second.dismiss()
                            val categoryName = categoryText.text.toString()
                            item.context.addNewCategory(categoryName)
                        }
                        dialogInterface.dismiss()
                    }

                    builder.setNegativeButton(item.context.resources.getString(R.string.generic_reply_negative)) {dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }

                    builder.show()
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
                        val name: String = sanitizeText(nameEditText.text.toString().uppercase())
                        val price: Float? = priceEditText.text.toString().toFloatOrNull()
                        val category: String = categoryTextView.text.toString()
                        if (price != null) {
                            val product = Product(name, category, price)
                            item.context.addNewProduct(product)
                            popupData.second.dismiss()
                        }
                        else {
                            Toast.makeText(item.context,
                                item.context.resources.getString(R.string.error_invalid_price),
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}