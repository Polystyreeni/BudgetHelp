package com.poly.budgethelp

import android.R.attr.data
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE
import androidx.activity.ComponentActivity
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
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.poly.budgethelp.ui.theme.BudgetHelpTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Column {
            Text(
                text = "Hello $name!",
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




