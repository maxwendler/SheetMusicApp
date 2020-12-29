package com.example.sheetmusicapp

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnLayout
import com.devs.vectorchildfinder.VectorChildFinder
import com.example.sheetmusicapp.scoreModel.*

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // val exampleScore = Score.makeEmpty(bars = 32, timeSignature =  TimeSignature(4, 4))
        val exampleBar = Bar.makeEmpty(1, TimeSignature(5, 8))

        // Set callback for resetting stroke widths of bar drawable after scaling.
        val mainConstraintLayout = findViewById<ConstraintLayout>(R.id.main)
        mainConstraintLayout.doOnLayout {
            scaleBarLineStrokeWidth()
            visualizeBarVoiceOne(exampleBar)
        }
    }

    /**
     * Sets the widths of all strokes of the bar drawable to be equal again, after they changed due to scaling.
     */
    fun scaleBarLineStrokeWidth(){
        val barImageView = findViewById<ImageView>(R.id.barImageView)
        val barDrawableChildFinder = VectorChildFinder(this, R.drawable.ic_bar, barImageView)
        val leftVertical = barDrawableChildFinder.findPathByName("leftVertical")
        val rightVertical = barDrawableChildFinder.findPathByName("rightVertical")
        val exampleHorizontal = barDrawableChildFinder.findPathByName("exampleHorizontal")
        leftVertical.strokeWidth = exampleHorizontal.strokeWidth
        rightVertical.strokeWidth = exampleHorizontal.strokeWidth
    }

    // Prototype function visualizing all intervals of voice "1" of the given bar. Prototype of view placement according to interval width.
    // Currently, one interval == one note view (an interval can have multiple note on multiple heights,
    // no height calculation for note views, eighth note image for intervals of all rhythmic lengths.
    fun visualizeBarVoiceOne(bar: Bar){

        // Calculate height of all notes from bar height.
        val barImageView = findViewById<ImageView>(R.id.barImageView)
        val localNoteHeadHeight = barImageView.height / 5.toDouble()
        // Scaling according to note head height derived from measurement of drawable proportions.
        val noteHeight = (localNoteHeadHeight * (1 / 0.2741)).toInt()
        val barWidth = barImageView.width

        val voice1 : MutableList<RhythmicInterval>? = bar.voices[1]
        val notesConstraintLayout = findViewById<ConstraintLayout>(R.id.notesConstraintLayout)

        if (voice1 != null){

            // margin to left bar border, increases while iteration over the intervals
            // start margin according to padding from left bar border
            var margin = (barWidth * (BAR_LEFTRIGHT_PADDING_PERCENT * 0.01 / 2)).toInt()

            // Iterate over all intervals
            for (interval in voice1){

                // Create new note image view (currently all notes are visualized as eighths)
                val noteView = ImageView(this)
                noteView.id = View.generateViewId()
                noteView.setImageResource(R.drawable.ic_eighth)

                // Add note views to notes constraint layout (on top of bar view).
                val noteLayout = ViewGroup.LayoutParams((noteHeight * 0.6031).toInt(), noteHeight)
                noteView.layoutParams = noteLayout
                notesConstraintLayout.addView(noteView)
                val notesConstraintSet = ConstraintSet()
                notesConstraintSet.clone(notesConstraintLayout)
                notesConstraintSet.connect(noteView.id, ConstraintSet.LEFT, R.id.notesConstraintLayout, ConstraintSet.LEFT, margin)
                // Currently no correct height, only centering.
                notesConstraintSet.connect(noteView.id, ConstraintSet.TOP, R.id.notesConstraintLayout, ConstraintSet.TOP)
                notesConstraintSet.connect(noteView.id, ConstraintSet.BOTTOM, R.id.notesConstraintLayout, ConstraintSet.BOTTOM)
                notesConstraintSet.applyTo(notesConstraintLayout)

                // Increase margin to left bar border according to width of added interval.
                margin += ((interval.widthPercent / 100) * barWidth).toInt()
            }
        }
    }
}