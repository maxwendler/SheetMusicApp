package com.example.sheetmusicapp.ui

import android.content.Context
import android.graphics.Paint
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.core.widget.TextViewCompat
import com.example.sheetmusicapp.R
import com.example.sheetmusicapp.scoreModel.TimeSignature

class TimeSignatureLayout (context: Context, initTimeSignature: TimeSignature) : LinearLayout(context) {

    var numerator : Int = initTimeSignature.numerator
        set(value) {
            val currentNumeratorView = numeratorView
                    ?: throw IllegalStateException("Can't change numerator if no numerator view was created yet!")
            currentNumeratorView.text = value.toString()
            field = value
        }
    var denominator : Int = initTimeSignature.denominator
        set(value) {
            val currentDenominatorView = denominatorView
                    ?: throw IllegalStateException("Can't change denominator if no denominator view was created yet!")
            currentDenominatorView.text = value.toString()
            field = value
        }

    var numeratorView : TextView? = null
    var denominatorView : TextView? = null
    var makeTextBold = false

    init {
        orientation = VERTICAL

        doOnLayout {
            numeratorView = addNumberLayout(numerator)
            addHorizontalStroke()
            denominatorView = addNumberLayout(denominator)
            if (makeTextBold){
                numeratorView?.paintFlags = Paint.FAKE_BOLD_TEXT_FLAG
                denominatorView?.paintFlags = Paint.FAKE_BOLD_TEXT_FLAG
            }
        }
    }

    private fun addNumberLayout(number: Int) : TextView {
        val numberViewHeight : Int = (height * (1 - barStrokeWidthToBarHeightRatio * 2) / 2).toInt()
        val numberView = TextView(context)
        numberView.id = generateViewId()
        numberView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, numberViewHeight)
        numberView.text = number.toString()
        numberView.gravity = Gravity.CENTER
        numberView.setTextColor(resources.getColor(R.color.black))
        TextViewCompat.setAutoSizeTextTypeWithDefaults(numberView, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM)
        addView(numberView)
        return numberView
    }

    private fun addHorizontalStroke() {
        val strokeWidth : Int = (height * barStrokeWidthToBarHeightRatio * 5).toInt()
        val strokeView = ImageView(context)
        strokeView.id = generateViewId()
        strokeView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, strokeWidth)
        strokeView.scaleType = ImageView.ScaleType.FIT_XY
        strokeView.setImageResource(R.drawable.black_rectangle)
        addView(strokeView)
    }

    fun updateViews(timeSignature: TimeSignature){
        numerator = timeSignature.numerator
        denominator = timeSignature.denominator
    }
}