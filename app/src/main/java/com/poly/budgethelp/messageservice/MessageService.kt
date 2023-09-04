package com.poly.budgethelp.messageservice

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import com.poly.budgethelp.MainActivity
import com.poly.budgethelp.config.UserConfig
import com.poly.budgethelp.config.VersionManager
import com.poly.budgethelp.volleyExtensions.Utf8StringRequest
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader

class MessageService {
    data class UpdateMessage(
        val messageId: String,
        val requiredVersion: VersionManager.Version,
        val messageContent: String
    )
    companion object {
        private const val TAG = "MessageService"
        private const val MESSAGE_URL = "DUMMY_REPLACE_ME"
        private const val MESSAGE_SAVE_FILE = "versionData"
        var currentMessage: UpdateMessage = UpdateMessage("0", VersionManager.currentVersion, "null")
        var currentMessageId: String? = null

        fun fetchMessage(context: MainActivity) {
            getLatestMessageId(context)
            val queue = Volley.newRequestQueue(context)
            val request = Utf8StringRequest(Request.Method.GET, MESSAGE_URL, {response ->
                Log.d(TAG, response)
                val contents = response.split("|")
                if (contents.size != 3) {
                    Log.d(TAG, "Message file is in invalid format, content size: " + contents.size)
                    context.onLatestMessageRetrieved(null)
                    return@Utf8StringRequest
                }
                val messageId: String = contents[0]
                val version: VersionManager.Version? = VersionManager.stringToVersionOrNull(contents[1])
                val messageContent: String = contents[2]

                if (version == null) {
                    Log.d(TAG, "Version specified in the message is in invalid format")
                    context.onLatestMessageRetrieved(null)
                    return@Utf8StringRequest
                }

                context.onLatestMessageRetrieved(UpdateMessage(messageId, version, messageContent))
            },
            {error ->
                context.onLatestMessageRetrieved(null)
            })
            queue.add(request)
        }

        private fun getLatestMessageId(context: Context) {
            val inputStream: FileInputStream
            try {
                inputStream = context.openFileInput(MESSAGE_SAVE_FILE)
                val inputReader: InputStreamReader = InputStreamReader(inputStream)
                val bufferedReader: BufferedReader = BufferedReader(inputReader)

                val fileContent = bufferedReader.readLine()
                currentMessageId = fileContent

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        fun saveLatestMessageId(context: Context): Boolean {
            if (currentMessageId == null)
                return false

            val outputStream: FileOutputStream
            try {
                outputStream = context.openFileOutput(MESSAGE_SAVE_FILE, Context.MODE_PRIVATE)
                outputStream.write(currentMessageId!!.encodeToByteArray())
                outputStream.close()
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
    }
}