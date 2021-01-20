package com.example.sheetmusicapp.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnLayout
import com.example.sheetmusicapp.R
import com.example.sheetmusicapp.scoreModel.*
import java.lang.IllegalStateException

const val noteHeightToNoteHeadHeightRatio = 1 / 0.2741

const val noteHeadWidthToNoteHeightRatio = 0.3474
const val noteHeadWidthToHeightRatio = noteHeadWidthToNoteHeightRatio * noteHeightToNoteHeadHeightRatio

const val noteStemWidthToNoteHeightRatio = 0.0362
const val noteStemWidthToNoteHeadHeightRatio = noteHeightToNoteHeadHeightRatio * noteStemWidthToNoteHeightRatio
const val noteStemStartFromUpNoteBottomToNoteHeightRatio = 0.1816
const val noteStemStartFromUpNoteBottomToNoteHeadHeightRatio = noteStemStartFromUpNoteBottomToNoteHeightRatio * noteHeightToNoteHeadHeightRatio

const val dotDiameterToNoteHeadWidthRatio = 0.2803
const val dotDiameterToNoteHeadHeightRatio = dotDiameterToNoteHeadWidthRatio * noteHeadWidthToHeightRatio

const val dotNoteDistanceToNoteHeadWidthRatio = 0.2179
const val dotNoteDistanceToNoteHeadHeightRatio = dotNoteDistanceToNoteHeadWidthRatio * noteHeadWidthToHeightRatio

const val barStrokeWidthToBarHeightRatio = 0.005

// Constant specifying the combined percentage of width of the padding elements on the right and the left of a UI bar.
const val BAR_LEFTRIGHT_PADDING_PERCENT = 0.15
// Constant specifying the combined percentage of width of all notes, depending on the padding fractions.
const val BAR_NOTES_PERCENT = 1 - BAR_LEFTRIGHT_PADDING_PERCENT
const val WIDTH_OF_ONE_SUBGROUP_PADDING_PERCENT = 0.025

class BarVisLayout(context: Context, private val barHeightPercentage: Double, val bar: Bar) : ConstraintLayout(context) {

    // bar height from screen height via barHeightPercentage
    private var barHeight : Int = 0
    // bar height without stroke width outside of rectangle path (notes on horizontal stroke middle!)
    private var trueBarHeight : Int = 0
    // width of bar without padding on left and right bar side
    private var noteAreaWidth : Int = 0

    private var barStrokeWidth : Int = 0
    // vertical margin step when constraining to layout relative to musical note height
    private var verticalMusicHeightStep : Double = 0.0
    // margin of smallest musical note height
    private var smallestMusicHeightToBottomMargin : Double = 0.0

    // note head views
    private var noteHeadHeight : Int = 0
    private var noteHeadWidthForNonWholes : Int = 0
    // note stem views
    private var noteStemWidth : Int = 0
    private var noteStemStartHeight : Int = 0
    // note dot views
    private var dotDiameter : Int = 0
    private var dotToNoteDistance : Int = 0
    private var dotMarginToUpNoteBottom : Int = 0

    init {

        // dynamically add content after this was laid out
        doOnLayout {
            calculateElementParams()
            addBarView()
            visualizeBar()
        }
    }

    /**
     * Calculates widths, heights and auxiliary margins / margin modifiers from current layout.
     * @throws IllegalStateException When this was not laid out yet.
     */
    private fun calculateElementParams(){
        if (height == 0 || width == 0){
            throw IllegalStateException("Parameters of elements can't be fully calculated because width or height is 0!")
        }

        barHeight = (height * barHeightPercentage).toInt()

        // make sure that stroke width is dividable by 2, so positions of stroke centres can actually
        // be calculated in terms of pixels
        var roundedStrokeWidth = (height * barStrokeWidthToBarHeightRatio).toInt()
        if (roundedStrokeWidth == 0){
            roundedStrokeWidth = 2
        }
        else {
            if (roundedStrokeWidth % 2 == 1) {
                roundedStrokeWidth++
            }
        }
        barStrokeWidth = roundedStrokeWidth
        trueBarHeight = barHeight - barStrokeWidth
        noteAreaWidth = (width * BAR_NOTES_PERCENT).toInt()

        verticalMusicHeightStep = trueBarHeight / 8.0
        // 3 steps below bar bottom
        smallestMusicHeightToBottomMargin = (height - barHeight) / 2.0 + barStrokeWidth / 2.0 - 3 * verticalMusicHeightStep

        // one "between horizontal bar strokes height"
        val noteHeadHeightFloat = verticalMusicHeightStep * 2
        noteHeadHeight = noteHeadHeightFloat.toInt()

        // calculate params from measures ratios
        noteStemWidth = (noteHeadHeightFloat * noteStemWidthToNoteHeadHeightRatio).toInt()
        noteHeadWidthForNonWholes = (noteHeadHeightFloat * noteHeadWidthToHeightRatio).toInt()
        noteStemStartHeight = (noteHeadHeightFloat * noteStemStartFromUpNoteBottomToNoteHeadHeightRatio).toInt()
        dotDiameter = (noteHeadHeightFloat * dotDiameterToNoteHeadHeightRatio).toInt()
        dotToNoteDistance = (noteHeadHeightFloat * dotNoteDistanceToNoteHeadHeightRatio).toInt()
        dotMarginToUpNoteBottom = (noteHeadHeight / 2.0 - 0.25 * dotDiameter).toInt()
    }

    /**
     * Adds and constrains fundamental bar paths, as [BarDrawableView].
     *
     * @throws IllegalStateException When id of this layout has not been set, i.e. generated, because constraining is not possible then.
     */
    private fun addBarView(){

        if (this.id == 0){
            throw IllegalStateException("Can't constrain elements because this instance has no id!")
        }

        val barView = BarDrawableView(context, width, barHeight, barStrokeWidth)
        barView.id = View.generateViewId()
        barView.tag = "barView"
        barView.layoutParams = ViewGroup.LayoutParams(width, barHeight)
        addView(barView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(this)
        val verticalMargin = (height - barHeight) / 2
        constraintSet.connect(barView.id, ConstraintSet.TOP, this.id, ConstraintSet.TOP, verticalMargin)
        constraintSet.connect(barView.id, ConstraintSet.BOTTOM, this.id, ConstraintSet.BOTTOM, verticalMargin)

        constraintSet.applyTo(this)
    }

    /**
     * Adds children layouts and constrains them for displaying the notes of [bar]. Iterates
     * over [Voice]s and their [RhythmicInterval]s, while analyzing the properties of the intervals' [SubGroup]s.
     *
     * @throws IllegalStateException When no subgroup index is mapped for an interval in one of the voices, or
     * it exceeds the according subgroup list.
     * @throws IllegalStateException When a subgroup does has not calculated a common stem direction for its intervals,
     * even though it contains notes, not just rests.
     */
    private fun visualizeBar(){

        for (voice in bar.voices.values){
            // voice only has common stem direction if there are multiple voices
            val voiceDirection: StemDirection? = voice.stemDirection
            val subGroups: MutableList<SubGroup> = voice.getCopyOfSubGroups()
            val intervalSubGroupIdxs: Map<RhythmicInterval, Int> = voice.getIntervalSubGroupIdxsCopy()

            // horizontal margin for iterative positioning starts after left bar padding
            var horizontalMargin = (width * (BAR_LEFTRIGHT_PADDING_PERCENT / 2))

            for (interval in voice.intervals){

                val horizontalMarginInt = horizontalMargin.toInt()
                val intervalLength = interval.getLengthCopy()
                val isDotted =  intervalLength.lengthModifier == LengthModifier.DOTTED
                val isWhole = intervalLength.basicLength == BasicRhythmicLength.WHOLE

                // Find subgroup of interval.
                val intervalSubGroupIdx : Int = intervalSubGroupIdxs[interval] ?: throw IllegalStateException("No subgroup was mapped for an interval in a voice!")
                if (intervalSubGroupIdx >= subGroups.size){
                    throw IllegalStateException("The mapped index of a subgroup exceeds the voice's subgroup list!")
                }
                val subGroup : SubGroup = subGroups[intervalSubGroupIdx]
                // If there's a voice direction, use this for the subgroup, otherwise let subgroup decide.
                val subGroupDirection = voiceDirection ?: subGroup.getStemDirection()

                val intervalWidth : Double = noteAreaWidth * calculateWidthPercent(interval, subGroup, voice.timeSignature)

                // Sort musical note heights of interval, so first will be the one displayed as note with proper stem.
                val musicalNoteHeights = interval.getNoteHeadsCopy().keys
                val sortedMusicalNoteHeights = if (subGroupDirection == StemDirection.UP) (musicalNoteHeights.sortedDescending()).toMutableList() else musicalNoteHeights.sorted().toMutableList()

                // if null, no notes are contained => display as rest
                val extremumMusicalHeight = sortedMusicalNoteHeights.firstOrNull()
                // if not, display as note(s)
                if (extremumMusicalHeight != null) {

                    if (subGroupDirection == null){
                        throw IllegalStateException("Subgroup provides no common stem direction even though it has notes!")
                    }

                    // create top (for UP) or bottom (for down) note with fully visualized stem
                    val noteView = createNoteView(subGroupDirection, intervalLength.basicLength)
                    addNoteView(noteView, extremumMusicalHeight, subGroupDirection, horizontalMarginInt, isWhole)
                    if (isDotted){
                        createConstrainedNoteDotView(noteView, subGroupDirection, isWhole)
                    }
                    sortedMusicalNoteHeights.removeAt(0)

                    // Visualize remaining notes as note heads.
                    var lastNoteHeadView : ImageView? = null
                    var nextMusicHeight : Int? = sortedMusicalNoteHeights.firstOrNull()
                    var nextIsMirrored = if(nextMusicHeight != null) kotlin.math.abs(nextMusicHeight - extremumMusicalHeight) == 1 else null
                    var isMirrored = false
                    for (i in sortedMusicalNoteHeights.indices) {
                        if (nextIsMirrored != null) {
                            val musicalNoteHeadHeight = sortedMusicalNoteHeights[i]
                            isMirrored = nextIsMirrored
                            // If two notes are on successive heights, they would overlap. Therefore, one of them needs to be mirrored with the common
                            // stem as axis.

                            if (i + 1 < sortedMusicalNoteHeights.size) {
                                nextMusicHeight = sortedMusicalNoteHeights[i + 1]
                                if (kotlin.math.abs(nextMusicHeight - musicalNoteHeadHeight) == 1) {
                                    nextIsMirrored = !isMirrored
                                }
                                else {
                                    nextIsMirrored = false
                                }
                            }
                            else nextIsMirrored = null

                            lastNoteHeadView = createNoteHeadView(intervalLength.basicLength, isMirrored)
                            // Adapt current horizontal margin to mirrored note heads.
                            var adaptedHorizontalMargin = horizontalMarginInt
                            if (isMirrored) {
                                // increase margin because mirroring moves note head from left to right of stem
                                if (subGroupDirection == StemDirection.UP) {
                                    adaptedHorizontalMargin += lastNoteHeadView.layoutParams.width - noteStemWidth
                                }
                                // decrease margin because mirroring moves note head from right to left of stem
                                else {
                                    adaptedHorizontalMargin -= lastNoteHeadView.layoutParams.width - noteStemWidth
                                }
                            }
                            addNoteView(lastNoteHeadView, musicalNoteHeadHeight, subGroupDirection, adaptedHorizontalMargin, isWhole)
                            if (isDotted) {
                                if (subGroupDirection == StemDirection.DOWN && !isMirrored) {
                                    createConstrainedNoteDotView(lastNoteHeadView, subGroupDirection, isWhole)
                                }
                                else if (subGroupDirection == StemDirection.UP) {
                                    if (nextIsMirrored != null) {
                                        if (!nextIsMirrored) {
                                            createConstrainedNoteDotView(lastNoteHeadView, subGroupDirection, isWhole)
                                        }
                                    }
                                    else {
                                        if (isMirrored) {
                                            createConstrainedNoteDotView(lastNoteHeadView, subGroupDirection, isWhole)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Add common stem for all note intervals.
                    if (sortedMusicalNoteHeights.size > 0) {
                        if (lastNoteHeadView != null){
                            // Whole notes generally have no stem and therefore also no connecting multi stem.
                            if (!isWhole) {
                                if (subGroupDirection == StemDirection.UP) {
                                    addMultiNoteStem(sortedMusicalNoteHeights.last(), extremumMusicalHeight, subGroupDirection, lastNoteHeadView, isMirrored)
                                } else {
                                    addMultiNoteStem(extremumMusicalHeight, sortedMusicalNoteHeights.last(), subGroupDirection, lastNoteHeadView, isMirrored)
                                }
                            }
                        }
                        else {
                            // won't happen
                        }
                    }

                } else {
                    // Visualize as rest.
                    val restView = createRestView(intervalLength.basicLength)
                    addRestView(restView, if (voiceDirection == null) null else voice.getAvgNoteHeight(), intervalLength.basicLength, intervalWidth, horizontalMarginInt)
                    if (isDotted) {
                        createConstrainedRestDotView(restView, intervalLength)
                    }
                }

                // Increase horizontal margin according to rhythmic interval length
                horizontalMargin += intervalWidth
            }
        }

    }

    /**
     * Creates and returns a note view with a generated id, facing upwards or downwards based on [stemDirection].
     */
    private fun createNoteView(stemDirection: StemDirection, basicLength: BasicRhythmicLength) : ImageView {
        val noteView = ImageView(context)
        noteView.id = View.generateViewId()
        // Set height & width of note image.
        val noteHeight = noteHeightFromNodeHeadHeight(basicLength,verticalMusicHeightStep * 2)
        val noteLayout = ViewGroup.LayoutParams(noteWidthFromHeight(basicLength, noteHeight).toInt(), noteHeight.toInt())
        noteView.layoutParams = noteLayout
        noteView.setImageResource(when(basicLength){
            BasicRhythmicLength.WHOLE -> R.drawable.ic_whole
            BasicRhythmicLength.HALF -> R.drawable.ic_half
            BasicRhythmicLength.QUARTER -> R.drawable.ic_quarter
            BasicRhythmicLength.EIGHTH -> R.drawable.ic_eighth
            BasicRhythmicLength.SIXTEENTH -> R.drawable.ic_sixteenth
        })
        if (stemDirection == StemDirection.DOWN && basicLength !== BasicRhythmicLength.WHOLE){
            noteView.scaleX = -1f
            noteView.scaleY = -1f
        }

        return  noteView
    }

    /**
     * Creates and returns a dot view with id and constrains it to the given note view. To top when [stemDirection] is [StemDirection.DOWN],
     * to bottom otherwise.
     *
     * @throws IllegalStateException When id of this layout has not been set, i.e. generated, because constraining is not possible then.
     */
    private fun createConstrainedNoteDotView(noteView: ImageView, stemDirection: StemDirection, isWhole: Boolean): ImageView{
        if (this.id == 0){
            throw IllegalStateException("Can't constrain elements because this instance has no id!")
        }

        val noteHeadWidth =
                if (!isWhole) noteHeadWidthForNonWholes
                else noteWidthFromHeight(BasicRhythmicLength.WHOLE, noteHeadHeight.toDouble()).toInt()

        val dotView = ImageView(context)
        dotView.id = ImageView.generateViewId()
        dotView.layoutParams = ViewGroup.LayoutParams(dotDiameter, dotDiameter)
        dotView.setImageResource(R.drawable.black_circle)

        this.addView(dotView)
        val constraintSet = ConstraintSet()
        constraintSet.clone(this)

        when (stemDirection) {
            // Constrain bottom to note view bottom & left to (left + noteHeadWidth)
            StemDirection.UP -> {
                constraintSet.connect(dotView.id, ConstraintSet.BOTTOM, noteView.id, ConstraintSet.BOTTOM, dotMarginToUpNoteBottom)
                constraintSet.connect(dotView.id, ConstraintSet.LEFT, noteView.id, ConstraintSet.LEFT, noteHeadWidth + dotToNoteDistance)
            }
            // Constrain top to note view top with inverse margin (relative to note head) & left to right
            StemDirection.DOWN -> {
                constraintSet.connect(dotView.id, ConstraintSet.TOP, noteView.id, ConstraintSet.TOP, noteHeadHeight - dotMarginToUpNoteBottom)
                constraintSet.connect(dotView.id, ConstraintSet.LEFT, noteView.id, ConstraintSet.RIGHT, dotToNoteDistance)
            }
        }

        constraintSet.applyTo(this)

        return dotView
    }

    /**
     * Creates and returns note head view with id potentially mirrored along y-axis.
      */
    private fun createNoteHeadView(basicLength: BasicRhythmicLength, isMirrored: Boolean): ImageView{
        val noteHeadView = ImageView(context)
        noteHeadView.id = View.generateViewId()
        // Set height and width.
        val noteHeadWidth =
                if (basicLength != BasicRhythmicLength.WHOLE) noteHeadWidthForNonWholes
                else noteWidthFromHeight(BasicRhythmicLength.WHOLE, noteHeadHeight.toDouble()).toInt()
        noteHeadView.layoutParams = ViewGroup.LayoutParams(noteHeadWidth, noteHeadHeight)
        noteHeadView.setImageResource(when(basicLength){
            BasicRhythmicLength.WHOLE -> R.drawable.ic_whole
            BasicRhythmicLength.HALF -> R.drawable.ic_half_notehead
            else -> R.drawable.ic_full_notehead
        })

        if (isMirrored && basicLength != BasicRhythmicLength.WHOLE){
            noteHeadView.scaleX = -1f
        }

        return noteHeadView
    }

    /**
     * Creates and returns a specific rest view, based on its [RhythmicLength], with id.
     */
    private fun createRestView(basicLength: BasicRhythmicLength) : ImageView{
        // only applies to eighth rests
        val restHeight = (barHeight * 2 * 0.9 / 4)
        val restView = ImageView(context)
        restView.id = View.generateViewId()
        restView.layoutParams = ViewGroup.LayoutParams(restWidthFromHeight(basicLength, restHeight).toInt(), restHeight.toInt())

        restView.scaleType = ImageView.ScaleType.FIT_XY
        restView.setImageResource(R.drawable.ic_rest_eighth)

        return restView
    }

    /**
     * Creates and returns a dot view with id and constrains it to the given rest view. To top or to bottom is
     * based on [restLength].
     *
     * @throws IllegalStateException When id of this layout has not been set, i.e. generated, because constraining is not possible then.
     */
    private fun createConstrainedRestDotView(restView: ImageView, restLength: RhythmicLength) : ImageView {
        if (this.id == 0){
            throw IllegalStateException("Can't constrain elements because this instance has no id!")
        }

        val dotView = ImageView(context)
        dotView.id = View.generateViewId()
        dotView.layoutParams = ViewGroup.LayoutParams(dotDiameter, dotDiameter)
        dotView.setImageResource(R.drawable.black_circle)

        this.addView(dotView)
        val constraintSet = ConstraintSet()
        constraintSet.clone(this)

        // only applies to eighth rests
        constraintSet.connect(dotView.id, ConstraintSet.TOP, restView.id, ConstraintSet.TOP, dotMarginToUpNoteBottom)
        constraintSet.connect(dotView.id, ConstraintSet.LEFT, restView.id, ConstraintSet.RIGHT, dotToNoteDistance)
        constraintSet.applyTo(this)

        return dotView
    }

    /**
     * Adds the given [noteView] to this layout, adds an underlying horizontal stroke for [musicalHeight]s 0 and 12,
     * and constrains it according to [stemDirection] and [horizontalMargin] to place it on the bar drawable view (independent of if there's any).
     *
     * @throws IllegalArgumentException When [musicalHeight] is not in range 0..12
     * @throws IllegalStateException When id of this layout has not been set, i.e. generated, because constraining is not possible then.
     */
    private fun addNoteView(noteView: ImageView, musicalHeight: Int, stemDirection: StemDirection, horizontalMargin: Int, isWhole: Boolean) {
        // error detection
        if (musicalHeight < 0 || musicalHeight > 12) {
            throw IllegalArgumentException("Height can't be less than 0 or larger than 12!")
        }
        if (this.id == 0){
            throw IllegalStateException("Can't constrain view because this instance was not laid out yet.")
        }

        val noteHeadWidth =
                if (!isWhole) noteHeadWidthForNonWholes
                else noteWidthFromHeight(BasicRhythmicLength.WHOLE, noteHeadHeight.toDouble()).toInt()

        this.addView(noteView)
        val constraintSet = ConstraintSet()

        if (musicalHeight in listOf(0, 12)){
            addHorizontalStroke(musicalHeight, horizontalMargin, stemDirection, noteHeadWidth)
        }
        constraintSet.clone(this)

        // Handle upwards notes / note heads.
        if (stemDirection == StemDirection.UP){
            // horizontal constraints
            constraintSet.connect(noteView.id, ConstraintSet.LEFT, this.id, ConstraintSet.LEFT, horizontalMargin)
            // vertical constraints
            val marginBottomToScreenBottom = (smallestMusicHeightToBottomMargin + verticalMusicHeightStep * musicalHeight).toInt()
            constraintSet.connect(noteView.id, ConstraintSet.BOTTOM, this.id, ConstraintSet.BOTTOM, marginBottomToScreenBottom)
        }
        // Handle downwards notes / note heads.
        else {
            // horizontal constraints
            constraintSet.connect(noteView.id, ConstraintSet.RIGHT, this.id, ConstraintSet.RIGHT, width - horizontalMargin - noteHeadWidth)
            // vertical constraints
            val marginTopToScreenTop = (smallestMusicHeightToBottomMargin + verticalMusicHeightStep * (12 - musicalHeight)).toInt()
            constraintSet.connect(noteView.id, ConstraintSet.TOP, this.id, ConstraintSet.TOP, marginTopToScreenTop)

        }

        constraintSet.applyTo(this)
    }

    /**
     * Creates and adds a horizontal stroke view for notes on [musicalHeight]s 0 and 12, missing bar strokes because they're above or below
     * the bar. Constrained based on [horizontalMargin] and [stemDirection], which should be derived from the note this stroke is placed under.
     *
     * @throws IllegalArgumentException When musical height is not 0 or 12.
     * @throws IllegalStateException When id of this layout has not been set, i.e. generated, because constraining is not possible then.
     */
    private fun addHorizontalStroke(musicalHeight: Int, horizontalMargin: Int, stemDirection: StemDirection, noteHeadWidth: Int){
        if (this.id == 0){
            throw IllegalStateException("Can't constrain view because this instance was not laid out yet.")
        }
        if (musicalHeight !in listOf(0, 12)){
            throw IllegalArgumentException("Specified height must be either highest (12) or lowest (0)!")
        }

        val verticalMargin = (smallestMusicHeightToBottomMargin + verticalMusicHeightStep - barStrokeWidth / 2.0).toInt()

        val optionalStrokeView = ImageView(context)
        optionalStrokeView.id = View.generateViewId()
        optionalStrokeView.scaleType = ImageView.ScaleType.FIT_XY
        optionalStrokeView.layoutParams = ViewGroup.LayoutParams((noteHeadWidth * 1.33).toInt(), barStrokeWidth)
        optionalStrokeView.setImageResource(R.drawable.black_rectangle)

        this.addView(optionalStrokeView)
        val constraintSet = ConstraintSet()
        constraintSet.clone(this)

        // Constrain to left or right of bar (similar to notes).
        if (stemDirection == StemDirection.UP){
            constraintSet.connect(optionalStrokeView.id, ConstraintSet.LEFT, this.id, ConstraintSet.LEFT, horizontalMargin - (noteHeadWidth * 0.165).toInt())
        }
        else {
            constraintSet.connect(optionalStrokeView.id, ConstraintSet.RIGHT, this.id, ConstraintSet.RIGHT, width - horizontalMargin - (noteHeadWidth * 1.165).toInt())
        }

        // Constrain to top or bottom of bar.
        if (musicalHeight == 12){
            constraintSet.connect(optionalStrokeView.id, ConstraintSet.TOP, this.id, ConstraintSet.TOP, verticalMargin)
        }
        else {
            constraintSet.connect(optionalStrokeView.id, ConstraintSet.BOTTOM, this.id, ConstraintSet.BOTTOM, verticalMargin)
        }

        constraintSet.applyTo(this)
    }

    /**
     * Adds the given [restView] to this layout, and constrains it to the centre of its voice and the centre of the horizontal voice section of the given [length],
     * via [avgVoiceNoteHeight], [intervalWidth] and [horizontalMargin].
     *
     * @throws IllegalStateException When id of this layout has not been set, i.e. generated, because constraining is not possible then.
     */
    private fun addRestView(restView: ImageView, avgVoiceNoteHeight: Double?, basicLength: BasicRhythmicLength, intervalWidth: Double, horizontalMargin: Int){
        if (this.id == 0){
            throw IllegalStateException("Can't constrain elements because this instance has no id!")
        }

        // only applies to eighth rests
        val restHeight = (barHeight * 2 * 0.9 / 4)
        val restWidth = restWidthFromHeight(basicLength, restHeight)

        // for constraining to middle of interval width
        // only applies to eighth rests
        val leftMargin = (horizontalMargin + (intervalWidth - restWidth) / 2.0).toInt()
        val voiceCentreNoteHeight = avgVoiceNoteHeight ?: 6.0
        val voiceCentreMargin = (smallestMusicHeightToBottomMargin + (13 - voiceCentreNoteHeight) * verticalMusicHeightStep)
        val verticalMargin = (voiceCentreMargin - restHeight / 2).toInt()

        this.addView(restView)
        val constraintSet = ConstraintSet()
        constraintSet.clone(this)

        constraintSet.connect(restView.id, ConstraintSet.LEFT, this.id, ConstraintSet.LEFT, leftMargin)
        // only applies to eighth rests
        constraintSet.connect(restView.id, ConstraintSet.TOP, this.id, ConstraintSet.TOP, verticalMargin)
        constraintSet.applyTo(this)
    }

    /**
     * Adds a common stem view (for notes of one interval) reaching from [minMusicalHeight] to [maxMusicalHeight],
     * constrained to position of note head placed last, [startNoteHeadView], while considering if it [isMirrored].
     *
     * @throws IllegalArgumentException When one of the musical heights is not in the range 0..12
     * @throws IllegalStateException When id of this layout has not been set, i.e. generated, because constraining is not possible then.
     */
    private fun addMultiNoteStem(minMusicalHeight: Int, maxMusicalHeight: Int, stemDirection: StemDirection, startNoteHeadView: ImageView, isMirrored: Boolean){
        if (minMusicalHeight < 0 || minMusicalHeight > 12){
            throw IllegalArgumentException("minMusicalHeight can't be less than 0 or larger than 12!")
        }
        if (maxMusicalHeight < 0 || maxMusicalHeight > 12){
            throw IllegalArgumentException("maxMusicalHeight can't be less than 0 or larger than 12!")
        }
        if (maxMusicalHeight - minMusicalHeight < 1){
            throw IllegalArgumentException("max - min musical height must be at least 1!")
        }

        val multiNoteStemView = ImageView(context)
        multiNoteStemView.id = View.generateViewId()
        multiNoteStemView.scaleType = ImageView.ScaleType.FIT_XY
        val newMultiStemHeight = ((maxMusicalHeight - minMusicalHeight) * verticalMusicHeightStep).toInt()
        multiNoteStemView.layoutParams = ViewGroup.LayoutParams(noteStemWidth, newMultiStemHeight)
        multiNoteStemView.setImageResource(R.drawable.black_rectangle)

        this.addView(multiNoteStemView)
        val constraintSet = ConstraintSet()
        constraintSet.clone(this)

        // Constrain to bottom of note head.
        if (stemDirection == StemDirection.UP){
            constraintSet.connect(multiNoteStemView.id, ConstraintSet.BOTTOM, startNoteHeadView.id, ConstraintSet.BOTTOM, noteStemStartHeight)
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
            constraintSet.connect(multiNoteStemView.id, ConstraintSet.TOP, startNoteHeadView.id, ConstraintSet.TOP, noteStemStartHeight)
            // Constrain to left of note head.
            if (!isMirrored){
                constraintSet.connect(multiNoteStemView.id, ConstraintSet.LEFT, startNoteHeadView.id, ConstraintSet.LEFT)
            }
            // Constrain to right of note head.
            else {
                constraintSet.connect(multiNoteStemView.id, ConstraintSet.RIGHT, startNoteHeadView.id, ConstraintSet.RIGHT)
            }
        }
        constraintSet.applyTo(this)
    }



    /**
     * Calculates the percentage of note area width an [interval] of a certain [RhythmicLength] should take up, based
     * on the amount of time units in the [timeSignature] and if [interval] is the rhythmically last of [subGroup] (not sorted),
     * which will get one or multiple "between subgroups" padding.
     */
    private fun calculateWidthPercent(interval: RhythmicInterval, subGroup: SubGroup, timeSignature: TimeSignature): Double {
        val notesPercentWithoutPadding = 1 - (timeSignature.numberOfSubGroups - 1) * WIDTH_OF_ONE_SUBGROUP_PADDING_PERCENT
        val intervalPercentWithoutPadding = notesPercentWithoutPadding * interval.getLengthCopy().lengthInUnits / timeSignature.units
        var subGroupPaddingPercent = 0.0
        if (subGroup.isLast(interval)) {
            subGroupPaddingPercent = WIDTH_OF_ONE_SUBGROUP_PADDING_PERCENT * subGroup.paddingFactor
        }
        return intervalPercentWithoutPadding + subGroupPaddingPercent
    }
}