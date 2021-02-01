package com.example.sheetmusicapp.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnLayout
import com.example.sheetmusicapp.R
import com.example.sheetmusicapp.scoreModel.*
import kotlin.IllegalStateException
import kotlin.math.round
import kotlin.math.max

enum class IntervalDoubleConnectionType {
    DOUBLE,
    NONE,
    ONE_THIRD_START,
    ONE_THIRD_END,
    ONE_QUARTER_END
}


const val noteHeightToNoteHeadHeightRatio = 1 / 0.2741

const val noteHeadWidthToNoteHeightRatio = 0.3474
const val noteHeadWidthToHeightRatio = noteHeadWidthToNoteHeightRatio * noteHeightToNoteHeadHeightRatio

const val noteStemWidthToNoteHeightRatio = 0.0362
const val noteStemWidthToNoteHeadHeightRatio = noteHeightToNoteHeadHeightRatio * noteStemWidthToNoteHeightRatio
const val ellipticStemStartFromUpNoteBottomToNoteHeightRatio = 0.1816
const val ellipticStemStartFromUpNoteBottomToNoteHeadHeightRatio = ellipticStemStartFromUpNoteBottomToNoteHeightRatio * noteHeightToNoteHeadHeightRatio

const val dotDiameterToNoteHeadWidthRatio = 0.2803
const val dotDiameterToNoteHeadHeightRatio = dotDiameterToNoteHeadWidthRatio * noteHeadWidthToHeightRatio

const val dotNoteDistanceToNoteHeadWidthRatio = 0.2179
const val dotNoteDistanceToNoteHeadHeightRatio = dotNoteDistanceToNoteHeadWidthRatio * noteHeadWidthToHeightRatio

const val barStrokeWidthToBarHeightRatio = 0.005

// Constant specifying the combined percentage of width of the padding elements on the right and the left of a UI bar.
const val BAR_LEFTRIGHT_PADDING_PERCENT = 0.2
// Constant specifying the combined percentage of width of all notes, depending on the padding fractions.
const val BAR_NOTES_PERCENT = 1 - BAR_LEFTRIGHT_PADDING_PERCENT
const val WIDTH_OF_ONE_SUBGROUP_PADDING_PERCENT = 0.025

class BarVisLayout(context: Context, private val barHeight: Int, initBar: Bar) : ConstraintLayout(context) {

    var bar : Bar = initBar
        set(value) {
            field = value
            visualizeBar()
        }
    private val barDetailViews : MutableList<ImageView> = mutableListOf()
    private var barView: BarDrawableView? = null
    private var barNrView : TextView? = null

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
    private var ellipticStemToBottomHeight : Int = 0
    // note dot views
    private var dotDiameter : Int = 0
    private var dotToNoteDistance : Int = 0
    private var dotMarginToUpNoteBottom : Int = 0

    private var editingOverlayCallback: ((MutableList<Int>, Int) -> Unit)? = null

    init {

        // dynamically add content after this was laid out
        doOnLayout {
            calculateElementParams()
            barView = addBarView()
            barNrView = addBarNrView()
            visualizeBar()
        }
    }

    fun setEditingOverlayCallback(callback: ((MutableList<Int>, Int) -> Unit)) {
        editingOverlayCallback = callback
    }

    private fun addBarDetailView(view: ImageView){
        barDetailViews.add(view)
        super.addView(view)
    }

    /**
     * Calculates widths, heights and auxiliary margins / margin modifiers from current layout.
     * @throws IllegalStateException When this was not laid out yet.
     */
    private fun calculateElementParams(){
        if (height == 0 || width == 0){
            throw IllegalStateException("Parameters of elements can't be fully calculated because width or height is 0!")
        }

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
        ellipticStemToBottomHeight = (noteHeadHeightFloat * ellipticStemStartFromUpNoteBottomToNoteHeadHeightRatio).toInt()
        dotDiameter = (noteHeadHeightFloat * dotDiameterToNoteHeadHeightRatio).toInt()
        dotToNoteDistance = (noteHeadHeightFloat * dotNoteDistanceToNoteHeadHeightRatio).toInt()
        dotMarginToUpNoteBottom = (noteHeadHeight / 2.0 - 0.25 * dotDiameter).toInt()
    }

    /**
     * Adds and constrains fundamental bar paths, as [BarDrawableView].
     *
     * @throws IllegalStateException When id of this layout has not been set, i.e. generated, because constraining is not possible then.
     */
    private fun addBarView() : BarDrawableView{

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

        return barView
    }

    private fun addBarNrView() : TextView {

        val currentBarView = barView
                ?: throw IllegalStateException("Can't constrain bar number view because a BarDrawableView was not created!")

        val barNrView = TextView(context)
        barNrView.id = generateViewId()
        barNrView.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        barNrView.text = bar.barNr.toString()
        barNrView.setTextColor(resources.getColor(R.color.black))
        barNrView.textSize = 15f
        this.addView(barNrView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(this)
        constraintSet.connect(barNrView.id, ConstraintSet.BOTTOM, currentBarView.id, ConstraintSet.TOP)
        constraintSet.connect(barNrView.id, ConstraintSet.LEFT, this.id, ConstraintSet.LEFT)
        constraintSet.applyTo(this)

        return barNrView
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
    fun visualizeBar(){

        barDetailViews.forEach {
            removeView(it)
        }

        val currentBarNrView = barNrView
                ?: throw IllegalStateException("Bar number view needs updating, but does not exist!")
        currentBarNrView.text = bar.barNr.toString()
        val nonVisualizedVoiceNums = mutableListOf(1,2,3,4)
        for (voicePair in bar.voices){
            // voice only has common stem direction if there are multiple voices
            val voice = voicePair.value
            nonVisualizedVoiceNums.remove(voicePair.key)
            val voiceDirection: StemDirection? = voice.stemDirection
            val subGroups: MutableList<SubGroup> = voice.getCopyOfSubGroups()
            val intervalSubGroupIdxs: Map<RhythmicInterval, Int> = voice.getIntervalSubGroupIdxsCopy()

            // horizontal margin for iterative positioning starts after left bar padding
            var horizontalMargin = (width * (BAR_LEFTRIGHT_PADDING_PERCENT / 2))
            val horizontalMargins = mutableListOf<Int>()

            var connectedIntervalCount = 0
            var connectionGroupExtremumHeight = 0
            var intervalConnectionGroup : MutableList<RhythmicInterval>? = null

            for (interval in voice.intervals){

                val horizontalMarginInt = horizontalMargin.toInt()
                horizontalMargins.add(horizontalMarginInt)

                // Find subgroup of interval.
                val intervalSubGroupIdx : Int = intervalSubGroupIdxs[interval] ?: throw IllegalStateException("No subgroup was mapped for an interval in a voice!")
                if (intervalSubGroupIdx >= subGroups.size){
                    throw IllegalStateException("The mapped index of a subgroup exceeds the voice's subgroup list!")
                }
                val subGroup : SubGroup = subGroups[intervalSubGroupIdx]
                // If there's a voice direction, use this for the subgroup, otherwise let subgroup decide.
                val subGroupDirection = voiceDirection ?: subGroup.getStemDirection()

                val intervalWidth : Double = noteAreaWidth * calculateWidthPercent(interval, subGroup, voice.timeSignature)
                val intervalWidthMinusOnePadding = noteAreaWidth * calculateWidthPercentMinusOnePadding(interval, subGroup, voice.timeSignature)

                if (connectedIntervalCount == 0){
                    intervalConnectionGroup = null
                    if (subGroup.connectedIntervals.isNotEmpty()) {
                        for (intervalGroup in subGroup.connectedIntervals){
                            if (intervalGroup.contains(interval)) {
                                intervalConnectionGroup = intervalGroup
                                break
                            }
                        }

                        if (intervalConnectionGroup != null){

                            if (intervalConnectionGroup.size == 1){
                                throw IllegalStateException("A group of connected intervals must be of size 2 or larger!")
                            }

                            // Calculate highest / lowest musical height in group of connected intervals.
                            connectionGroupExtremumHeight = when (subGroupDirection){
                                StemDirection.UP -> -1
                                StemDirection.DOWN -> 13
                            }
                            for (connectedInterval in intervalConnectionGroup){
                                if (!connectedInterval.isRest) {
                                    when (subGroupDirection) {
                                        StemDirection.UP -> {
                                            val maxIntervalHeight : Int = connectedInterval.getNoteHeadsCopy().keys.maxOrNull()
                                                ?: throw IllegalStateException("The interval can't be empty!")
                                            if (maxIntervalHeight > connectionGroupExtremumHeight){
                                                connectionGroupExtremumHeight = maxIntervalHeight
                                            }
                                        }
                                        StemDirection.DOWN -> {
                                            val minIntervalHeight : Int = connectedInterval.getNoteHeadsCopy().keys.minOrNull()
                                                ?: throw IllegalStateException("The interval can't be empty!")
                                            if (minIntervalHeight < connectionGroupExtremumHeight){
                                                connectionGroupExtremumHeight = minIntervalHeight
                                            }
                                        }
                                    }
                                }
                            }

                            visualizeConnectedInterval(interval, subGroupDirection, horizontalMarginInt, intervalWidth, connectionGroupExtremumHeight, false, calculateIntervalDoubleConnectionType(interval, intervalConnectionGroup))
                            connectedIntervalCount = intervalConnectionGroup.size - 1
                        }
                        else {
                            visualizeUnconnectedInterval(interval, intervalWidthMinusOnePadding, subGroupDirection, voice, horizontalMarginInt)
                        }
                    }
                    else {
                        visualizeUnconnectedInterval(interval, intervalWidthMinusOnePadding, subGroupDirection, voice, horizontalMarginInt)
                    }
                }
                else {
                    val isLast = connectedIntervalCount == 1
                    if (intervalConnectionGroup == null){
                        throw IllegalStateException("IntervalConnectionGroup is null even though not all intervals of the group were visualized, according to connectedIntervalCount.")
                    }
                    visualizeConnectedInterval(interval, subGroupDirection, horizontalMarginInt, intervalWidth, connectionGroupExtremumHeight, isLast, calculateIntervalDoubleConnectionType(interval, intervalConnectionGroup))
                    connectedIntervalCount--
                }

                // Increase horizontal margin according to rhythmic interval length
                horizontalMargin += intervalWidth
            }
            editingOverlayCallback?.invoke(horizontalMargins, voicePair.key)
        }

        for (voiceNum in nonVisualizedVoiceNums){
            editingOverlayCallback?.invoke(mutableListOf(), voiceNum)
        }
    }

    private fun calculateIntervalDoubleConnectionType(interval: RhythmicInterval, connectionGroup: MutableList<RhythmicInterval>) : IntervalDoubleConnectionType {
        val intervalIdx = connectionGroup.indexOf(interval)
        if (intervalIdx == -1){
            throw IllegalArgumentException("The given interval is not part of the given connection group!")
        }

        val intervalDoubleConnectionType : IntervalDoubleConnectionType

        if (intervalIdx == connectionGroup.size - 1){
            intervalDoubleConnectionType = IntervalDoubleConnectionType.NONE
        }
        else {
            val intervalLength = interval.getLengthCopy()
            val nextInterval = connectionGroup[intervalIdx + 1]
            when (intervalLength.lengthInUnits){
                RhythmicLength(BasicRhythmicLength.SIXTEENTH).lengthInUnits -> {
                    when (nextInterval.getLengthCopy().lengthInUnits){
                        RhythmicLength(BasicRhythmicLength.SIXTEENTH).lengthInUnits -> {
                            intervalDoubleConnectionType = IntervalDoubleConnectionType.DOUBLE
                        }
                        RhythmicLength(BasicRhythmicLength.EIGHTH).lengthInUnits, RhythmicLength(BasicRhythmicLength.EIGHTH, LengthModifier.DOTTED).lengthInUnits -> {
                            if (intervalIdx - 1 < 0){
                                intervalDoubleConnectionType =  IntervalDoubleConnectionType.ONE_THIRD_START
                            }
                            else {
                                val previousInterval = connectionGroup[intervalIdx - 1]
                                if (calculateIntervalDoubleConnectionType(previousInterval, connectionGroup) == IntervalDoubleConnectionType.NONE) {
                                    intervalDoubleConnectionType = IntervalDoubleConnectionType.ONE_THIRD_START
                                }
                                else {
                                    intervalDoubleConnectionType = IntervalDoubleConnectionType.NONE
                                }
                            }
                        }
                        else -> intervalDoubleConnectionType = IntervalDoubleConnectionType.NONE
                    }
                }
                RhythmicLength(BasicRhythmicLength.EIGHTH).lengthInUnits -> {
                    when (nextInterval.getLengthCopy().lengthInUnits){
                        RhythmicLength(BasicRhythmicLength.SIXTEENTH).lengthInUnits -> {
                            if (intervalIdx + 2 < connectionGroup.size){
                                val nextIntervalSuccessor = connectionGroup[intervalIdx + 2]
                                if (nextIntervalSuccessor.getLengthCopy().lengthInUnits != RhythmicLength(BasicRhythmicLength.SIXTEENTH).lengthInUnits){
                                    intervalDoubleConnectionType = IntervalDoubleConnectionType.ONE_THIRD_END
                                }
                                else intervalDoubleConnectionType = IntervalDoubleConnectionType.NONE
                            }
                            else intervalDoubleConnectionType = IntervalDoubleConnectionType.ONE_THIRD_END
                        }
                        else -> intervalDoubleConnectionType = IntervalDoubleConnectionType.NONE
                    }
                }
                RhythmicLength(BasicRhythmicLength.EIGHTH, LengthModifier.DOTTED).lengthInUnits -> {
                    when (nextInterval.getLengthCopy().lengthInUnits){
                        RhythmicLength(BasicRhythmicLength.SIXTEENTH).lengthInUnits -> {
                            intervalDoubleConnectionType = IntervalDoubleConnectionType.ONE_QUARTER_END
                        }
                        else -> intervalDoubleConnectionType = IntervalDoubleConnectionType.NONE
                    }
                }
                else -> throw IllegalStateException("Interval connection group has an interval with length which does not lead to connections!")
            }
        }

        return intervalDoubleConnectionType
    }

    private fun calculateIntervalConnectionToBottomMargin(connectedIntervals: MutableList<RhythmicInterval>, stemDirection: StemDirection) : Int{
        if (stemDirection == StemDirection.UP){
            var maxMusicHeight = -1
            for (interval in connectedIntervals){
                val localMaxMusicHeight = interval.getNoteHeadsCopy().keys.maxOrNull() ?: throw IllegalStateException("An interval without note heads is a rest, which shouldn't have connections!")
                if (localMaxMusicHeight >= maxMusicHeight) {
                    maxMusicHeight = localMaxMusicHeight
                }
            }
            return (smallestMusicHeightToBottomMargin + maxMusicHeight * verticalMusicHeightStep +
                    noteHeightFromNodeHeadHeight(BasicRhythmicLength.QUARTER, verticalMusicHeightStep * 2)).toInt()
        }
        else {
            var minMusicHeight = 13
            for (interval in connectedIntervals){
                val localMinMusicHeight : Int = interval.getNoteHeadsCopy().keys.minOrNull() ?: throw IllegalStateException("An interval without note heads is a rest, which shouldn't have connections!")
                if (localMinMusicHeight <= minMusicHeight){
                    minMusicHeight = localMinMusicHeight
                }
            }
            return (smallestMusicHeightToBottomMargin + (minMusicHeight + 2) * verticalMusicHeightStep -
                    noteHeightFromNodeHeadHeight(BasicRhythmicLength.QUARTER, verticalMusicHeightStep * 2)).toInt()
        }
    }

    private fun visualizeConnectedInterval(interval: RhythmicInterval, subGroupDirection: StemDirection, horizontalMargin: Int, intervalWidth: Double, connectionGroupExtremumHeight: Int, isLast: Boolean, intervalDoubleConnectionType: IntervalDoubleConnectionType){

        // Sort musical note heights of interval, so first will be the one displayed as note with proper stem.
        val noteHeads = interval.getNoteHeadsCopy()
        val musicalNoteHeights = noteHeads.keys
        val sortedMusicalNoteHeights = if (subGroupDirection == StemDirection.UP) (musicalNoteHeights.sortedDescending()).toMutableList() else musicalNoteHeights.sorted().toMutableList()

        if (sortedMusicalNoteHeights.isEmpty()){
            throw IllegalStateException("Interval is a rest, which should have no connections!")
        }

        val intervalLength = interval.getLengthCopy()
        if (intervalLength.basicLength !in listOf(BasicRhythmicLength.SIXTEENTH, BasicRhythmicLength.EIGHTH)){
            throw IllegalArgumentException("The given interval is not an eighth or sixteenth. Only those should be connected!")
        }
        val isDotted = intervalLength.lengthModifier == LengthModifier.DOTTED

        // Visualize remaining notes as note heads.
        var lastNoteHeadView : NoteView? = null
        var firstNoteHeadType : NoteHeadType? = null
        var nextIsMirrored : Boolean? = null
        var isMirrored = false

        // visualize note heads
        for (i in sortedMusicalNoteHeights.indices) {
            val musicalNoteHeadHeight = sortedMusicalNoteHeights[i]
            val noteHeadType = noteHeads[musicalNoteHeadHeight]
                    ?: throw IllegalStateException("noteHeads does not contain an element from sortedMusicalNoteHeights!")
            if (i == 0){
                firstNoteHeadType = noteHeadType
            }
            isMirrored = nextIsMirrored ?: false
            // If two notes are on successive heights, they would overlap. Therefore, one of them needs to be mirrored with the common
            // stem as axis.

            if (i + 1 < sortedMusicalNoteHeights.size) {
                val nextMusicHeight = sortedMusicalNoteHeights[i + 1]
                if (kotlin.math.abs(nextMusicHeight - musicalNoteHeadHeight) == 1) {
                    nextIsMirrored = !isMirrored
                }
                else {
                    nextIsMirrored = false
                }
            }
            else nextIsMirrored = null

            lastNoteHeadView = createNoteHeadView(intervalLength.basicLength, isMirrored, noteHeadType)
            // Adapt current horizontal margin to mirrored note heads.
            var adaptedHorizontalMargin = horizontalMargin
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
            addNoteView(lastNoteHeadView, musicalNoteHeadHeight, subGroupDirection, adaptedHorizontalMargin)
            if (isDotted) {
                if (subGroupDirection == StemDirection.DOWN && !isMirrored) {
                    createConstrainedNoteDotView(lastNoteHeadView, subGroupDirection, false)
                }
                else if (subGroupDirection == StemDirection.UP) {
                    if (nextIsMirrored != null) {
                        if (!nextIsMirrored) {
                            createConstrainedNoteDotView(lastNoteHeadView, subGroupDirection, false)
                        }
                    }
                    else {
                        createConstrainedNoteDotView(lastNoteHeadView, subGroupDirection, false)
                    }
                }
            }
        }

        var commonStemView : ImageView? = null
        // Add common stem for all interval notes.
        if (lastNoteHeadView != null && firstNoteHeadType != null){
            commonStemView = addMultiNoteStemForConnection(subGroupDirection, lastNoteHeadView, sortedMusicalNoteHeights.last(), connectionGroupExtremumHeight, isMirrored)
        }

        // Add connection to next interval.
        if (commonStemView != null) {
            if (!isLast) {
                addConnectionToNextInterval(commonStemView, intervalWidth, subGroupDirection, intervalDoubleConnectionType)
            }
        }
    }

    private fun addConnectionToNextInterval(currentIntervalStemView: ImageView, intervalWidth: Double, subGroupDirection: StemDirection, doubleConnectionType: IntervalDoubleConnectionType){
        val connectionStrokeWidth = noteStemWidth * 2
        val connectionWidth = intervalWidth.toInt()

        val connectionView = ImageView(context)
        connectionView.id = View.generateViewId()
        connectionView.scaleType = ImageView.ScaleType.FIT_XY
        connectionView.layoutParams = ViewGroup.LayoutParams(connectionWidth, connectionStrokeWidth)
        connectionView.setImageResource(R.drawable.black_rectangle)
        addBarDetailView(connectionView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(this)
        constraintSet.connect(connectionView.id, ConstraintSet.LEFT, currentIntervalStemView.id, ConstraintSet.RIGHT)
        if (subGroupDirection == StemDirection.UP){
            constraintSet.connect(connectionView.id, ConstraintSet.TOP, currentIntervalStemView.id, ConstraintSet.TOP)
        }
        else {
            constraintSet.connect(connectionView.id, ConstraintSet.BOTTOM, currentIntervalStemView.id, ConstraintSet.BOTTOM)
        }
        constraintSet.applyTo(this)

        if (doubleConnectionType != IntervalDoubleConnectionType.NONE) {

            val secondConnectionView = ImageView(context)
            secondConnectionView.id = generateViewId()
            secondConnectionView.scaleType = ImageView.ScaleType.FIT_XY

            when (doubleConnectionType) {
                IntervalDoubleConnectionType.DOUBLE -> {
                    secondConnectionView.layoutParams = LayoutParams(connectionWidth, connectionStrokeWidth)
                    secondConnectionView.setImageResource(R.drawable.black_rectangle)
                    addBarDetailView(secondConnectionView)

                    constraintSet.clone(this)
                    constraintSet.connect(secondConnectionView.id, ConstraintSet.LEFT, currentIntervalStemView.id, ConstraintSet.RIGHT)
                }
                IntervalDoubleConnectionType.ONE_THIRD_START -> {
                    secondConnectionView.layoutParams = LayoutParams((intervalWidth / 3).toInt(), connectionStrokeWidth)
                    secondConnectionView.setImageResource(R.drawable.black_rectangle)
                    addBarDetailView(secondConnectionView)

                    constraintSet.clone(this)
                    constraintSet.connect(secondConnectionView.id, ConstraintSet.LEFT, currentIntervalStemView.id, ConstraintSet.RIGHT)
                }
                IntervalDoubleConnectionType.ONE_THIRD_END -> {
                    secondConnectionView.layoutParams = LayoutParams((intervalWidth / 3).toInt(), connectionStrokeWidth)
                    secondConnectionView.setImageResource(R.drawable.black_rectangle)
                    addBarDetailView(secondConnectionView)

                    constraintSet.clone(this)
                    constraintSet.connect(secondConnectionView.id, ConstraintSet.LEFT, currentIntervalStemView.id, ConstraintSet.RIGHT, (intervalWidth * 2 / 3.0).toInt())
                }
                IntervalDoubleConnectionType.ONE_QUARTER_END -> {
                    secondConnectionView.layoutParams = LayoutParams((intervalWidth / 4).toInt(), connectionStrokeWidth)
                    secondConnectionView.setImageResource(R.drawable.black_rectangle)
                    addBarDetailView(secondConnectionView)

                    constraintSet.clone(this)
                    constraintSet.connect(secondConnectionView.id, ConstraintSet.LEFT, currentIntervalStemView.id, ConstraintSet.RIGHT, (intervalWidth * 3 / 4.0).toInt())
                }
            }

            if (subGroupDirection == StemDirection.UP) {
                constraintSet.connect(secondConnectionView.id, ConstraintSet.TOP, connectionView.id, ConstraintSet.BOTTOM, connectionStrokeWidth / 2)
            } else {
                constraintSet.connect(secondConnectionView.id, ConstraintSet.BOTTOM, connectionView.id, ConstraintSet.TOP, connectionStrokeWidth / 2)
            }
            constraintSet.applyTo(this)
        }
    }

    private fun visualizeUnconnectedInterval(interval: RhythmicInterval, intervalWidthMinusOnePadding: Double, subGroupDirection: StemDirection, voice: Voice, horizontalMargin: Int){
        val intervalLength = interval.getLengthCopy()
        val isWhole = intervalLength.basicLength == BasicRhythmicLength.WHOLE
        val isDotted = intervalLength.lengthModifier ==  LengthModifier.DOTTED
        val voiceDirection = voice.stemDirection

        // Sort musical note heights of interval, so first will be the one displayed as note with proper stem.
        val noteHeads = interval.getNoteHeadsCopy()
        val musicalNoteHeights = noteHeads.keys
        val sortedMusicalNoteHeights = if (subGroupDirection == StemDirection.UP) (musicalNoteHeights.sortedDescending()).toMutableList() else musicalNoteHeights.sorted().toMutableList()

        // if null, no notes are contained => display as rest
        val extremumMusicalHeight = sortedMusicalNoteHeights.firstOrNull()

        // if not, display as note(s)
        if (extremumMusicalHeight != null) {

            val extremumNoteHeadType = noteHeads[extremumMusicalHeight] ?: throw IllegalStateException("noteHeads does not contain an element from sortedMusicalNoteHeights!")

            // create top (for UP) or bottom (for down) note with fully visualized stem
            val noteView = createNoteView(subGroupDirection, intervalLength.basicLength, extremumNoteHeadType)
            addNoteView(noteView, extremumMusicalHeight, subGroupDirection, horizontalMargin)
            if (isDotted){
                createConstrainedNoteDotView(noteView, subGroupDirection, isWhole)
            }
            sortedMusicalNoteHeights.removeAt(0)

            // Visualize remaining notes as note heads.
            var lastNoteHeadView : NoteView? = null
            val firstNoteHeadType : NoteHeadType = noteHeads[extremumMusicalHeight] ?: throw IllegalStateException("Extremum musical height not contained in note heads.")
            var nextMusicHeight : Int? = sortedMusicalNoteHeights.firstOrNull()
            var nextIsMirrored = if(nextMusicHeight != null) kotlin.math.abs(nextMusicHeight - extremumMusicalHeight) == 1 else null
            var isMirrored = false
            for (i in sortedMusicalNoteHeights.indices) {
                val musicalNoteHeadHeight = sortedMusicalNoteHeights[i]
                val noteHeadType = noteHeads[musicalNoteHeadHeight]
                        ?: throw IllegalStateException("noteHeads does not contain an element from sortedMusicalNoteHeights!")

                if (nextIsMirrored != null) {
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

                    lastNoteHeadView = createNoteHeadView(intervalLength.basicLength, isMirrored, noteHeadType)
                    // Adapt current horizontal margin to mirrored note heads.
                    var adaptedHorizontalMargin = horizontalMargin
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
                    addNoteView(lastNoteHeadView, musicalNoteHeadHeight, subGroupDirection, adaptedHorizontalMargin)
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
                            addMultiNoteStem(sortedMusicalNoteHeights.last(), extremumMusicalHeight, subGroupDirection, lastNoteHeadView, firstNoteHeadType, isMirrored)
                        } else {
                            addMultiNoteStem(extremumMusicalHeight, sortedMusicalNoteHeights.last(), subGroupDirection, lastNoteHeadView, firstNoteHeadType, isMirrored)
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
            addRestView(restView, if (voiceDirection == null) null else voice.getAvgNoteHeight(), intervalLength.basicLength, intervalWidthMinusOnePadding, horizontalMargin)
            if (isDotted) {
                createConstrainedRestDotView(restView, intervalLength.basicLength)
            }
        }
    }

    /**
     * Creates and returns a note view with a generated id, facing upwards or downwards based on [stemDirection].
     */
    private fun createNoteView(stemDirection: StemDirection, basicLength: BasicRhythmicLength, type: NoteHeadType) : NoteView {
        val noteView = NoteView(context, type, basicLength)
        noteView.id = View.generateViewId()
        // Set height & width of note image.
        val noteHeight = noteHeightFromNodeHeadHeight(basicLength,verticalMusicHeightStep * 2)
        noteView.layoutParams = ViewGroup.LayoutParams(noteWidthFromHeight(basicLength, noteHeight, type).toInt(), noteHeight.toInt())
        noteView.setImageResource(when (type){
            NoteHeadType.ELLIPTIC -> {
                when(basicLength){
                    BasicRhythmicLength.WHOLE -> R.drawable.ic_whole
                    BasicRhythmicLength.HALF -> R.drawable.ic_half
                    BasicRhythmicLength.QUARTER -> R.drawable.ic_quarter
                    BasicRhythmicLength.EIGHTH -> R.drawable.ic_eighth
                    BasicRhythmicLength.SIXTEENTH -> R.drawable.ic_sixteenth
                }
            }
            NoteHeadType.CROSS -> {
                when (basicLength){
                    BasicRhythmicLength.QUARTER -> R.drawable.ic_x_quarter
                    BasicRhythmicLength.EIGHTH -> R.drawable.ic_x_eighth
                    BasicRhythmicLength.SIXTEENTH -> R.drawable.ic_x_sixteenth
                    else -> throw IllegalArgumentException("Crossed notes are only supported for 16ths, 8ths and quarters!")
                }
            }
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
    private fun createConstrainedNoteDotView(noteView: NoteView, stemDirection: StemDirection, isWhole: Boolean): ImageView{
        if (this.id == 0){
            throw IllegalStateException("Can't constrain elements because this instance has no id!")
        }
        if (noteView.headType == NoteHeadType.CROSS && isWhole){
            throw IllegalArgumentException("Crossed notes are not supported for wholes!")
        }

        val noteHeadWidth =
                if (!isWhole) noteHeadWidthForNonWholes
                else noteWidthFromHeight(BasicRhythmicLength.WHOLE, noteHeadHeight.toDouble(), noteView.headType).toInt()

        val dotView = ImageView(context)
        dotView.id = ImageView.generateViewId()
        dotView.layoutParams = ViewGroup.LayoutParams(dotDiameter, dotDiameter)
        dotView.setImageResource(R.drawable.black_circle)

        this.addBarDetailView(dotView)
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

    private fun getNoteHeadWidth(type: NoteHeadType, basicLength: BasicRhythmicLength) : Int{
        return when (type){
            NoteHeadType.ELLIPTIC -> {
                if (basicLength != BasicRhythmicLength.WHOLE) noteHeadWidthForNonWholes
                else noteWidthFromHeight(BasicRhythmicLength.WHOLE, noteHeadHeight.toDouble(), type).toInt()
            }
            NoteHeadType.CROSS -> {
                noteHeadHeight
            }
        }
    }

    /**
     * Creates and returns note head view with id potentially mirrored along y-axis.
      */
    private fun createNoteHeadView(basicLength: BasicRhythmicLength, isMirrored: Boolean, type: NoteHeadType): NoteView{
        val noteHeadView = NoteView(context, type, basicLength)
        noteHeadView.id = View.generateViewId()
        // Set height and width.

        val noteHeadWidth = getNoteHeadWidth(type, basicLength)

        noteHeadView.layoutParams = ViewGroup.LayoutParams(noteHeadWidth, noteHeadHeight)
        noteHeadView.setImageResource(when(type) {
            NoteHeadType.ELLIPTIC -> {
                when (basicLength) {
                    BasicRhythmicLength.WHOLE -> R.drawable.ic_whole
                    BasicRhythmicLength.HALF -> R.drawable.ic_half_notehead
                    else -> R.drawable.ic_full_notehead
                }
            }
            NoteHeadType.CROSS -> R.drawable.ic_x_notehead
        })

        if (type != NoteHeadType.CROSS) {
            if (isMirrored && basicLength != BasicRhythmicLength.WHOLE) {
                noteHeadView.scaleX = -1f
            }
        }

        return noteHeadView
    }

    private fun getRestHeight(basicLength: BasicRhythmicLength) : Double{
        return when (basicLength){
            BasicRhythmicLength.EIGHTH -> verticalMusicHeightStep * 4 * 0.9
            BasicRhythmicLength.SIXTEENTH, BasicRhythmicLength.QUARTER -> verticalMusicHeightStep * 6 * 0.9
            BasicRhythmicLength.HALF, BasicRhythmicLength.WHOLE -> verticalMusicHeightStep * 4/3.0
        }
    }

    /**
     * Creates and returns a specific rest view, based on its [RhythmicLength], with id.
     */
    private fun createRestView(basicLength: BasicRhythmicLength) : ImageView{
        // only applies to eighth rests
        val restHeight = getRestHeight(basicLength)
        val restView = ImageView(context)
        restView.id = View.generateViewId()
        restView.layoutParams = ViewGroup.LayoutParams(restWidthFromHeight(basicLength, restHeight).toInt(), restHeight.toInt())

        restView.scaleType = ImageView.ScaleType.FIT_XY
        restView.setImageResource(when(basicLength){
            BasicRhythmicLength.SIXTEENTH -> R.drawable.ic_rest_sixteenth
            BasicRhythmicLength.EIGHTH -> R.drawable.ic_rest_eighth
            BasicRhythmicLength.QUARTER -> R.drawable.ic_rest_quarter
            BasicRhythmicLength.HALF, BasicRhythmicLength.WHOLE -> R.drawable.ic_rest_half
        })

        // Whole rest is half rest upside down
        if (basicLength == BasicRhythmicLength.WHOLE){
            restView.scaleY = -1f
        }

        return restView
    }

    /**
     * Creates and returns a dot view with id and constrains it to the given rest view. To top or to bottom is
     * based on [basicLength].
     *
     * @throws IllegalStateException When id of this layout has not been set, i.e. generated, because constraining is not possible then.
     */
    private fun createConstrainedRestDotView(restView: ImageView, basicLength: BasicRhythmicLength) : ImageView {
        if (this.id == 0){
            throw IllegalStateException("Can't constrain elements because this instance has no id!")
        }

        val dotView = ImageView(context)
        dotView.id = View.generateViewId()
        dotView.layoutParams = ViewGroup.LayoutParams(dotDiameter, dotDiameter)
        dotView.setImageResource(R.drawable.black_circle)

        this.addBarDetailView(dotView)
        val constraintSet = ConstraintSet()
        constraintSet.clone(this)

        // constrain dot to top or bottom of rest
        if (basicLength in listOf(BasicRhythmicLength.SIXTEENTH, BasicRhythmicLength.EIGHTH, BasicRhythmicLength.WHOLE)){
            constraintSet.connect(dotView.id, ConstraintSet.TOP, restView.id, ConstraintSet.TOP, dotMarginToUpNoteBottom)
        }
        else {
            constraintSet.connect(dotView.id, ConstraintSet.BOTTOM, restView.id, ConstraintSet.BOTTOM, dotMarginToUpNoteBottom)
        }

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
    private fun addNoteView(noteView: NoteView, musicalHeight: Int, stemDirection: StemDirection, horizontalMargin: Int) {
        // error detection
        if (musicalHeight < 0 || musicalHeight > 12) {
            throw IllegalArgumentException("Height can't be less than 0 or larger than 12!")
        }
        if (this.id == 0){
            throw IllegalStateException("Can't constrain view because this instance was not laid out yet.")
        }

        val noteHeadWidth = (noteHeadHeight * noteHeadWidthToHeightRatio).toInt()

        this.addBarDetailView(noteView)
        val constraintSet = ConstraintSet()

        if (musicalHeight in listOf(0, 12)){
            addHorizontalStroke(musicalHeight, horizontalMargin, stemDirection, noteHeadWidth)
        }
        constraintSet.clone(this)

        var horizontalMarginModifier = 0
        if (noteView.headType == NoteHeadType.CROSS){
           horizontalMarginModifier = noteHeadWidth - getNoteHeadWidth(NoteHeadType.CROSS, noteView.basicLength)
        }

        // Handle upwards notes / note heads.
        if (stemDirection == StemDirection.UP){
            // horizontal constraints
            constraintSet.connect(noteView.id, ConstraintSet.LEFT, this.id, ConstraintSet.LEFT, horizontalMargin + horizontalMarginModifier)
            // vertical constraints
            val marginBottomToScreenBottom = (smallestMusicHeightToBottomMargin + verticalMusicHeightStep * musicalHeight).toInt()
            constraintSet.connect(noteView.id, ConstraintSet.BOTTOM, this.id, ConstraintSet.BOTTOM, marginBottomToScreenBottom)
        }
        // Handle downwards notes / note heads.
        else {
            // horizontal constraints
            constraintSet.connect(noteView.id, ConstraintSet.RIGHT, this.id, ConstraintSet.RIGHT, width - horizontalMargin - noteHeadWidth + horizontalMarginModifier)
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

        this.addBarDetailView(optionalStrokeView)
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
     * Adds the given [restView] to this layout, and constrains it to the centre of its voice and the centre of the horizontal voice section of the given [basicLength],
     * via [avgVoiceNoteHeight], [intervalWidth] and [horizontalMargin].
     *
     * @throws IllegalStateException When id of this layout has not been set, i.e. generated, because constraining is not possible then.
     */
    private fun addRestView(restView: ImageView, avgVoiceNoteHeight: Double?, basicLength: BasicRhythmicLength, intervalWidth: Double, horizontalMargin: Int){
        if (this.id == 0){
            throw IllegalStateException("Can't constrain elements because this instance has no id!")
        }

        // only applies to eighth rests
        val restHeight = getRestHeight(basicLength)
        val restWidth = restWidthFromHeight(basicLength, restHeight)

        // for constraining to middle of interval width
        val leftMargin = (horizontalMargin + (intervalWidth - restWidth) / 2.0).toInt()
        val voiceCentreNoteHeight = avgVoiceNoteHeight ?: 6.0

        this.addBarDetailView(restView)
        val constraintSet = ConstraintSet()
        constraintSet.clone(this)

        constraintSet.connect(restView.id, ConstraintSet.LEFT, this.id, ConstraintSet.LEFT, leftMargin)

        // constrain as middle to middle of voice vertically
        if (basicLength !in listOf(BasicRhythmicLength.HALF, BasicRhythmicLength.WHOLE)) {
            val voiceCentreMargin = (smallestMusicHeightToBottomMargin + (13 - voiceCentreNoteHeight) * verticalMusicHeightStep)
            val verticalMargin = (voiceCentreMargin - restHeight / 2.0).toInt()
            constraintSet.connect(restView.id, ConstraintSet.TOP, this.id, ConstraintSet.TOP, verticalMargin)
        }
        // constrain to top or bottom of rounded voice middle (i.e. a vertical position that's possible for notes)
        else {
            // make sure the rest is "attached" to horizontal bar stroke (or in position of optional horizontal above or below bar)
            val evenMusicHeightDerivedFromVoiceAvg = round(voiceCentreNoteHeight / 2.0) * 2
            // constrain to bottom
            if (basicLength == BasicRhythmicLength.HALF){
                val verticalMargin = (smallestMusicHeightToBottomMargin + (evenMusicHeightDerivedFromVoiceAvg + 1) * verticalMusicHeightStep - barStrokeWidth / 2.0).toInt()
                constraintSet.connect(restView.id, ConstraintSet.BOTTOM, this.id, ConstraintSet.BOTTOM, verticalMargin)
            }
            // constrain to top
            else {
                val verticalMargin = (smallestMusicHeightToBottomMargin + (13 - voiceCentreNoteHeight) * verticalMusicHeightStep - barStrokeWidth / 2.0).toInt()
                constraintSet.connect(restView.id, ConstraintSet.TOP, this.id, ConstraintSet.TOP, verticalMargin)
            }
        }
        constraintSet.applyTo(this)
    }

    /**
     * Adds a common stem view (for notes of one interval) reaching from [minMusicalHeight] to [maxMusicalHeight],
     * constrained to position of note head placed last, [startNoteHeadView], while considering if it [isMirrored].
     *
     * @throws IllegalArgumentException When one of the musical heights is not in the range 0..12
     * @throws IllegalStateException When id of this layout has not been set, i.e. generated, because constraining is not possible then.
     */
    private fun addMultiNoteStem(minMusicalHeight: Int, maxMusicalHeight: Int, stemDirection: StemDirection, startNoteHeadView: NoteView, endViewType: NoteHeadType, isMirrored: Boolean){
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
        var newMultiStemHeight = ((maxMusicalHeight - minMusicalHeight) * verticalMusicHeightStep).toInt()
        val startNoteHeadType = startNoteHeadView.headType
        if (startNoteHeadType != endViewType){
            val stemHeightModifier = noteHeadHeight - ellipticStemToBottomHeight
            when (startNoteHeadType){
                NoteHeadType.CROSS -> newMultiStemHeight -= stemHeightModifier
                NoteHeadType.ELLIPTIC -> newMultiStemHeight += stemHeightModifier
            }
        }
        val headTypeStemStartHeight = if (startNoteHeadType == NoteHeadType.ELLIPTIC) ellipticStemToBottomHeight else noteHeadHeight

        multiNoteStemView.layoutParams = ViewGroup.LayoutParams(noteStemWidth, newMultiStemHeight)
        multiNoteStemView.setImageResource(R.drawable.black_rectangle)

        this.addBarDetailView(multiNoteStemView)
        val constraintSet = ConstraintSet()
        constraintSet.clone(this)


        // Constrain to bottom of note head.
        if (stemDirection == StemDirection.UP){
            constraintSet.connect(multiNoteStemView.id, ConstraintSet.BOTTOM, startNoteHeadView.id, ConstraintSet.BOTTOM, headTypeStemStartHeight)
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
            constraintSet.connect(multiNoteStemView.id, ConstraintSet.TOP, startNoteHeadView.id, ConstraintSet.TOP, headTypeStemStartHeight)
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

    private fun addMultiNoteStemForConnection(stemDirection: StemDirection, startNoteHeadView: NoteView, startMusicHeight: Int, connectionGroupExtremumHeight: Int, isMirrored: Boolean) : ImageView{
        val multiNoteStemView = ImageView(context)
        multiNoteStemView.id = View.generateViewId()
        multiNoteStemView.scaleType = ImageView.ScaleType.FIT_XY

        val heightDifferenceToExtremum = verticalMusicHeightStep * when (stemDirection){
            StemDirection.UP -> {
                connectionGroupExtremumHeight - startMusicHeight
            }
            StemDirection.DOWN -> {
                startMusicHeight - connectionGroupExtremumHeight
            }
        }
        val headTypeStemStartHeight = if (startNoteHeadView.headType == NoteHeadType.ELLIPTIC) ellipticStemToBottomHeight else (verticalMusicHeightStep * 2).toInt()
        val multiStemHeight = noteHeightFromNodeHeadHeight(BasicRhythmicLength.QUARTER, verticalMusicHeightStep * 2) + heightDifferenceToExtremum - headTypeStemStartHeight

        multiNoteStemView.layoutParams = ViewGroup.LayoutParams(noteStemWidth, multiStemHeight.toInt())
        multiNoteStemView.setImageResource(R.drawable.black_rectangle)

        this.addBarDetailView(multiNoteStemView)
        val constraintSet = ConstraintSet()
        constraintSet.clone(this)

        // Constrain to bottom of note head.
        if (stemDirection == StemDirection.UP){
            constraintSet.connect(multiNoteStemView.id, ConstraintSet.BOTTOM, startNoteHeadView.id, ConstraintSet.BOTTOM, headTypeStemStartHeight)
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
            constraintSet.connect(multiNoteStemView.id, ConstraintSet.TOP, startNoteHeadView.id, ConstraintSet.TOP, headTypeStemStartHeight)
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
        return multiNoteStemView
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

    private fun calculateWidthPercentMinusOnePadding(interval: RhythmicInterval, subGroup: SubGroup, timeSignature: TimeSignature): Double {
        val notesPercentWithoutPadding = 1 - (timeSignature.numberOfSubGroups - 1) * WIDTH_OF_ONE_SUBGROUP_PADDING_PERCENT
        val intervalPercentWithoutPadding = notesPercentWithoutPadding * interval.getLengthCopy().lengthInUnits / timeSignature.units
        var subGroupPaddingPercent = 0.0
        if (subGroup.isLast(interval)) {
            subGroupPaddingPercent = WIDTH_OF_ONE_SUBGROUP_PADDING_PERCENT * max(0, subGroup.paddingFactor - 1)
        }
        return intervalPercentWithoutPadding + subGroupPaddingPercent
    }
}