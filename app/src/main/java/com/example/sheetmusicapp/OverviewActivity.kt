package com.example.sheetmusicapp

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnLayout
import androidx.core.view.setPadding
import com.example.sheetmusicapp.scoreModel.Bar
import com.example.sheetmusicapp.scoreModel.Score
import com.example.sheetmusicapp.scoreModel.TimeSignature
import com.example.sheetmusicapp.ui.BarVisLayout
import com.example.sheetmusicapp.ui.TimeSignatureLayout
import kotlin.math.min

const val barsPerLine = 4
const val linesPerPage = 8

class OverviewActivity : AppCompatActivity() {
    lateinit var score : Score
    lateinit var overviewLayout : LinearLayout
    lateinit var titleView : TextView
    var currentTimeSignature : TimeSignature? = null
    var pageCount : Int = 0

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_overview)
        overviewLayout = findViewById(R.id.overviewLayout)
        overviewLayout.setPadding(64)

        score = intent.getSerializableExtra("score") as Score
        var barCount = score.barList.size
        pageCount = 1
        // first page also needs space for title
        if ((barCount / barsPerLine) > linesPerPage - 1){
            barCount -= (linesPerPage - 1) * barsPerLine
            pageCount += barCount / (barsPerLine * linesPerPage) + 1
        }
        barCount = score.barList.size
        if (barCount == 0){
            throw IllegalStateException("Score can't be completely empty!")
        }

        overviewLayout.doOnLayout {
            val pageWidth = it.width - 128
            // A4 ratio
            val pageHeight = (pageWidth * 1.414).toInt()
            val barLineHeight : Double = pageHeight / 8.0

            // add page views
            for (i in 1..pageCount){
                val pageBars =
                    if (i == 1) score.barList.subList(0, min((linesPerPage - 1) * barsPerLine, barCount))
                    else {
                        val fromIdx = (linesPerPage - 1) * barsPerLine + (i-2) * linesPerPage * barsPerLine
                        val toIdx = min((linesPerPage - 1) * barsPerLine + (i-1) * linesPerPage * barsPerLine, barCount)
                        score.barList.subList(fromIdx, toIdx)
                    }

                val pageLayout = ConstraintLayout(this)
                pageLayout.id = ViewGroup.generateViewId()
                pageLayout.layoutParams = ConstraintLayout.LayoutParams(pageWidth, pageHeight)
                pageLayout.setBackgroundColor(resources.getColor(R.color.white))
                overviewLayout.addView(pageLayout)

                if (i == 1){
                    // add title
                    titleView = TextView(this)
                    titleView.id = ViewGroup.generateViewId()
                    titleView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (barLineHeight / 3).toInt())
                    titleView.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM)
                    titleView.setText(score.title)
                    titleView.setTextColor(resources.getColor(R.color.black))
                    titleView.gravity = Gravity.CENTER
                    pageLayout.addView(titleView)

                    val constraintSet = ConstraintSet()
                    constraintSet.clone(pageLayout)
                    constraintSet.connect(titleView.id, ConstraintSet.LEFT, pageLayout.id, ConstraintSet.LEFT)
                    constraintSet.connect(titleView.id, ConstraintSet.RIGHT, pageLayout.id, ConstraintSet.RIGHT)
                    constraintSet.connect(titleView.id, ConstraintSet.TOP, pageLayout.id, ConstraintSet.TOP)
                    constraintSet.connect(titleView.id, ConstraintSet.BOTTOM, pageLayout.id, ConstraintSet.BOTTOM, (barLineHeight * 7).toInt())
                    constraintSet.applyTo(pageLayout)

                    for (j in 0 until (linesPerPage - 1)){
                        val lineBars = pageBars.subList(barsPerLine * j, min(barsPerLine * (j + 1), pageBars.size))
                        addBarLine(pageLayout, lineBars, barLineHeight, pageWidth, j + 1)
                        if (lineBars.size < barsPerLine) break
                    }
                }
                else {
                    for (j in 0 until linesPerPage){
                        val lineBars = pageBars.subList(barsPerLine * j, min(barsPerLine * (j + 1), pageBars.size))
                        addBarLine(pageLayout, lineBars, barLineHeight, pageWidth, j)
                        if (lineBars.size < barsPerLine) break
                    }
                }


                // add space view between pages
                if (i < pageCount){
                    val spaceView = Space(this)
                    spaceView.id = ViewGroup.generateViewId()
                    spaceView.layoutParams = ViewGroup.LayoutParams(pageWidth, 64)
                    overviewLayout.addView(spaceView)
                }
            }
        }
    }

    fun addBarLine(pageLayout: ConstraintLayout, bars: MutableList<Bar>, barLineHeight: Double, barLineWidth: Int, lineIdx: Int){
        if (lineIdx < 0 || lineIdx > linesPerPage - 1){
            throw IllegalArgumentException("LineIdx exceeds linesPerPage!")
        }

        if (bars.size > barsPerLine){
            throw IllegalArgumentException("Number of bars exceeds bars per line")
        }

        val barLineLeftRightMargin = 64
        val barWidth = ((barLineWidth - 2 * barLineLeftRightMargin) / barsPerLine.toDouble()).toInt()
        var leftMargin = barLineLeftRightMargin + ((barsPerLine - 1) / 2)
        val constraintSet = ConstraintSet()
        for (i in bars.indices){
            val bar = bars[i]
            val barHeight = (barLineHeight * 0.25).toInt()
            val barVisLayout = BarVisLayout(this, barHeight, bar)
            barVisLayout.id = ViewGroup.generateViewId()
            barVisLayout.layoutParams = ViewGroup.LayoutParams(barWidth, barLineHeight.toInt())
            barVisLayout.setOnClickListener {
                val barVisLayout = it as BarVisLayout
                val data = Intent().apply { putExtra("barNr", barVisLayout.bar.barNr) }
                setResult(RESULT_OK, data)
                finish()
            }
            pageLayout.addView(barVisLayout)

            constraintSet.clone(pageLayout)
            constraintSet.connect(barVisLayout.id, ConstraintSet.LEFT, pageLayout.id, ConstraintSet.LEFT, leftMargin)
            constraintSet.connect(barVisLayout.id, ConstraintSet.TOP, pageLayout.id, ConstraintSet.TOP, (lineIdx * barLineHeight).toInt())
            constraintSet.applyTo(pageLayout)

            if (currentTimeSignature != bar.timeSignature){
                val timeSignatureLayout = TimeSignatureLayout(this, bar.timeSignature)
                timeSignatureLayout.id = ViewGroup.generateViewId()
                timeSignatureLayout.layoutParams = ViewGroup.LayoutParams(barHeight / 3, barHeight)
                timeSignatureLayout.makeTextBold = true
                pageLayout.addView(timeSignatureLayout)

                constraintSet.clone(pageLayout)
                constraintSet.connect(timeSignatureLayout.id, ConstraintSet.LEFT, barVisLayout.id, ConstraintSet.LEFT, 5)
                constraintSet.connect(timeSignatureLayout.id, ConstraintSet.TOP, barVisLayout.id, ConstraintSet.TOP)
                constraintSet.connect(timeSignatureLayout.id, ConstraintSet.BOTTOM, barVisLayout.id, ConstraintSet.BOTTOM, barHeight / 8)
                constraintSet.applyTo(pageLayout)

                currentTimeSignature = bar.timeSignature
            }

            leftMargin += barWidth - 1
        }

    }
}