package com.poly.budgethelp

import android.content.Intent
import android.os.Bundle
import android.text.util.Linkify
import android.util.Log
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.poly.budgethelp.config.UserConfig
import com.poly.budgethelp.config.VersionManager
import com.poly.budgethelp.messageservice.MessageService
import com.poly.budgethelp.ui.theme.BudgetHelpTheme
import com.poly.budgethelp.ui.theme.DarkGreen
import com.poly.budgethelp.ui.theme.HyperlinkText
import com.poly.budgethelp.utility.ActivityUtils
import com.poly.budgethelp.utility.DateUtils
import com.poly.budgethelp.utility.TextUtils
import com.poly.budgethelp.viewmodel.ReceiptViewModel
import com.poly.budgethelp.viewmodel.ReceiptViewModelFactory
import java.util.Random
import kotlin.math.abs


class MainActivity : ComponentActivity() {

    private val BUNDLE_UPDATE = "updateShown"
    private val BUNDLE_MESSAGE = "messageShown"

    // Mutable states
    private val userName = mutableStateOf(UserConfig.DEFAULTUSERNAME)
    private val spendingThisMonth = mutableStateOf(0f)
    private val spendingLastMonth = mutableStateOf(0f)
    private val additionalText = mutableStateOf("")

    // Popup states
    private val showNewReceiptPopup = mutableStateOf(false)
    private val showSpendingPopup = mutableStateOf(false)
    private val showManagementPopup = mutableStateOf(false)
    private val showUpdatePopup = mutableStateOf(false)
    private val showMessagePopup = mutableStateOf(false)

    private val activePopups: ArrayList<PopupWindow> = arrayListOf()
    private var updateAcknowledged = false
    private var messageAcknowledged = false

    private var fetchedAppVersion: VersionManager.Version? = null

    // viewModels
    private val receiptViewModel: ReceiptViewModel by viewModels {
        ReceiptViewModelFactory((application as BudgetApplication).receiptRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val configExists: Boolean = UserConfig.readConfig(this)
        if (!configExists) {
            startSettingsActivity()
        }

        userName.value = UserConfig.userName

        if (savedInstanceState == null) {
            VersionManager.fetchLatestVersion(this)
            MessageService.fetchMessage(this)
        } else {
            if (!savedInstanceState.getBoolean(BUNDLE_UPDATE))
                VersionManager.fetchLatestVersion(this)
            if (!savedInstanceState.getBoolean(BUNDLE_MESSAGE))
                MessageService.fetchMessage(this)
        }

        onBackPressedDispatcher.addCallback(this) {
            if (activePopups.size > 0) {
                val popup = activePopups[activePopups.size - 1]
                activePopups.remove(popup)
                popup.dismiss()
            } else {
                finish()
            }
        }

        // Fetch receipt data to display -> spending this month & spending last month
        getSpendingData()

        setContent {
            BudgetHelpTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(BUNDLE_UPDATE, updateAcknowledged)
        outState.putBoolean(BUNDLE_MESSAGE, messageAcknowledged)
    }

    private fun getSpendingData() {
        val currentTime = DateUtils.getDayLastHour(System.currentTimeMillis())
        val firstDayOfCurrent = DateUtils.getFirstDayOfMonth(currentTime)
        val currentLastMonth = DateUtils.getDayLastMonth(currentTime)
        val firstDayLastMonth = DateUtils.getFirstDayOfMonth(currentLastMonth)

        receiptViewModel.receiptsInRange(firstDayOfCurrent, currentTime).observe(this) { receipts ->
            receipts.let {
                var price = 0f
                it.forEach {receipt -> price += receipt.receiptPrice}
                spendingThisMonth.value = price
                updateSpendingInfoText()
            }
        }

        receiptViewModel.receiptsInRange(firstDayLastMonth, currentLastMonth).observe(this) { receipts ->
            receipts.let {
                var price = 0f
                it.forEach {receipt -> price += receipt.receiptPrice}
                spendingLastMonth.value = price
            }
        }
    }

    private fun updateSpendingInfoText() {
        val currentSpending = spendingThisMonth.value
        val lastSpending = spendingLastMonth.value

        // No previous data available -> no reinforcement possible
        if (lastSpending < 0.01f) {
            return
        }

        val diff = currentSpending - lastSpending
        val aLotLimit = 10f

        var text: String = ""
        val rand = Random(System.currentTimeMillis())
        if (diff > 0) {
            // More money spent this month
            text = if (diff > aLotLimit) {
                val textArr = resources.getStringArray(R.array.reinforcement_negative_strong)
                textArr[rand.nextInt(textArr.size)]
            } else {
                val textArr = resources.getStringArray(R.array.reinforcement_negative)
                textArr[rand.nextInt(textArr.size)]
            }
        } else {
            // Less money spent this month
            text = if (abs(diff) > aLotLimit) {
                val textArr = resources.getStringArray(R.array.reinforcement_positive_strong)
                textArr[rand.nextInt(textArr.size)]
            } else {
                val textArr = resources.getStringArray(R.array.reinforcement_positive)
                textArr[rand.nextInt(textArr.size)]
            }
        }

        additionalText.value = text
    }

    private fun startCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        this.startActivity(intent)
    }

    private fun startNewReceipt() {
        val intent = Intent(this, NewReceiptActivity::class.java)
        this.startActivity(intent)
    }

    private fun startReceiptList() {
        val intent = Intent(this, ReceiptListActivity::class.java)
        this.startActivity(intent)
    }

    private fun startProductList() {
        val intent = Intent(this, ProductListActivity::class.java)
        this.startActivity(intent)
    }

    private fun startSpendingActivity() {
        val intent = Intent(this, SpendingOverviewActivity::class.java)
        this.startActivity(intent)
    }

    private fun startSettingsActivity() {
        val intent = Intent(this, UserSettingsActivity::class.java)
        this.startActivity(intent)
    }

    private fun startCategoryActivity() {
        val intent = Intent(this, CategoryActivity::class.java)
        this.startActivity(intent)
    }

    fun onVersionRetrieved(current: VersionManager.Version, fetched: VersionManager.Version) {
        fetchedAppVersion = fetched
        if (current == fetched)
            return

        // Close popups, show update popup
        showNewReceiptPopup.value = false
        showSpendingPopup.value = false
        showManagementPopup.value = false
        showMessagePopup.value = false
        showUpdatePopup.value = true
    }

    @Composable
    private fun getSpendingTextColor(): Color {
        if (spendingLastMonth.value < 0.01f)
            return if (isSystemInDarkTheme()) {Color.White} else {Color.Black}
        return if (spendingThisMonth.value < spendingLastMonth.value) {DarkGreen} else {Color.Red}
    }

    fun onLatestMessageRetrieved(message: MessageService.UpdateMessage?) {
        if (message == null) {
            Toast.makeText(this, resources.getString(R.string.error_update_message_failed), Toast.LENGTH_SHORT).show()
            return
        }
        // Message already acknowledged
        if (MessageService.currentMessageId != null && message.messageId == MessageService.currentMessageId)
            return

        // Message is meant for a different application version
        if (message.requiredVersion != VersionManager.currentVersion)
            return

        // Save message id to file, so we don't get the message every time we launch the app
        MessageService.currentMessageId = message.messageId
        MessageService.currentMessage = message
        MessageService.saveLatestMessageId(this)

        // Don't display message if update popup is visible
        if (showUpdatePopup.value) {
            return
        }
        showNewReceiptPopup.value = false
        showSpendingPopup.value = false
        showManagementPopup.value = false
        showMessagePopup.value = true
    }

    @Composable
    fun Greeting(modifier: Modifier = Modifier) {
        Column (
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
                ){
            Text(
                text = resources.getString(R.string.main_text_greeting, userName.value),
                modifier = Modifier.padding(6.dp),
                fontSize = 24.sp
            )
            Text(
                text = resources.getString(R.string.main_text_spending_current_header)
            )
            Text(
                text = resources.getString(R.string.item_price, spendingThisMonth.value, UserConfig.currency),
                fontSize = 20.sp,
                color = getSpendingTextColor(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset(1f, 1f),
                        blurRadius = 6f
                    )
                )
            )
            Text(
                text = resources.getString(R.string.main_text_spending_previous_header)
            )
            Text(
                text = resources.getString(R.string.item_price, spendingLastMonth.value, UserConfig.currency),
                fontSize = 20.sp,
            )
            Text(
                text = additionalText.value
            )

            // New receipt actions (camera, create new)
            Button(onClick = {
                showNewReceiptPopup.value = true
            }) {
                Text(text = resources.getString(R.string.header_new_receipt))
            }

            // Spending activities (receipt list, spending overview)
            Button(onClick = {
                showSpendingPopup.value = true
            }) {
                Text(text = resources.getString(R.string.main_text_spending_header))
            }

            // Management activities (category list, product list, settings)
            Button(onClick = {
                showManagementPopup.value = true
            }) {
                Text(text = resources.getString(R.string.main_text_management_header))
            }

            if (showNewReceiptPopup.value) {
                NewReceiptPopup {
                    showNewReceiptPopup.value = false
                }
            }
            if (showSpendingPopup.value) {
                SpendingPopup {
                    showSpendingPopup.value = false
                }
            }
            if (showManagementPopup.value) {
                ManagementPopup {
                    showManagementPopup.value = false
                }
            }
            if (showUpdatePopup.value) {
                UpdatePopup {
                    showUpdatePopup.value = false
                }
            }
            if (showMessagePopup.value) {
                MessagePopup {
                    showMessagePopup.value = false
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        BudgetHelpTheme {
            Greeting()
        }
    }

    @Composable
    fun NewReceiptPopup(onDismiss: () -> Unit) {
        Dialog(
            onDismissRequest = {
                onDismiss()
            }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = resources.getString(R.string.main_text_new_receipt_header),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(6.dp)
                    )
                    Button(onClick = {
                        startCamera()
                        onDismiss()
                    }) {
                        Text(text = resources.getString(R.string.main_button_new_receipt_camera))
                    }
                    Button(onClick = {
                        startNewReceipt()
                        onDismiss()
                    }) {
                        Text(text = resources.getString(R.string.main_button_new_receipt_manual))
                    }
                }
            }
        }
    }

    @Composable
    fun SpendingPopup(onDismiss: () -> Unit) {
        Dialog(
            onDismissRequest = {
                onDismiss()
            }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = resources.getString(R.string.main_text_spending_header),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(6.dp)
                    )
                    Button(onClick = {
                        startSpendingActivity()
                        onDismiss()
                    }) {
                        Text(text = resources.getString(R.string.spending_activity_header))
                    }
                    Button(onClick = {
                        startReceiptList()
                        onDismiss()
                    }) {
                        Text(text = resources.getString(R.string.header_receipt_list))
                    }
                }
            }
        }
    }

    @Composable
    fun ManagementPopup(onDismiss: () -> Unit) {
        Dialog(
            onDismissRequest = {
                onDismiss()
            }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = resources.getString(R.string.main_text_management_header),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(6.dp)
                    )
                    Button(onClick = {
                        startProductList()
                        onDismiss()
                    }) {
                        Text(text = resources.getString(R.string.main_button_product_list))
                    }
                    Button(onClick = {
                        startCategoryActivity()
                        onDismiss()
                    }) {
                        Text(text = resources.getString(R.string.main_button_category_list))
                    }
                    Button(onClick = {
                        startSettingsActivity()
                        onDismiss()
                    }) {
                        Text(text = resources.getString(R.string.settings_header))
                    }
                }
            }
        }
    }

    @Composable
    fun UpdatePopup(onDismiss: () -> Unit) {
        Dialog(
            onDismissRequest = {
                onDismiss()
            }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Column (
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = resources.getString(R.string.app_update_header),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                    Text(
                        text = resources.getString(R.string.app_update_version_compare,
                            VersionManager.currentVersion.toString(), fetchedAppVersion!!.toString()),
                        fontSize = 16.sp
                    )
                    HyperlinkText(
                        modifier = Modifier.padding(8.dp),
                        fullText = resources.getString(R.string.app_update_info),
                        linkText = listOf(resources.getString(R.string.app_update_link_short)),
                        hyperlinks = listOf(VersionManager.RELEASE_URL)
                    )

                    Button(onClick = {
                        updateAcknowledged = true
                        onDismiss()
                    }) {
                        Text(text = resources.getString(R.string.app_update_decline))
                    }
                }
            }
        }
    }

    @Composable
    fun MessagePopup(onDismiss: () -> Unit) {
        Dialog(
            onDismissRequest = {
                onDismiss()
            }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Column (
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = resources.getString(R.string.app_update_changelog_header),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                    Text(
                        text = resources.getString(R.string.app_update_changelog_version,
                            VersionManager.currentVersion.toString()),
                        fontSize = 16.sp
                    )
                    Text(
                        text = MessageService.currentMessage.messageContent,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    )

                    Button(onClick = {
                        messageAcknowledged = true
                        onDismiss()
                    }) {
                        Text(text = resources.getString(R.string.generic_reply_positive))
                    }
                }
            }
        }
    }
}




