package com.poly.budgethelp.utility

import android.os.Build
import android.text.Html
import android.text.Spanned
import com.poly.budgethelp.NewReceiptActivity

class TextUtils {
    companion object {
        private val TAG = "TextUtils"

        /**
         * Gets string text as spanned, which supports displaying html tags like <b>, <i> etc.
         * Use this for text views
         */
        fun getSpannedText(text: String): Spanned {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
            } else {
                Html.fromHtml(text)
            }
        }

        /**
         * Removes all "forbidden" characters from the text provided
         * Forbidden characters mean such characters that are used in save file parsing
         *
         */
        fun sanitizeText(text: String): String {
            return text.replace(NewReceiptActivity.saveFileDelimiter, " ")
        }
    }
}