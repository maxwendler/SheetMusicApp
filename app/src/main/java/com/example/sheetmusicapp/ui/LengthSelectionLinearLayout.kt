package com.example.sheetmusicapp.ui

import android.content.Context
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import com.example.sheetmusicapp.R
import com.example.sheetmusicapp.scoreModel.BasicRhythmicLength
import com.example.sheetmusicapp.scoreModel.NoteHeadType

class LengthSelectionLinearLayout(context: Context, noteHeadType: NoteHeadType?, private val isDotted: Boolean) : LinearLayout(context) {

    val imageButtons = mutableListOf<ImageButton>()
    var highlightedButton : ImageButton? = null
    var highlightedLength : BasicRhythmicLength? = null
    val layoutParamsForElements = LayoutParams(0, LayoutParams.MATCH_PARENT)
    init {
        layoutParamsForElements.weight = 1f
        orientation = HORIZONTAL
        when (noteHeadType){
            NoteHeadType.ELLIPTIC -> addAllNoteImages()
            NoteHeadType.CROSS -> addCrossNoteImages()
            null -> addRestImages()
        }
    }

    private fun addAllNoteImages(){
        val resourceIds = mutableListOf<Int>(R.drawable.ic_whole, R.drawable.ic_half, R.drawable.ic_quarter, R.drawable.ic_eighth)
        val lengths = mutableListOf(BasicRhythmicLength.WHOLE, BasicRhythmicLength.HALF, BasicRhythmicLength.QUARTER, BasicRhythmicLength.EIGHTH)
        if (!isDotted){
            resourceIds.add(R.drawable.ic_sixteenth)
            lengths.add(BasicRhythmicLength.SIXTEENTH)
        }
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

    private fun addCrossNoteImages(){
        val resourceIds = mutableListOf<Int>(R.drawable.ic_x_quarter, R.drawable.ic_x_eighth)
        val lengths = mutableListOf(BasicRhythmicLength.QUARTER, BasicRhythmicLength.EIGHTH)
        if (!isDotted){
            resourceIds.add(R.drawable.ic_x_sixteenth)
            lengths.add(BasicRhythmicLength.SIXTEENTH)
        }
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

    private fun addRestImages(){
        val resourceIds = mutableListOf<Int>(R.drawable.ic_rest_half, R.drawable.ic_rest_half, R.drawable.ic_rest_quarter, R.drawable.ic_rest_eighth)
        val lengths = mutableListOf(BasicRhythmicLength.WHOLE, BasicRhythmicLength.HALF, BasicRhythmicLength.QUARTER, BasicRhythmicLength.EIGHTH)
        if (!isDotted){
            resourceIds.add(R.drawable.ic_rest_sixteenth)
            lengths.add(BasicRhythmicLength.SIXTEENTH)
        }
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