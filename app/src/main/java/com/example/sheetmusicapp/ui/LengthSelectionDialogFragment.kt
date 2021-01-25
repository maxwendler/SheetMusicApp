package com.example.sheetmusicapp.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.sheetmusicapp.scoreModel.BasicRhythmicLength
import com.example.sheetmusicapp.scoreModel.NoteHeadType

class LengthSelectionDialogFragment(private val noteHeadType: NoteHeadType?, private val isDotted: Boolean) : DialogFragment(){

    lateinit var listener: NewLengthListener

    interface NewLengthListener {
        fun onLengthDialogPositiveClick(newLength: BasicRhythmicLength?)
    }

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