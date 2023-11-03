package com.poly.budgethelp.config

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader

class UserConfig {

    interface MethodRunner {
        fun run(arg: Any)
    }

    companion object {
        private const val CONFIGFILENAME: String = "userconfig"
        private const val FILEDELIMITER: String = "/"
        const val DEFAULTUSERNAME: String = "Tuntematon"
        var userName = DEFAULTUSERNAME
        var currency: String = "€"
        var productMaxPrice: Float = 1000f
        var priceNameMaxOffset: Int = 40
        var darkMode: Boolean = false
        var similarityRequirement: Float = 0.85f

        private val userSettings: List<MethodRunner> = listOf(
            object : MethodRunner {
                override fun run(arg: Any) {
                    val value: String = arg.toString()
                    userName = value
                }
            },
            object : MethodRunner {
                override fun run(arg: Any) {
                    val value: String = arg.toString()
                    currency = value
                }
            },
            object : MethodRunner {
                override fun run(arg: Any) {
                    val value: Float? = arg.toString().toFloatOrNull()
                    if (value != null) productMaxPrice = value
                }
            },
            object : MethodRunner {
                override fun run(arg: Any) {
                    val value: Int? = arg.toString().toIntOrNull()
                    if (value != null) priceNameMaxOffset = value
                }
            },
            object: MethodRunner {
                override fun run(arg: Any) {
                    val value: Boolean? = arg.toString().toBooleanStrictOrNull()
                    if (value != null) darkMode = value
                }
            },
            object: MethodRunner {
                override fun run(arg: Any) {
                    val value: Float? = arg.toString().toFloatOrNull()
                    if (value != null) similarityRequirement = value
                }
            }
        )

        fun readConfig(context: Context): Boolean {
            val inputStream: FileInputStream
            try {
                inputStream = context.openFileInput(CONFIGFILENAME)
                val inputReader: InputStreamReader = InputStreamReader(inputStream)
                val bufferedReader: BufferedReader = BufferedReader(inputReader)

                val fileContent = bufferedReader.readLine()
                val configValues: List<String> = fileContent.split(FILEDELIMITER)

                // Read settings file
                for (i in configValues.indices) {
                    Log.d("UserConfig", i.toString())
                    userSettings[i].run(configValues[i])
                }

                return true

            } catch (ex: Exception) {
                ex.printStackTrace()
                return false
            }
        }

        fun writeConfig(context: Context): Boolean {
            val outputStream: FileOutputStream
            try {
                outputStream = context.openFileOutput(CONFIGFILENAME, Context.MODE_PRIVATE)
                val settings: String = StringBuilder()
                    .append(userName).append(FILEDELIMITER)
                    .append(currency).append(FILEDELIMITER)
                    .append(productMaxPrice).append(FILEDELIMITER)
                    .append(priceNameMaxOffset).append(FILEDELIMITER)
                    .append(darkMode).append(FILEDELIMITER)
                    .append(similarityRequirement)
                    .toString()

                outputStream.write(settings.encodeToByteArray())
                outputStream.close()
                return true
            }
            catch(ex: Exception) {
                ex.printStackTrace()
                return false
            }
        }
    }
}