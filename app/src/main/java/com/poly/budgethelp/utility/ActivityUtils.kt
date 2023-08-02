package com.poly.budgethelp.utility

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
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
    }
}