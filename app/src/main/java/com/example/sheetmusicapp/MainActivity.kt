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
import java.lang.IllegalStateException

const val noteWidthToHeightRatio = 0.6031
const val noteHeadHeightToTotalHeightRatio = 0.2741
const val noteHeadWidthToNoteHeightRatio = 0.3474
const val noteStemWidthToNoteHeightRatio = 0.0362
const val noteStemStartFromUpNoteBottomToNoteHeightRatio = 0.1816
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

        // val exampleScore = Score.makeEmpty(bars = 32, timeSignature =  TimeSignature(4, 4))
        val exampleBar = Bar.makeEmpty(1, TimeSignature(5, 8))
        exampleBar.addNote(1, RhythmicLength(BasicRhythmicLength.SIXTEENTH), NoteHeadType.ELLIPTIC, 12, 0)
        exampleBar.addNote(1, RhythmicLength(BasicRhythmicLength.QUARTER, LengthModifier.DOTTED), NoteHeadType.ELLIPTIC, 11, 0)
        exampleBar.addNote(1, RhythmicLength(BasicRhythmicLength.QUARTER, LengthModifier.DOTTED), NoteHeadType.ELLIPTIC, 6, 0)

//        val json = parser.setPrettyPrinting().create().toJson(exampleScore)
//        saveToFile("test.txt", json)
//        println(json)
//        val test = parser.create().fromJson(json, Score::class.java)

        setContentView(R.layout.activity_main)

        val mainConstraintLayout = findViewById<ConstraintLayout>(R.id.realMain)
        mainConstraintLayout.doOnLayout {
            scaleBarLineStrokeWidth()
            visualizeBar(exampleBar)
        }
    }

    /**
     * Sets the widths of all strokes of the bar drawable to be equal again, after they changed due to scaling.
     */
    fun scaleBarLineStrokeWidth() {
        val barImageView = findViewById<ImageView>(R.id.barImageView)
        val barDrawableChildFinder = VectorChildFinder(this, R.drawable.ic_bar, barImageView)
        val leftVertical = barDrawableChildFinder.findPathByName("leftVertical")
        val rightVertical = barDrawableChildFinder.findPathByName("rightVertical")
        val exampleHorizontal = barDrawableChildFinder.findPathByName("exampleHorizontal")
        leftVertical.strokeWidth = exampleHorizontal.strokeWidth
        rightVertical.strokeWidth = exampleHorizontal.strokeWidth
    }

    // Visualizes all voices of a bar.
    fun visualizeBar(bar: Bar){
        // Calculate different size properties from bar height & width.
        val barImageView = findViewById<ImageView>(R.id.barImageView)
        val barWidth = barImageView.width
        val barHeight = barImageView.height
        val localNoteHeadHeight = barImageView.height / 4.toDouble()
        val noteImageHeight = (localNoteHeadHeight * (1 / noteHeadHeightToTotalHeightRatio)).toInt()
        val noteHeadWidth = (noteImageHeight * noteHeadWidthToNoteHeightRatio).toInt()
        val stemWidth = (noteImageHeight * noteStemWidthToNoteHeightRatio).toInt()


        for (voice in bar.voices.values){
            // voice only has common stem direction if there are multiple voices
            val voiceDirection: StemDirection? = voice.stemDirection
            val subGroups: MutableList<SubGroup> = voice.getCopyOfSubGroups()
            val intervalSubGroupIdxs: Map<RhythmicInterval, Int> = voice.getIntervalSubGroupIdxsCopy()

            // horizontal margin for iterative positioning starts after left bar padding
            var horizontalMargin = (barWidth * (BAR_LEFTRIGHT_PADDING_PERCENT / 2)).toInt()

            for (interval in voice.intervals){

                // Find subgroup of interval.
                val intervalSubGroupIdx : Int = intervalSubGroupIdxs[interval] ?: throw IllegalStateException("No subgroup was mapped for an interval in a voice!")
                if (intervalSubGroupIdx >= subGroups.size){
                    throw IllegalStateException("The mapped index of a subgroup exceeds the voice's subgroup list!")
                }
                val subGroup : SubGroup = subGroups[intervalSubGroupIdx]
                // If there's a voice direction, use this for the subgroup, otherwise let subgroup decide.
                val subGroupDirection = voiceDirection ?: subGroup.getStemDirection()

                // Sort musical note heights of interval, so first will be the one displayed as note with proper stem.
                val musicalNoteHeights = interval.getNoteHeadsCopy().keys
                val sortedMusicalNoteHeights = if (subGroupDirection == StemDirection.UP) (musicalNoteHeights.sortedDescending()).toMutableList() else musicalNoteHeights.sorted().toMutableList()


                // if null, no notes are contained (interval is rest)
                val extremumMusicalHeight = sortedMusicalNoteHeights.firstOrNull()
                if (extremumMusicalHeight != null) {
                    if (subGroupDirection == null){
                        throw IllegalStateException("Subgroup provides no common stem direction even though it has notes!")
                    }

                    // create top (for UP) or bottom (for down) note with fully visualized stem
                    addNoteView(createNoteView(noteImageHeight, subGroupDirection), extremumMusicalHeight, horizontalMargin, subGroupDirection, noteHeadWidth, barHeight, barWidth)
                    sortedMusicalNoteHeights.removeAt(0)

                    // Visualize remaining notes as note heads.
                    var lastNoteHeadView : ImageView? = null
                    var lastMusicalHeight : Int = extremumMusicalHeight
                    var isMirrored = false
                    for (musicalNoteHeadHeight in sortedMusicalNoteHeights) {
                        // If two notes are on successive heights, they would overlap. Therefore, one of them needs to be mirrored with the common
                        // stem as axis.
                        isMirrored = kotlin.math.abs(lastMusicalHeight - musicalNoteHeadHeight) == 1
                        lastNoteHeadView = createNoteHeadView(noteHeadWidth, localNoteHeadHeight.toInt(), isMirrored)
                        // Adapt current horizontal margin to mirrored note heads.
                        var adaptedHorizontalMargin = horizontalMargin
                        if (isMirrored){
                            if (subGroupDirection == StemDirection.UP){
                                adaptedHorizontalMargin += noteHeadWidth - stemWidth
                            }
                            else {
                                adaptedHorizontalMargin -= noteHeadWidth - stemWidth
                            }
                        }
                        addNoteView(lastNoteHeadView, musicalNoteHeadHeight, adaptedHorizontalMargin, subGroupDirection, noteHeadWidth, barHeight, barWidth)
                        lastMusicalHeight = musicalNoteHeadHeight
                    }

                    // Add common stem for all note intervals.
                    if (sortedMusicalNoteHeights.size > 0) {
                        if (lastNoteHeadView != null){
                            if (subGroupDirection == StemDirection.UP){
                                addMultiNoteStem(sortedMusicalNoteHeights.last(), extremumMusicalHeight, subGroupDirection, lastNoteHeadView, isMirrored, noteImageHeight, barHeight)
                            }
                            else {
                                addMultiNoteStem(extremumMusicalHeight, sortedMusicalNoteHeights.last(), subGroupDirection, lastNoteHeadView, isMirrored, noteImageHeight, barHeight)
                            }
                        }
                        else {
                            // won't happen
                        }
                    }

                } else {
                    // Visualize as rest (currently as note on musical height 6)
                    addNoteView(createNoteView(noteImageHeight, StemDirection.UP), 6, horizontalMargin, StemDirection.UP, noteHeadWidth, barHeight, barWidth)
                }

                // Increase horizontal margin according to rhythmic interval length
                horizontalMargin += (barWidth * calculateWidthPercent(interval, subGroup, voice.timeSignature)).toInt()
            }
        }

    }

    // Creates and returns a note ImageView facing upwards or downwards.
    private fun createNoteView(noteImageHeight: Int, stemDirection: StemDirection) : ImageView {
        val noteView = ImageView(this)
        noteView.id = View.generateViewId()
        // Set height & width of note image.
        val noteLayout = ViewGroup.LayoutParams((noteImageHeight * noteWidthToHeightRatio).toInt(), noteImageHeight)
        noteView.layoutParams = noteLayout
        if (stemDirection == StemDirection.UP){
            noteView.setImageResource(R.drawable.ic_eighth)
        }
        else {
            noteView.setImageResource(R.drawable.ic_eighth_flipped)
        }

        return  noteView
    }

    // Creates and returns potentially vertically mirrored note head.
    private fun createNoteHeadView(noteHeadWidth: Int, noteHeadHeight: Int, isMirrored: Boolean): ImageView{
        val noteHeadView = ImageView(this)
        noteHeadView.id = View.generateViewId()
        // Set height and width.
        noteHeadView.layoutParams = ViewGroup.LayoutParams(noteHeadWidth, noteHeadHeight)
        noteHeadView.setImageResource(R.drawable.ic_full_notehead)
        if (isMirrored){
            noteHeadView.scaleX = -1f
        }

        return noteHeadView
    }

    // Adds a given note (head) view to the UI bar.
    private fun addNoteView(noteView: ImageView, musicalHeight: Int, horizontalMargin: Int, stemDirection: StemDirection, noteHeadWidth: Int, barHeight: Int, barWidth: Int) {
        if (musicalHeight < 0 || musicalHeight > 12) {
            throw IllegalArgumentException("Height can't be less than 0 or larger than 12!")
        }

        // In UI component: Calculate following vals as global class property.
        val barStrokeWidth = (barHeight * barStrokeWidthToBarHeightRatio).toInt()
        val verticalMarginStep = barHeight / 8
        val lowestVerticalMargin = -(3 * verticalMarginStep)

        val constraintLayout = findViewById<ConstraintLayout>(R.id.notesConstraintLayout)
        constraintLayout.addView(noteView)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        // Handle upwards notes / note heads.
        if (stemDirection == StemDirection.UP){
            // horizontal constraints
            constraintSet.connect(noteView.id, ConstraintSet.LEFT, R.id.notesConstraintLayout, ConstraintSet.LEFT, horizontalMargin)

            // vertical constraints
            // handle notes / note heads on or below bar bottom
            if (musicalHeight < 3){

                // add horizontal stroke in middle of lowest note / note head
                if (musicalHeight == 0){
                    constraintSet.applyTo(constraintLayout)
                    addHorizontalStroke(0, horizontalMargin, stemDirection, noteHeadWidth, barHeight, barWidth, barStrokeWidth)
                    constraintSet.clone(constraintLayout)
                }

                // using auxiliary space view
                val stepsBelowBarBottom = 3 - musicalHeight
                val bottomDifferenceToBarBottom = stepsBelowBarBottom * verticalMarginStep
                val spaceView = Space(this)
                spaceView.id = View.generateViewId()
                spaceView.minimumHeight = bottomDifferenceToBarBottom
                constraintLayout.addView(spaceView)
                constraintSet.applyTo(constraintLayout)
                constraintSet.clone(constraintLayout)

                constraintSet.connect(spaceView.id, ConstraintSet.TOP, R.id.notesBottomGuideline, ConstraintSet.BOTTOM)
                constraintSet.connect(spaceView.id, ConstraintSet.LEFT, R.id.notesConstraintLayout, ConstraintSet.LEFT, horizontalMargin)
                constraintSet.connect(noteView.id, ConstraintSet.BOTTOM, spaceView.id, ConstraintSet.BOTTOM)

            }
            // vertical constraints
            // handle notes above bar bottom
            else {
                // add horizontal stroke in middle of highest note / note head
                if (musicalHeight == 12){
                    constraintSet.applyTo(constraintLayout)
                    addHorizontalStroke(12, horizontalMargin, stemDirection, noteHeadWidth, barHeight, barWidth, barStrokeWidth)
                    constraintSet.clone(constraintLayout)
                }

                val marginBottomToBarBottom = lowestVerticalMargin + verticalMarginStep * musicalHeight
                constraintSet.connect(noteView.id, ConstraintSet.BOTTOM, R.id.notesBottomGuideline, ConstraintSet.TOP, marginBottomToBarBottom)
            }
        }
        // Handle downwards notes / note heads.
        else {
            // horizontal constraints
            constraintSet.connect(noteView.id, ConstraintSet.RIGHT, R.id.notesConstraintLayout, ConstraintSet.RIGHT, barWidth - horizontalMargin - noteHeadWidth)

            // vertical constraints
            // handle notes on or above bar top
            if (musicalHeight > 9){

                // add horizontal stroke in middle of highest note / note head
                if (musicalHeight == 12){
                    constraintSet.applyTo(constraintLayout)
                    addHorizontalStroke(12, horizontalMargin, stemDirection, noteHeadWidth, barHeight, barWidth, barStrokeWidth)
                    constraintSet.clone(constraintLayout)
                }

                // using auxiliary space view
                val stepsAboveBarTop = 3 - (12 - musicalHeight)
                val topDifferenceToBarTop = stepsAboveBarTop * verticalMarginStep
                val spaceView = Space(this)
                spaceView.id = View.generateViewId()
                spaceView.minimumHeight = topDifferenceToBarTop
                constraintLayout.addView(spaceView)
                constraintSet.applyTo(constraintLayout)
                constraintSet.clone(constraintLayout)

                constraintSet.connect(spaceView.id, ConstraintSet.BOTTOM, R.id.notesTopGuideline, ConstraintSet.TOP)
                constraintSet.connect(spaceView.id, ConstraintSet.RIGHT, R.id.notesConstraintLayout, ConstraintSet.RIGHT, barWidth - horizontalMargin - noteHeadWidth)
                constraintSet.connect(noteView.id, ConstraintSet.TOP, spaceView.id, ConstraintSet.TOP)
            }

            // vertical constraints
            // handle notes below bar top
            else {
                // add horizontal stroke in middle of lowest note / note head
                if (musicalHeight == 0){
                    constraintSet.applyTo(constraintLayout)
                    addHorizontalStroke(0, horizontalMargin, stemDirection, noteHeadWidth, barHeight, barWidth, barStrokeWidth)
                    constraintSet.clone(constraintLayout)
                }

                val marginTopToBarTop = lowestVerticalMargin + verticalMarginStep * (12 - musicalHeight)
                constraintSet.connect(noteView.id, ConstraintSet.TOP, R.id.notesTopGuideline, ConstraintSet.BOTTOM, marginTopToBarTop)
            }
        }
        constraintSet.applyTo(constraintLayout)
    }

    // Adds a common stem for notes of an interval reaching from specified minimal and maximal musical height constrained to position of note head placed last.
    private fun addMultiNoteStem(minMusicalHeight: Int, maxMusicalHeight: Int, stemDirection: StemDirection, startNoteHeadView: ImageView, isMirrored: Boolean, noteImageHeight: Int, barHeight: Int){
        if (minMusicalHeight < 0 || minMusicalHeight > 12){
            throw IllegalArgumentException("minMusicalHeight can't be less than 0 or larger than 12!")
        }
        if (maxMusicalHeight < 0 || maxMusicalHeight > 12){
            throw IllegalArgumentException("maxMusicalHeight can't be less than 0 or larger than 12!")
        }
        if (maxMusicalHeight - minMusicalHeight < 1){
            throw IllegalArgumentException("max - min musical height must be at least 1!")
        }

        val stemWidth = (noteImageHeight * noteStemWidthToNoteHeightRatio).toInt()
        val stemStartHeight = (noteImageHeight * noteStemStartFromUpNoteBottomToNoteHeightRatio).toInt()
        val verticalMarginStep = barHeight / 8

        val multiNoteStemView = ImageView(this)
        multiNoteStemView.id = View.generateViewId()
        multiNoteStemView.scaleType = ImageView.ScaleType.FIT_XY
        val newMultiStemHeight = (maxMusicalHeight - minMusicalHeight) * verticalMarginStep
        multiNoteStemView.layoutParams = ViewGroup.LayoutParams(stemWidth, newMultiStemHeight)
        multiNoteStemView.setImageResource(R.drawable.black_rectangle)

        val constraintLayout = findViewById<ConstraintLayout>(R.id.notesConstraintLayout)
        constraintLayout.addView(multiNoteStemView)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        // Constrain to bottom of note head.
        if (stemDirection == StemDirection.UP){
            constraintSet.connect(multiNoteStemView.id, ConstraintSet.BOTTOM, startNoteHeadView.id, ConstraintSet.BOTTOM, stemStartHeight)
            // Constrain to right of note head.
            if (!isMirrored){
                constraintSet.connect(multiNoteStemView.id, ConstraintSet.RIGHT, startNoteHeadView.id, ConstraintSet.RIGHT)
            }
            // Constrain to left of note head.
            else {
                constraintSet.connect(multiNoteStemView.id, ConstraintSet.LEFT, startNoteHeadView.id, ConstraintSet.LEFT)
            }

        }
        // Constrain to top of note head.
        else {
            constraintSet.connect(multiNoteStemView.id, ConstraintSet.TOP, startNoteHeadView.id, ConstraintSet.TOP, stemStartHeight)
            // Constrain to left of note head.
            if (!isMirrored){
                constraintSet.connect(multiNoteStemView.id, ConstraintSet.LEFT, startNoteHeadView.id, ConstraintSet.LEFT)
            }
            // Constrain to right of note head.
            else {
                constraintSet.connect(multiNoteStemView.id, ConstraintSet.RIGHT, startNoteHeadView.id, ConstraintSet.RIGHT)
            }
        }
        constraintSet.applyTo(constraintLayout)
    }

    // Adds horizontal stroke for notes on height 0 or 12.
    private fun addHorizontalStroke(musicalHeight: Int, horizontalMargin: Int, stemDirection: StemDirection, noteHeadWidth: Int, barHeight: Int, barWidth: Int, barStrokeWidth: Int){
        if (musicalHeight !in listOf(0, 12)){
            throw IllegalArgumentException("Specified height must be either highest (12) or lowest (0)!")
        }

        val verticalMarginStep = barHeight / 8

        val optionalStrokeView = ImageView(this)
        optionalStrokeView.id = View.generateViewId()
        optionalStrokeView.scaleType = ImageView.ScaleType.FIT_XY
        optionalStrokeView.layoutParams = ViewGroup.LayoutParams((noteHeadWidth * 1.33).toInt(), barStrokeWidth)
        optionalStrokeView.setImageResource(R.drawable.black_rectangle)

        val constraintLayout = findViewById<ConstraintLayout>(R.id.notesConstraintLayout)
        constraintLayout.addView(optionalStrokeView)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)

        // Constrain to left or right of bar (similar to notes).
        if (stemDirection == StemDirection.UP){
            constraintSet.connect(optionalStrokeView.id, ConstraintSet.LEFT, R.id.notesConstraintLayout, ConstraintSet.LEFT, horizontalMargin - (noteHeadWidth * 0.165).toInt())
        }
        else {
            constraintSet.connect(optionalStrokeView.id, ConstraintSet.RIGHT, R.id.notesConstraintLayout, ConstraintSet.RIGHT, barWidth - horizontalMargin - (noteHeadWidth * 1.165).toInt())
        }

        // Constrain to top or bottom of bar.
        if (musicalHeight == 12){
            constraintSet.connect(optionalStrokeView.id, ConstraintSet.BOTTOM, R.id.notesTopGuideline, ConstraintSet.TOP, 2 * verticalMarginStep - barStrokeWidth / 2)
        }
        else {
            constraintSet.connect(optionalStrokeView.id, ConstraintSet.TOP, R.id.notesBottomGuideline, ConstraintSet.BOTTOM, 2 * verticalMarginStep - barStrokeWidth / 2)
        }

        constraintSet.applyTo(constraintLayout)
    }

    // Calculates percentage of width an interval with following or included paddings takes up.
    // Only widths of last intervals of sub groups get added padding.
    private fun calculateWidthPercent(interval: RhythmicInterval, subGroup: SubGroup, timeSignature: TimeSignature): Double {
        val barPercentWithoutPadding = BAR_NOTES_PERCENT - (timeSignature.numberOfSubGroups - 1) * WIDTH_OF_ONE_SUBGROUP_PADDING_PERCENT
        val intervalPercentWithoutPadding = barPercentWithoutPadding * interval.getLengthCopy().lengthInUnits / timeSignature.units
        var subGroupPaddingPercent = 0.0
        if (subGroup.isLast(interval)) {
            subGroupPaddingPercent = WIDTH_OF_ONE_SUBGROUP_PADDING_PERCENT * subGroup.paddingFactor
        }
        return intervalPercentWithoutPadding + subGroupPaddingPercent
    }
}