package com.example.sheetmusicapp

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Space
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnLayout
import com.devs.vectorchildfinder.VectorChildFinder
import com.example.sheetmusicapp.parser.ScoreDeserializer
import com.example.sheetmusicapp.parser.ScoreSerializer
import com.example.sheetmusicapp.scoreModel.*
import java.lang.IllegalArgumentException
import com.google.gson.GsonBuilder
import java.io.File

const val noteWidthToHeightRatio = 0.6031
const val noteHeadHeightToTotalHeightRatio = 0.2741
const val noteHeadWidthToNoteHeightRatio = 0.3474
const val barStrokeWidthToBarHeightRatio = 0.0125


class MainActivity : AppCompatActivity() {
    var parser = GsonBuilder()

    private fun initParser() {
        parser.registerTypeAdapter(Score::class.java, ScoreSerializer())
        parser.registerTypeAdapter(Score::class.java, ScoreDeserializer())
    }

    fun saveToFile(name: String, json: String) {
        try {
            val file = File(applicationContext.getExternalFilesDir(null), name)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initParser()
        super.onCreate(savedInstanceState)

        val exampleScore = Score.makeEmpty(bars = 32, timeSignature =  TimeSignature(4, 4))
        val exampleBar = Bar(1, TimeSignature(5, 8), mapOf())
        exampleBar.addEmptyVoice(1)
        exampleBar.addNote(1, RhythmicLength(BasicRhythmicLength.SIXTEENTH), NoteHeadType.ELLIPTIC, 0, 0)
        // exampleBar.addNote(1, RhythmicLength(BasicRhythmicLength.QUARTER, LengthModifier.DOTTED), NoteHeadType.ELLIPTIC, 12, 0)

//        val json = parser.setPrettyPrinting().create().toJson(exampleScore)
//        saveToFile("test.txt", json)
//        println(json)
//        val test = parser.create().fromJson(json, Score::class.java)

        setContentView(R.layout.activity_main)

        val mainConstraintLayout = findViewById<ConstraintLayout>(R.id.realMain)
        mainConstraintLayout.doOnLayout {
            scaleBarLineStrokeWidth()
            visualizeBarVoiceOne(exampleScore.barList[0])
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
        val barWidth = barImageView.width
        val barHeight = barImageView.height
        val barStrokeWidth = (barHeight * barStrokeWidthToBarHeightRatio).toInt()

        val localNoteHeadHeight = barImageView.height / 4.toDouble()
        // Scaling according to note head height derived from measurement of drawable proportions.
        val noteHeight = (localNoteHeadHeight * (1 / noteHeadHeightToTotalHeightRatio)).toInt()
        val noteHeadWidth = (noteHeight * noteHeadWidthToNoteHeightRatio).toInt()


        val voice1 : MutableList<RhythmicInterval>? = bar.voices[1]

        if (voice1 != null){
            addVoiceNotesToBar(voice1, barWidth, barHeight, noteHeight, noteHeadWidth, barStrokeWidth)
        }
    }

    // bar height & width, noteImageHeight don't need to be actual parameters but a constant property that's assigned once when the app starts
    fun addVoiceNotesToBar(voiceIntervals: List<RhythmicInterval>, barWidth: Int, barHeight: Int, noteImageHeight: Int, noteHeadWidth: Int, barStrokeWidth: Int){

        val constraintLayout = findViewById<ConstraintLayout>(R.id.notesConstraintLayout)

        // While upwards notes get constrained to the bottom of the bar, and the lowest margin is for notes below its lowest line,
        // flipped notes get constrained to the top, so that the lowest margin creates notes above its highest line for those.
        val verticalMarginStep = barHeight / 8
        val lowestVerticalMargin = -(3 * verticalMarginStep)
        var horizontalMargin = (barWidth * (BAR_LEFTRIGHT_PADDING_PERCENT * 0.01 / 2)).toInt()

        for (interval in voiceIntervals){

            // Create new note image view and an id for it.
            val noteView = ImageView(this)
            noteView.id = View.generateViewId()
            // Set height & width of note image.
            val noteLayout = ViewGroup.LayoutParams((noteImageHeight * noteWidthToHeightRatio).toInt(), noteImageHeight)
            noteView.layoutParams = noteLayout

            // Add note view to layout and prepare setting of constraints in set to be applied at end of loop.
            constraintLayout.addView(noteView)
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)

            // Set height constraints in constraint set.
            //
            // later, multiple notes need to be visualized and connect their stems
            // rests, which have no notes heads, are currently put at center musical height.
            val noteMusicalHeight : Int = interval.noteHeads.keys.lastOrNull() ?: 6

            if (noteMusicalHeight < 0 || noteMusicalHeight > 12){
                throw IllegalArgumentException("Height can't be less than 0 or larger than 12!")
            }

            // add note with upwards stem, note head constrained to bar bottom
            if (noteMusicalHeight < 5){
                noteView.setImageResource(R.drawable.ic_eighth)
                constraintSet.connect(noteView.id, ConstraintSet.LEFT, R.id.notesConstraintLayout, ConstraintSet.LEFT, horizontalMargin)

                // note bottom needs to be aligned to something between bar bottom
                if (noteMusicalHeight < 3){

                    val stepsBelowBarBottom = 3 - noteMusicalHeight
                    val bottomDifferenceToBarBottom = stepsBelowBarBottom * verticalMarginStep
                    val spaceView = Space(this)
                    spaceView.id = View.generateViewId()
                    spaceView.minimumHeight = bottomDifferenceToBarBottom
                    constraintLayout.addView(spaceView)

                    constraintSet.connect(spaceView.id, ConstraintSet.TOP, R.id.notesBottomGuideline, ConstraintSet.BOTTOM)
                    constraintSet.connect(spaceView.id, ConstraintSet.LEFT, R.id.notesConstraintLayout, ConstraintSet.LEFT, horizontalMargin)
                    constraintSet.connect(noteView.id, ConstraintSet.BOTTOM, spaceView.id, ConstraintSet.BOTTOM)

                    // Deal with lowest notes, which need to lie on a horizontal stroke.
                    if (noteMusicalHeight == 0) {
                        val optionalBottomStrokeView = ImageView(this)
                        optionalBottomStrokeView.id = View.generateViewId()
                        optionalBottomStrokeView.scaleType = ImageView.ScaleType.FIT_XY
                        optionalBottomStrokeView.layoutParams = ViewGroup.LayoutParams((noteHeadWidth * 1.33).toInt(), barStrokeWidth)
                        optionalBottomStrokeView.setImageResource(R.drawable.black_rectangle)

                        constraintLayout.addView(optionalBottomStrokeView)
                        constraintSet.applyTo(constraintLayout)
                        constraintSet.clone(constraintLayout)

                        constraintSet.connect(optionalBottomStrokeView.id, ConstraintSet.LEFT, R.id.notesConstraintLayout, ConstraintSet.LEFT, horizontalMargin - (noteHeadWidth * 0.165).toInt())
                        constraintSet.connect(optionalBottomStrokeView.id, ConstraintSet.TOP, R.id.notesBottomGuideline, ConstraintSet.BOTTOM, 2 * verticalMarginStep - barStrokeWidth / 2)
                    }
                }
                else {
                    val marginBottomToBarBottom = lowestVerticalMargin + verticalMarginStep * noteMusicalHeight
                    constraintSet.connect(noteView.id, ConstraintSet.BOTTOM, R.id.notesBottomGuideline, ConstraintSet.TOP, marginBottomToBarBottom)
                }

            }
            // add note with downwards stem, note head constrained to bar top
            else {
                noteView.setImageResource(R.drawable.ic_eighth_flipped)
                // To constrain the left of the note head to the left border of each subdivision, note head right needs to be constrained to
                // this position + noteHeadWidth, to deal with note stem "appendages".
                constraintSet.connect(noteView.id, ConstraintSet.RIGHT, R.id.notesConstraintLayout, ConstraintSet.RIGHT, barWidth - horizontalMargin - noteHeadWidth)

                if (noteMusicalHeight > 9){

                    val stepsAboveBarTop = 3 - (12 - noteMusicalHeight)
                    val topDifferenceToBarTop = stepsAboveBarTop * verticalMarginStep
                    val spaceView = Space(this)
                    spaceView.id = View.generateViewId()
                    spaceView.minimumHeight = topDifferenceToBarTop
                    constraintLayout.addView(spaceView)


                    constraintSet.connect(spaceView.id, ConstraintSet.BOTTOM, R.id.notesTopGuideline, ConstraintSet.TOP)
                    constraintSet.connect(spaceView.id, ConstraintSet.RIGHT, R.id.notesConstraintLayout, ConstraintSet.RIGHT, barWidth - horizontalMargin - noteHeadWidth)
                    constraintSet.connect(noteView.id, ConstraintSet.TOP, spaceView.id, ConstraintSet.TOP)

                    // Deal with lowest notes, which need to lie on a horizontal stroke.
                    if (noteMusicalHeight == 12){
                        val optionalTopStrokeView = ImageView(this)
                        optionalTopStrokeView.id = View.generateViewId()
                        optionalTopStrokeView.scaleType = ImageView.ScaleType.FIT_XY
                        optionalTopStrokeView.layoutParams = ViewGroup.LayoutParams((noteHeadWidth * 1.33).toInt(), barStrokeWidth)
                        optionalTopStrokeView.setImageResource(R.drawable.black_rectangle)

                        constraintLayout.addView(optionalTopStrokeView)
                        constraintSet.applyTo(constraintLayout)
                        constraintSet.clone(constraintLayout)


                        constraintSet.connect(optionalTopStrokeView.id, ConstraintSet.RIGHT, R.id.notesConstraintLayout, ConstraintSet.RIGHT, barWidth - horizontalMargin - (noteHeadWidth * 1.165).toInt())
                        constraintSet.connect(optionalTopStrokeView.id, ConstraintSet.BOTTOM, R.id.notesTopGuideline, ConstraintSet.TOP, 2 * verticalMarginStep - barStrokeWidth / 2)
                    }

                }
                else {
                    val marginTopToBarTop = lowestVerticalMargin + verticalMarginStep * (12 - noteMusicalHeight)
                    constraintSet.connect(noteView.id, ConstraintSet.TOP, R.id.notesTopGuideline, ConstraintSet.BOTTOM, marginTopToBarTop)
                }

            }

            constraintSet.applyTo(constraintLayout)
            // Increase margin to left bar border according to width of added interval.
            horizontalMargin += ((interval.widthPercent * 0.01) * barWidth).toInt()

        }
    }
}