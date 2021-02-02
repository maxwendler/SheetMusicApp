package com.example.sheetmusicapp.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import java.lang.ClassCastException

/**
 * Dialog fragment for entering a title to set to the current score and forward the result to
 * the [SetTitleListener] callback.
 * On "OK", should forward the resulting title to a listener [Context] which shows this dialog fragment.
 * MainActivity in this app.
 *
 * @param currentTitle The title initially contained by the fragment's [titleEditText].
 */
class SetTitleDialogFragment(private val currentTitle: String) : DialogFragment() {

    lateinit var listener: SetTitleListener
    lateinit var titleEditText: EditText

    interface SetTitleListener {
        fun saveWithTitle(title: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as SetTitleListener
        }
        catch(e: ClassCastException) {
            throw ClassCastException("$context must implement SetTitleListener!")
        }
    }

    /**
     * Fills dialog with new instance for [titleEditText] and prepares "OK" callback by setting IME_ACTION_DONE .
     * Does not set positive button onClickListener (see onResume()), because it needs properties
     * only available after this function.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialog = activity?.let {
            val builder = AlertDialog.Builder(it)

            titleEditText = EditText(context)
            titleEditText.id = ViewGroup.generateViewId()
            titleEditText.hint = "title"
            titleEditText.setText(currentTitle)
            titleEditText.imeOptions = EditorInfo.IME_ACTION_DONE

            builder.setTitle("Set title").setView(titleEditText)
                    .setNegativeButton("CANCEL", { dialog, _ -> dialog.cancel()})
                    .setPositiveButton("OK", {_, _ -> })

            builder.create()
        } ?: throw IllegalStateException("Activity can't be null!")

        return alertDialog
    }

    /**
     * Sets "OK" button onClickListener, which calls the listeners [SetTitleListener.saveWithTitle]
     * if evaluation of current [titleEditText] content succeeds in [evaluateTitle].
     */
    override fun onResume() {
        super.onResume()
        val alertDialog = dialog as AlertDialog?
        if (alertDialog != null){
            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val title = titleEditText.text.toString()
                if (evaluateTitle(title)){
                    listener.saveWithTitle(title)
                    dismiss()
                }
            }
        }
    }

    /**
     * Checks a string for constraints for titles. Right now: Titles can't be empty.
     * Returns true, if it fits them, false otherwise.
     */
    private fun evaluateTitle(title: String) : Boolean{
        if (title != ""){
            return true
        }
        else Toast.makeText(context, "Title can't be empty!", Toast.LENGTH_SHORT).show()
        return false
    }
}