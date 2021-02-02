package com.example.sheetmusicapp.ui.dialogs

import android.content.Context
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import com.example.sheetmusicapp.R
import com.example.sheetmusicapp.scoreModel.BasicRhythmicLength
import com.example.sheetmusicapp.scoreModel.NoteHeadType

/**
 * LinearLayout that contains a list of [BasicRhythmicLength] note visualizations, based on what's
 * supported for different note head types and limitations for dotted notes.
 */
class LengthSelectionLinearLayout(context: Context, noteHeadType: NoteHeadType?, private val isDotted: Boolean) : LinearLayout(context) {

    private val imageButtons = mutableListOf<ImageButton>()
    // User can select a button, i.e. a length, via click.
    var highlightedButton : ImageButton? = null
    var highlightedLength : BasicRhythmicLength? = null
    private val layoutParamsForElements = LayoutParams(0, LayoutParams.MATCH_PARENT)
    // Fill the layout.
    init {
        layoutParamsForElements.weight = 1f
        orientation = HORIZONTAL
        when (noteHeadType){
            NoteHeadType.ELLIPTIC -> addAllNoteImageButtons()
            NoteHeadType.CROSS -> addCrossNoteImageButtons()
            null -> addRestImageButtons()
        }
    }

    /**
     * Adds image buttons for elliptic note heads.
     */
    private fun addAllNoteImageButtons(){

        // Create lists to iterate over for button creation.
        val resourceIds = mutableListOf<Int>(R.drawable.ic_whole, R.drawable.ic_half, R.drawable.ic_quarter, R.drawable.ic_eighth)
        val lengths = mutableListOf(BasicRhythmicLength.WHOLE, BasicRhythmicLength.HALF, BasicRhythmicLength.QUARTER, BasicRhythmicLength.EIGHTH)
        // Dotted sixteenths are not supported.
        if (!isDotted){
            resourceIds.add(R.drawable.ic_sixteenth)
            lengths.add(BasicRhythmicLength.SIXTEENTH)
        }
        // Create image buttons while setting highlightButton() as onClickListener.
        for (i in resourceIds.indices){
            val id = resourceIds[i]
            val length = lengths[i]
            val imageButton = ImageButton(context)
            imageButton.id = generateViewId()
            imageButton.setImageResource(id)
            imageButton.scaleType = ImageView.ScaleType.FIT_CENTER
            imageButton.layoutParams = layoutParamsForElements
            imageButton.setOnClickListener {
                highlightButton(it as ImageButton, length)
            }
            addView(imageButton)
            imageButtons.add(imageButton)
        }
    }

    /**
     * Adds image buttons for cross note heads.
     */
    private fun addCrossNoteImageButtons(){

        // Create lists to iterate over for button creation.
        val resourceIds = mutableListOf<Int>(R.drawable.ic_x_quarter, R.drawable.ic_x_eighth)
        val lengths = mutableListOf(BasicRhythmicLength.QUARTER, BasicRhythmicLength.EIGHTH)
        // Dotted sixteenths are not supported.
        if (!isDotted){
            resourceIds.add(R.drawable.ic_x_sixteenth)
            lengths.add(BasicRhythmicLength.SIXTEENTH)
        }
        // Create image buttons while setting highlightButton() as onClickListener.
        for (i in resourceIds.indices){
            val id = resourceIds[i]
            val length = lengths[i]
            val imageButton = ImageButton(context)
            imageButton.id = generateViewId()
            imageButton.setImageResource(id)
            imageButton.scaleType = ImageView.ScaleType.FIT_CENTER
            imageButton.layoutParams = layoutParamsForElements
            imageButton.setOnClickListener {
                highlightButton(it as ImageButton, length)
            }
            addView(imageButton)
            imageButtons.add(imageButton)
        }
    }

    /**
     * Adds image buttons for rests.
     */
    private fun addRestImageButtons(){

        // Create lists to iterate over for button creation.
        val resourceIds = mutableListOf<Int>(R.drawable.ic_rest_half, R.drawable.ic_rest_half, R.drawable.ic_rest_quarter, R.drawable.ic_rest_eighth)
        val lengths = mutableListOf(BasicRhythmicLength.WHOLE, BasicRhythmicLength.HALF, BasicRhythmicLength.QUARTER, BasicRhythmicLength.EIGHTH)
        // Dotted sixteenths are not supported.
        if (!isDotted){
            resourceIds.add(R.drawable.ic_rest_sixteenth)
            lengths.add(BasicRhythmicLength.SIXTEENTH)
        }
        // Create image buttons while setting highlightButton() as onClickListener. Full rest is vertically mirrored half.
        for (i in resourceIds.indices){
            val id = resourceIds[i]
            val length = lengths[i]
            val imageButton = ImageButton(context)
            imageButton.id = generateViewId()
            imageButton.setImageResource(id)
            imageButton.scaleType = ImageView.ScaleType.FIT_CENTER
            if (i == 0){
                imageButton.scaleY = -1f
            }
            imageButton.layoutParams = layoutParamsForElements
            imageButton.setOnClickListener {
                highlightButton(it as ImageButton, length)
            }
            addView(imageButton)
            imageButtons.add(imageButton)
        }
    }

    /**
     * Highlights the given [button] via coloring, and sets [highlightedButton] and [highlightedLength].
     * Removes highlighting from other buttons.
     */
    private fun highlightButton(button: ImageButton, length: BasicRhythmicLength){
        if (button != highlightedButton){
            for (imageButton in imageButtons){
                if (imageButton == button){
                    imageButton.setColorFilter(resources.getColor(R.color.purple_200))
                }
                else {
                    imageButton.clearColorFilter()
                }
            }
            highlightedButton = button
            highlightedLength = length
        }
    }
}