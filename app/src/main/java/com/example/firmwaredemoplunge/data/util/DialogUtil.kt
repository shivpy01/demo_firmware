package com.example.firmwaredemoplunge.data.util

import android.app.Dialog
import android.content.Context
import com.example.firmwaredemoplunge.R
import java.lang.ref.WeakReference

object DialogUtil {

    fun progressBarLoader(context: Context, isCancel: Boolean = false): Dialog {
        val weakref = WeakReference(context)
        val dialog = Dialog(weakref.get()!!, R.style.MaterialDialog)
        dialog.setContentView(R.layout.layout_progressbar)
        dialog.setCancelable(isCancel)
        dialog.setCanceledOnTouchOutside(isCancel)
        return dialog
    }
}