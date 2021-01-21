package com.example.sheetmusicapp.ui

import android.content.Context
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout
import com.example.sheetmusicapp.scoreModel.Bar
import java.lang.IllegalArgumentException

class EditableBarLayout (context: Context, private val barHeightPercentage: Double, val bar: Bar) : ConstraintLayout(context) {

    val voiceGridOverlays : MutableMap<Int, BarEditingOverlayLayout> = mutableMapOf()

    init {
        doOnLayout {
            val barVisLayout = addBarVisLayout(bar)
            for (i in 1..4){
                val newOverlay = addBarEditingOverlayLayout()
                voiceGridOverlays[i] = newOverlay
                newOverlay.visibility = INVISIBLE
            }
            voiceGridOverlays[1]?.visibility = VISIBLE
            barVisLayout.setEditingOverlayCallback { horizontalMargins, voiceNum ->
                if (voiceNum !in 1..4){
                    throw IllegalArgumentException("Only voices 1 to 4 can exist!")
                }
                voiceGridOverlays[voiceNum]?.horizontalMargins = horizontalMargins
            }
        }
    }

    private fun addBarVisLayout(bar: Bar) : BarVisLayout{

        val barVisLayout = BarVisLayout(context, barHeightPercentage, bar)
        barVisLayout.id = ViewGroup.generateViewId()
        barVisLayout.tag = "barVisLayout"
        barVisLayout.layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        this.addView(barVisLayout)

        return barVisLayout
    }

    fun addBarEditingOverlayLayout() : BarEditingOverlayLayout{

        val barEditingOverlayLayout = BarEditingOverlayLayout(context, height * barHeightPercentage)
        barEditingOverlayLayout.id = generateViewId()
        barEditingOverlayLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        this.addView(barEditingOverlayLayout)

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
    }
}