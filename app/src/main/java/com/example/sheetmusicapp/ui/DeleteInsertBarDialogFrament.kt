package com.example.sheetmusicapp.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import java.lang.ClassCastException

class DeleteInsertBarDialogFragment(private val deleteEnabled: Boolean) : DialogFragment() {

    lateinit var listener: BarsModListener

    interface BarsModListener {
        fun onBarInsertButtonClick()
        fun onBarDeleteButtonClick()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as BarsModListener
        }
        catch(e: ClassCastException) {
            throw ClassCastException("$context must implement BarsModListener!")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let{
            val builder = AlertDialog.Builder(it)

            val message =
                if (deleteEnabled) "Delete current bar or insert empty bar after it?"
                else "Insert empty bar after current? \n(Can't delete the only bar!)"

            builder.setTitle("Modify score bars")
                .setMessage(message)
                .setNeutralButton("CANCEL", { dialog, _ -> dialog.cancel()})
                .setPositiveButton("INSERT", { _, _ -> listener.onBarInsertButtonClick() })

            if (deleteEnabled) builder.setNegativeButton("DELETE", { _, _ -> listener.onBarDeleteButtonClick() })

            builder.create()
        } ?: throw IllegalStateException("Activity can't be null!")
    }
}