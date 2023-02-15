package com.flyprosper.syncplay.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.text.Html
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.flyprosper.syncplay.R

class CustomAlertDialog {

    interface OnChoiceClickListener {
        fun onPositiveClick()
    }

    private lateinit var context: Context

    fun builder(
        title: String,
        message: String,
        context: Context,
        listener: OnChoiceClickListener
    ): AlertDialog.Builder {
        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.CustomAlertDialog))

        @SuppressLint("ResourceType")
        var textColor = context.resources.getString(R.color.text_primary)
        textColor = textColor.substring(3 until textColor.length)

        builder.setTitle(
            Html.fromHtml(
                "<font color='#$textColor'>$title</font>",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        )

        builder.setMessage(
            Html.fromHtml(
                "<font color='#$textColor'>$message</font>",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        )
        builder.setPositiveButton("Confirm") { _, _ ->
            listener.onPositiveClick()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setIcon(R.drawable.logo)

        this.context = context

        return builder
    }

    fun createDialog(builder: AlertDialog.Builder): AlertDialog {
        val dialog = builder.create()

        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val r = 24f
        val p = 20
        val shape = ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null))
        shape.paint.color = ContextCompat.getColor(context, R.color.background_dialog)
        shape.setPadding(p, p, p, p)

        dialog.window?.setBackgroundDrawable(shape)

        return dialog
    }
}