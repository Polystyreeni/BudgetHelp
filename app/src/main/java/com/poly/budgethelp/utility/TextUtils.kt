package com.poly.budgethelp.utility

import android.os.Build
import android.text.Html
import android.text.Spanned
import android.util.Log
import com.poly.budgethelp.NewReceiptActivity
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
                .replace(System.lineSeparator(), " ")
        }

        /**
         * Calculates Jaro similarity of two strings
         *
         */
        fun jaroDistance(s1: String, s2: String): Float {
            if (s1 == s2)
                return 1.0f

            val len1 = s1.length
            val len2 = s2.length

            if (len1 == 0 || len2 == 0)
                return 0.0f

            val maxDist: Int = floor(max(len1, len2) / 2.0f).roundToInt() - 1

            var match = 0

            val hashS1: MutableList<Int> = MutableList(len1) { 0 }
            val hashS2: MutableList<Int> = MutableList(len2) { 0 }

            for (i in 0 until len1) {

                // Check for matches
                val start = max(0, i - maxDist)
                val end = min(len2, i + maxDist + 1)
                for (j in start until end) {

                    // Found a match
                    if (s1[i] == s2[j] && hashS2[j] == 0) {
                        hashS1[i] = 1
                        hashS2[j] = 1
                        match++
                        break
                    }
                }
            }

            // No match found
            if (match == 0)
                return 0.0f

            var t: Double = 0.0
            var point = 0

            for (i in 0 until len1) {
                if (hashS1[i] == 1) {

                    while (hashS2[point] == 0)
                        point++

                    if (s1[i] != s2[point++])
                        t++
                }
            }

            t /= 2

            return ( match.toFloat() / len1.toFloat()
                    + match.toFloat() / len2.toFloat()
                    + (match - t).toFloat() / match.toFloat() ) / 3.0f
        }
    }
}