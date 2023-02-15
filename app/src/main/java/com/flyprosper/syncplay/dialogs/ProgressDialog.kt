package com.flyprosper.syncplay.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.widget.ProgressBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.flyprosper.syncplay.R


class ProgressDialog(context: Context) {
    private var _dialog: Dialog? = null
    private val dialog get() = _dialog

    fun show() {
        dialog?.show()
    }

    fun dismiss() {
        dialog?.dismiss()
    }

    init {
        _dialog = Dialog(context)
        val progressBar = ProgressBar(context)
        progressBar.indeterminateTintList =
            AppCompatResources.getColorStateList(context, R.color.icon_tint)
        dialog?.setContentView(progressBar)
        val r = 24f
        val p = 20
        val shape = ShapeDrawable(RoundRectShape(floatArrayOf(r, r, r, r, r, r, r, r), null, null))
        shape.paint.color = ContextCompat.getColor(context, R.color.background_dialog)
        shape.setPadding(p, p, p, p)

        dialog?.window?.setBackgroundDrawable(shape)
        dialog?.setCancelable(false)
    }
}