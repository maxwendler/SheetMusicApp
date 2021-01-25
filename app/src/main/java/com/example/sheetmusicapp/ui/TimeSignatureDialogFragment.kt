package com.example.sheetmusicapp.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.sheetmusicapp.R
import com.example.sheetmusicapp.scoreModel.TimeSignature
import java.lang.ClassCastException

class TimeSignatureDialogFragment(private val timeSignature: TimeSignature) : DialogFragment() {

    lateinit var denominatorEditText : EditText
    lateinit var numeratorEditText: EditText
    internal lateinit var listener: NewSignatureListener

    interface NewSignatureListener {
        fun onDialogPositiveClick(dialog : TimeSignatureDialogFragment)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as NewSignatureListener
        }
        catch(e: ClassCastException) {
            throw ClassCastException("$context must implement NewSignatureListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater

            val layout = inflater.inflate(R.layout.dialog_time_signature, null)

            numeratorEditText = layout.findViewById<EditText>(R.id.dialog_numerator)
                    ?: throw IllegalStateException("Numerator edit text can not be found by id!")
            numeratorEditText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    evaluateNumerator(v.text)
                }
                false
            }
            numeratorEditText.setText(timeSignature.numerator.toString())
            denominatorEditText = layout.findViewById<EditText>(R.id.dialog_denominator)
                    ?: throw IllegalStateException("Denominator edit text can not be found by id!")
            denominatorEditText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE){
                    evaluateDenominator(v.text)
                }
                false
            }
            denominatorEditText.setText(timeSignature.denominator.toString())

            builder.setView(layout)
                    .setPositiveButton("OK",  { _, _ -> listener.onDialogPositiveClick(this) })
                    .setNegativeButton("CANCEL", { dialog, _ -> dialog.cancel()})
                    .setTitle("Change time signature")


            builder.create()
        } ?: throw IllegalStateException("Activity can't be null!")
    }

    private fun evaluateNumerator(numeratorText : CharSequence){
        if (numeratorText.toString() == "") return
        val numerator = numeratorText.toString().toInt()

        val denominatorText = denominatorEditText.text
        var denominator : Int? = null
        if (denominatorText.toString() != ""){
            denominator = denominatorText.toString().toInt()
        }

        if (denominator == 8){
            if (numerator > 12) {
                numeratorEditText.hint = "1-12 for eighths"
                numeratorEditText.setText("")
                Toast.makeText(context, "Numerator must be in 1 to 12 for eights!", Toast.LENGTH_LONG).show()
            }
        }
        else {
            if (denominator != null){
                numeratorEditText.hint = "numerator"
            }
        }
    }

    private fun evaluateDenominator(denominatorText : CharSequence){
        if (denominatorText.toString() == "") {
            numeratorEditText.hint = "numerator"
            return
        }
        val denominator = denominatorText.toString().toInt()

        val numeratorText = numeratorEditText.text
        var numerator : Int? = null
        if (numeratorText.toString() != ""){
            numerator = numeratorText.toString().toInt()
        }

        if (denominator !in listOf(2,4,8)){
            denominatorEditText.setText("")
            Toast.makeText(context, "Denominator must be 2, 4 or 8!", Toast.LENGTH_SHORT).show()
        }

        if (denominator == 8){
            numeratorEditText.hint = "1-12 for eighths"
            if (numerator != null) {
                if (numerator > 12) {
                    denominatorEditText.setText("")
                    Toast.makeText(context, "Numerator must be in 1 to 12 for eights!", Toast.LENGTH_LONG).show()
                }
            }
        }
        else {
            numeratorEditText.hint = "numerator"
        }
    }

}