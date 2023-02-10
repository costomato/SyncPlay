package com.flyprosper.syncplay.dialogs

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.ShapeDrawable
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import com.flyprosper.syncplay.R

class CustomAlertDialog {

    interface OnChoiceClickListener {
        fun onPositiveClick()
    }

    fun builder(context: Context, listener: OnChoiceClickListener): AlertDialog.Builder {
        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.CustomAlertDialog))
        builder.setTitle("Confirm")
        builder.setMessage("Are you sure you want to do this?")
        builder.setPositiveButton("Confirm") { _, _ ->
            listener.onPositiveClick()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        return builder
    }

    fun createDialog(builder: AlertDialog.Builder): AlertDialog {
        val dialog = builder.create()

        val window = dialog.window
        window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val background = window?.decorView?.background
        if (background is ShapeDrawable) {
            // Set the corner radius
            background.paint.shader = LinearGradient(
                0f, 0f, 0f, background.intrinsicHeight.toFloat(),
                intArrayOf(Color.WHITE, Color.WHITE),
                null,
                Shader.TileMode.CLAMP
            )
        }
        return dialog
    }
}