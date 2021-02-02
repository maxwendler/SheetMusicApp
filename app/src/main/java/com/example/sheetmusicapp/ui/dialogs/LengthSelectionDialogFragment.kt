package com.example.sheetmusicapp.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.sheetmusicapp.scoreModel.BasicRhythmicLength
import com.example.sheetmusicapp.scoreModel.NoteHeadType

/**
 * Class for dialog showing and allowing selection of a [BasicRhythmicLength], depending on what's
 * supported for different note head types ([noteHeadType]) and (non-)dotted notes ([isDotted]).
 * On "OK", forwards the resulting length to a listener [Context] which shows this dialog fragment.
 * MainActivity in this app.
 */
class LengthSelectionDialogFragment(private val noteHeadType: NoteHeadType?, private val isDotted: Boolean) : DialogFragment(){

    lateinit var listener: NewLengthListener

    interface NewLengthListener {
        fun onLengthDialogPositiveClick(newLength: BasicRhythmicLength?)
    }

    /**
     * Manages dynamic content by forwarding class parameters to [LengthSelectionLinearLayout] constructor.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let{
            val builder = AlertDialog.Builder(it)

            val context = context ?: throw IllegalStateException("Context can't be null!")
            val contentLayout = LengthSelectionLinearLayout(context, noteHeadType, isDotted)
            builder.setView(contentLayout).setTitle("Select length")
                    .setPositiveButton("OK",  { _, _ -> listener.onLengthDialogPositiveClick(contentLayout.highlightedLength) })
                    .setNegativeButton("CANCEL", { dialog, _ -> dialog.cancel()})

            builder.create()
        } ?: throw IllegalStateException("Activity can't be null!")
    }
}