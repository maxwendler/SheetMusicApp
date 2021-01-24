package com.example.sheetmusicapp.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import com.example.sheetmusicapp.R
import com.example.sheetmusicapp.scoreModel.Bar
import com.example.sheetmusicapp.scoreModel.Score
import kotlin.IllegalArgumentException

class ScoreEditingLayout (context: Context, val prevBarButton: ImageButton ,private val barHeight: Int, score: Score, initBarIdx : Int = 0) : ConstraintLayout(context) {

    val voiceGridOverlays : MutableMap<Int, BarEditingOverlayLayout> = mutableMapOf()
    var activeVoiceOverlayNum = 1
    var barVisLayout : BarVisLayout? = null
    
    val bars = score.barList
    var previousButtonDisabled = initBarIdx == 0
    
    var bar : Bar = run {
        if (bars.size <= 0){
            throw IllegalArgumentException("Don't create empty scores!")
        }
        if (initBarIdx >= bars.size){
            throw IllegalArgumentException("initBarIdx exceeds score bar list!")
        }
        bars[initBarIdx]
    }
        // bar set //
        set(value) {
            val currentBarVisLayout = barVisLayout
                    ?: throw IllegalStateException("Bar can't be reset if a bar visualization layout wasn't added yet!")
            currentBarVisLayout.bar = value
            field = value
        }
    var barIdx = initBarIdx


    init {
        doOnLayout {
            val newBarVisLayout = addBarVisLayout(bar)
            for (i in 1..4){
                val newOverlay = addBarEditingOverlayLayout()
                voiceGridOverlays[i] = newOverlay
                newOverlay.visibility = INVISIBLE
            }
            voiceGridOverlays[1]?.visibility = VISIBLE
            newBarVisLayout.setEditingOverlayCallback { horizontalMargins, voiceNum ->
                if (voiceNum !in 1..4){
                    throw IllegalArgumentException("Only voices 1 to 4 can exist!")
                }
                val voiceGridOverlay = voiceGridOverlays[voiceNum]
                if (voiceGridOverlay != null) {
                    voiceGridOverlay.horizontalMargins = horizontalMargins
                    if (voiceGridOverlay.width > 0) {
                        voiceGridOverlay.createOverlay()
                    }
                }
            }
            barVisLayout = newBarVisLayout
        }
    }

    private fun addBarVisLayout(bar: Bar) : BarVisLayout{

        val barVisLayout = BarVisLayout(context, barHeight, bar)
        barVisLayout.id = ViewGroup.generateViewId()
        barVisLayout.tag = "barVisLayout"
        barVisLayout.layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        this.addView(barVisLayout)

        return barVisLayout
    }

    private fun addBarEditingOverlayLayout() : BarEditingOverlayLayout{

        val barEditingOverlayLayout = BarEditingOverlayLayout(context, barHeight)
        barEditingOverlayLayout.id = generateViewId()
        barEditingOverlayLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (barHeight * 1.75).toInt())
        this.addView(barEditingOverlayLayout)

        val constraintSet = ConstraintSet()
        constraintSet.clone(this)
        constraintSet.connect(barEditingOverlayLayout.id, ConstraintSet.TOP, this.id, ConstraintSet.TOP)
        constraintSet.connect(barEditingOverlayLayout.id, ConstraintSet.BOTTOM, this.id, ConstraintSet.BOTTOM)
        constraintSet.applyTo(this)

        return barEditingOverlayLayout
    }

    fun changeVisibleGrid(voiceNum: Int){
        if (voiceNum !in 1..4){
            throw IllegalArgumentException("Only voices 1 to 4 can exist!")
        }

        voiceGridOverlays.values.forEach {
            it.visibility = INVISIBLE
        }
        voiceGridOverlays[voiceNum]?.visibility = VISIBLE
        activeVoiceOverlayNum = voiceNum
    }

    fun nextBar(){
        if (previousButtonDisabled) {
            prevBarButton.isClickable = true
            previousButtonDisabled = false
        }

        if (barIdx == bars.size - 1){
            val newBar = Bar.makeEmpty(barIdx + 2, bar.timeSignature)
            bars.add(newBar)
        }

        barIdx++
        updateOverlays(bars[barIdx])
        bar = bars[barIdx]
    }

    fun previousBar(){
        if (previousButtonDisabled){
            prevBarButton.isClickable = false
            return
        }

        if (barIdx <= 0){
            throw IllegalArgumentException("Previous bar button should be disabled!")
        }

        barIdx--
        if (barIdx == 0){
            prevBarButton.isClickable = false
            previousButtonDisabled = true
        }

        val previousBar = bars[barIdx]
        if (bar.isBarOfRests() && bar.timeSignature.equals(previousBar.timeSignature)){
            bars.removeAt(barIdx + 1)
        }
        updateOverlays(previousBar)
        bar = previousBar
    }

    private fun updateOverlays(nextBar: Bar){
        val voiceNumsToRemove = mutableListOf<Int>()
        for (voiceNum in voiceGridOverlays.keys){
            if (nextBar.voices[voiceNum] == null){
                voiceNumsToRemove.add(voiceNum)
            }
        }
        voiceNumsToRemove.forEach {
            removeView(voiceGridOverlays[it])
            voiceGridOverlays.remove(it)
        }

        for (voiceNum in nextBar.voices.keys){
            if (voiceGridOverlays[voiceNum] == null){
                voiceGridOverlays[voiceNum] = addBarEditingOverlayLayout()
            }
        }

        voiceGridOverlays.forEach{
            it.value.visibility =
                    if (it.key == activeVoiceOverlayNum) VISIBLE
                    else INVISIBLE
        }
    }
}