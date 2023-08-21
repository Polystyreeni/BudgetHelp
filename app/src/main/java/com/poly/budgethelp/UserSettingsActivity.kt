package com.poly.budgethelp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.poly.budgethelp.adapter.WordToIgnoreAdapter
import com.poly.budgethelp.config.UserConfig
import com.poly.budgethelp.data.WordToIgnore
import com.poly.budgethelp.utility.ActivityUtils
import com.poly.budgethelp.viewmodel.WordToIgnoreViewModel
import com.poly.budgethelp.viewmodel.WordToIgnoreViewModelFactory
import kotlinx.coroutines.launch

class UserSettingsActivity : AppCompatActivity() {

    private lateinit var nameEdit: EditText
    private lateinit var currencySpinner: Spinner
    private lateinit var priceEdit: EditText
    private lateinit var offsetEdit: EditText
    private lateinit var wordsToIgnoreEdit: EditText

    private val wordsToIgnore: ArrayList<String> = arrayListOf()
    private lateinit var adapter: WordToIgnoreAdapter

    private val activePopups: ArrayList<PopupWindow> = arrayListOf()

    // ViewModels
    private val wordToIgnoreViewModel: WordToIgnoreViewModel by viewModels {
        WordToIgnoreViewModelFactory((application as BudgetApplication).wordToIgnoreRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_settings)

        nameEdit = findViewById(R.id.settingsNameEdit)
        currencySpinner = findViewById(R.id.settingsCurrencySpinner)
        priceEdit = findViewById(R.id.settingsMaxPriceEdit)
        offsetEdit = findViewById(R.id.settingsOffsetEdit)
        wordsToIgnoreEdit = findViewById(R.id.wordsToIgnoreEditText)

        adapter = WordToIgnoreAdapter()

        ArrayAdapter.createFromResource(this,
            R.array.settings_currency_symbol,
            android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                currencySpinner.adapter = adapter
                currencySpinner.setSelection(adapter.getPosition(UserConfig.currency))
            }

        // Fill in the default values
        initializeValues()

        // Register callbacks
        currencySpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                UserConfig.currency = currencySpinner.adapter.getItem(position).toString()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                // Ignore
            }
        }

        wordToIgnoreViewModel.allWords.observe(this) {words ->
            words.let {
                wordsToIgnore.clear()
                adapter.submitList(it)
                it.forEach {wordToIgnore -> wordsToIgnore.add(wordToIgnore.word)}
                updateWordEditText()
            }
        }

        wordsToIgnoreEdit.setOnClickListener {_ ->
            createWordsPopup()
        }

        // Back button functionality
        onBackPressedDispatcher.addCallback(this) {
            if (activePopups.size > 0) {
                val popup = activePopups[activePopups.size - 1]
                popup.dismiss()
            } else {
                finish()
            }
        }

        // Return button
        val returnButton: TextView = findViewById(R.id.settingsReturnButton)
        returnButton.setOnClickListener { _ ->
            for (popup in activePopups)
                popup.dismiss()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (nameEdit.text.toString().isBlank())
            nameEdit.setText(UserConfig.DEFAULTUSERNAME)
        saveSettings()
    }

    private fun saveSettings() {
        val name: String = nameEdit.text.toString()
        name.replace("/", " ")
        UserConfig.userName = name
        UserConfig.currency = currencySpinner.selectedItem.toString()

        val price: Float? = priceEdit.text.toString().toFloatOrNull()
        if (price != null)
            UserConfig.productMaxPrice = price

        val offset: Int? = offsetEdit.text.toString().toIntOrNull()
        if (offset != null)
            UserConfig.priceNameMaxOffset = offset

        UserConfig.writeConfig(this)
    }

    private fun initializeValues() {
        if (UserConfig.userName != UserConfig.DEFAULTUSERNAME) {
            nameEdit.setText(UserConfig.userName)
        }

        priceEdit.setText(UserConfig.productMaxPrice.toString())
        offsetEdit.setText(UserConfig.priceNameMaxOffset.toString())
    }

    private fun updateWordEditText() {
        val builder: StringBuilder = StringBuilder()
        if (wordsToIgnore.size > 6) {
            builder.append("...")
        }
        else if (wordsToIgnore.size > 0) {
            for (i in 0 until wordsToIgnore.size - 1) {
                builder.append(wordsToIgnore[i]).append(", ")
            }
            builder.append(wordsToIgnore[wordsToIgnore.size - 1])
        }

        wordsToIgnoreEdit.setText(builder.toString())
    }

    private fun createWordsPopup() {
        val popupData = ActivityUtils.createPopup(R.layout.popup_ignored_words_config, this)
        val recyclerView: RecyclerView = popupData.first.findViewById(R.id.wordsRecyclerView)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        addPopup(popupData.second)
        popupData.second.isFocusable = true
        popupData.second.setOnDismissListener { removePopup(popupData.second) }

        val newButton: Button = popupData.first.findViewById(R.id.wordsAddNewButton)
        newButton.setOnClickListener { _ ->
            createNewWordPopup()
        }
    }

    private fun createNewWordPopup() {
        val popupData = ActivityUtils.createPopup(R.layout.popup_add_word, this)
        val wordEdit: EditText = popupData.first.findViewById(R.id.addWordEditText)
        val confirmButton: Button = popupData.first.findViewById(R.id.addWordConfirmButton)

        confirmButton.setOnClickListener { addNewWordToIgnore(wordEdit.text.toString().uppercase(), popupData.second) }

        addPopup(popupData.second)
        popupData.second.isFocusable = true
        popupData.second.setOnDismissListener { removePopup(popupData.second) }
        popupData.second.update()
    }

    private fun addPopup(popup: PopupWindow) {
        activePopups.add(popup)
    }

    private fun removePopup(popup: PopupWindow) {
        activePopups.remove(popup)
    }

    private fun addNewWordToIgnore(word: String, window: PopupWindow) {
        if (word.isBlank()) {
            Toast.makeText(this, resources.getString(R.string.error_empty_word_to_ignore), Toast.LENGTH_SHORT).show()
            return
        }

        if (wordsToIgnore.contains(word)) {
            Toast.makeText(this, resources.getString(R.string.error_word_to_ignore_exists), Toast.LENGTH_SHORT).show()
            return
        }

        val wordToIgnore = WordToIgnore(word)
        lifecycleScope.launch {
            wordToIgnoreViewModel.insert(wordToIgnore)
        }

        window.dismiss()
    }
}