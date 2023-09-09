package com.poly.budgethelp.utility

import android.content.res.Configuration
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.ComponentActivity
import com.poly.budgethelp.R

class ActivityUtils {
    companion object {
        fun createPopup(popupId: Int, context: AppCompatActivity): Pair<View, PopupWindow> {
            val popupView = LayoutInflater.from(context).inflate(popupId, null)
            val width: Int = LinearLayout.LayoutParams.WRAP_CONTENT
            val height: Int = LinearLayout.LayoutParams.WRAP_CONTENT

            val popupWindow = PopupWindow(popupView, width, height, false)
            popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

            return Pair(popupView, popupWindow)
        }
        fun createPopup(popupId: Int, context: ComponentActivity): Pair<View, PopupWindow> {
            val popupView = LayoutInflater.from(context).inflate(popupId, null)
            val width: Int = LinearLayout.LayoutParams.WRAP_CONTENT
            val height: Int = LinearLayout.LayoutParams.WRAP_CONTENT

            val popupWindow = PopupWindow(popupView, width, height, false)
            popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

            return Pair(popupView, popupWindow)
        }

        /***
         * Checks to see if the device is using dark mode, or light mode.
         * For android versions that don't support dark mode, this is always false
         */
        fun isUsingNightModeResources(activity: AppCompatActivity): Boolean {
            return when (activity.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> true
                Configuration.UI_MODE_NIGHT_NO -> false
                Configuration.UI_MODE_NIGHT_UNDEFINED -> false
                else -> false
            }
        }
    }
}