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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.poly.budgethelp.config.UserConfig
import com.poly.budgethelp.config.VersionManager
import com.poly.budgethelp.messageservice.MessageService
import com.poly.budgethelp.ui.theme.BudgetHelpTheme
import com.poly.budgethelp.utility.ActivityUtils
import com.poly.budgethelp.utility.TextUtils


class MainActivity : ComponentActivity() {

    private val activePopups: ArrayList<PopupWindow> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val configExists: Boolean = UserConfig.readConfig(this)
        if (!configExists) {
            startSettingsActivity()
        }

        VersionManager.fetchLatestVersion(this)
        MessageService.fetchMessage(this)

        onBackPressedDispatcher.addCallback(this) {
            if (activePopups.size > 0) {
                val popup = activePopups[activePopups.size - 1]
                activePopups.remove(popup)
                popup.dismiss()
            } else {
                finish()
            }
        }

        setContent {
            BudgetHelpTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
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
        if (current == fetched)
            return

        val popupData = ActivityUtils.createPopup(R.layout.popup_update_available, this)

        val versionText: TextView = popupData.first.findViewById(R.id.updateVersionText)
        val versionLink: TextView = popupData.first.findViewById(R.id.updateDirectLink)
        val declineButton: Button = popupData.first.findViewById(R.id.updateDeclineButton)
        versionText.text = resources.getString(R.string.app_update_version_compare,
            current.toString(), fetched.toString())
        versionLink.text = VersionManager.RELEASE_URL
        Linkify.addLinks(versionLink, Linkify.WEB_URLS)

        declineButton.setOnClickListener { popupData.second.dismiss() }

        popupData.second.setOnDismissListener {
            activePopups.remove(popupData.second)
        }
        popupData.second.isFocusable = false
        activePopups.add(popupData.second)
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
        MessageService.saveLatestMessageId(this)

        val popupData = ActivityUtils.createPopup(R.layout.popup_update_changelog, this)
        val versionTextView: TextView = popupData.first.findViewById(R.id.updateVersionText)
        val contentTextView: TextView = popupData.first.findViewById(R.id.updateChangeLogText)
        val okButton: Button = popupData.first.findViewById(R.id.updateChangelogButton)

        versionTextView.text = resources.getString(R.string.app_update_changelog_version,
            message.requiredVersion.toString())

        contentTextView.text = TextUtils.getSpannedText(message.messageContent)
        okButton.setOnClickListener { popupData.second.dismiss() }

        popupData.second.isFocusable = false
        activePopups.add(popupData.second)
        popupData.second.setOnDismissListener {
            activePopups.remove(popupData.second)
        }
    }

    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Column {
            Text(
                text = "Hello ${UserConfig.userName}!",
                modifier = modifier
            )
            Button(onClick = { startCamera() }) {
                Text(text = "Start camera")
            }
            Button(onClick = { startNewReceipt() }) {
                Text(text = "New receipt")
            }
            Button(onClick = { startReceiptList() }) {
                Text(text = "Receipt list")
            }
            Button(onClick = { startProductList() }) {
                Text(text = "Product list")
            }
            Button(onClick = { startSpendingActivity() }) {
                Text(text = "Spending")
            }
            Button(onClick = {startCategoryActivity()}) {
                Text(text = "Categories")
            }
            Button(onClick = { startSettingsActivity() }) {
                Text(text = "Settings")
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        BudgetHelpTheme {
            Greeting("Android")
        }
    }
}




