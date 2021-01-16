package com.example.sheetmusicapp.scoreModel

import java.lang.IllegalArgumentException
import java.lang.IllegalStateException



// Constant specifying the combined percentage of width of the padding elements on the right and the left of a UI bar.
const val BAR_LEFTRIGHT_PADDING_PERCENT = 0.15
// Constant specifying the combined percentage of width of all notes, depending on the padding fractions.
const val BAR_NOTES_PERCENT = 1 - BAR_LEFTRIGHT_PADDING_PERCENT
const val WIDTH_OF_ONE_SUBGROUP_PADDING_PERCENT = 0.025

/**
 * Bar instances are the elements of a drum score. They have a certain time signature and consist of
 * voices out of different rhythmic intervals with their respective notes.
 *
 * @property barNr The number of the bar in a score. Can change after creation.
 * @property timeSignature The time signature of the bar. Can change after creation.
 * @property voices Map of voices of rhythmic intervals in this bar. [Voice] instances are created on construction
 * from interval lists in param voiceIntervals. Only voices "1" to "4" can exist.
 * @constructor Creates a bar which contains the given voices, with the given time signature and bar number.
 * @author Max Wendler
 */
class Bar(var barNr: Int, var timeSignature: TimeSignature, voiceIntervals: Map<Int,MutableList<RhythmicInterval>>) {

    val voices: MutableMap<Int, Voice> = mutableMapOf()
    // voice instantiation from voice intervals
    init {
        for (pair in voiceIntervals){
            if (pair.key !in 1..4){
                throw IllegalArgumentException("Faulty voice keys in constructor parameters. Only voices 1 to 4 can exist!")
            }
            voices[pair.key] = Voice(pair.value, timeSignature)
        }
    }

    /**
     * Creates and appends a [RhythmicInterval] instance which is a rest specified by a [RhythmicLength] to a voice of rhythmic intervals.
     * Private because it's currently only used for the creation of an empty voice by [addEmptyVoice].
     */
    private fun appendRest(voiceIntervals: MutableList<RhythmicInterval>, length: RhythmicLength){
        appendRhythmicInterval(voiceIntervals, length, mapOf<Int,NoteHeadType>())
    }

    /**
     * Creates and appends a [RhythmicInterval] instance, specified by a [RhythmicLength] and some note heads, to a voice of rhythmic intervals.
     * Private because it's only used for the creation of an empty voice by [addEmptyVoice] via [appendRest].
     *
     * See [RhythmicInterval] for details on the last two parameters.
     */
    private fun appendRhythmicInterval(voiceIntervals: MutableList<RhythmicInterval>, length: RhythmicLength, initNoteHeads: Map<Int, NoteHeadType>){

        // Get first unit after the currently last interval.
        var startUnit = 0
        val lastInterval = voiceIntervals.lastOrNull()
        if (lastInterval != null){
            startUnit += lastInterval.startUnit + lastInterval.getLengthCopy().lengthInUnits
        }
        else startUnit = 1

        if (startUnit > timeSignature.units) {
            throw IllegalArgumentException("The start unit exceeds the length of the bar in units.")
        }

        val endUnit = startUnit + (length.lengthInUnits - 1)
        if (endUnit > timeSignature.units){
            throw IllegalArgumentException("The end unit exceeds the length of the bar in units.")
        }

        // Create and add interval with width from widthPercentOfRhythmicLength.
        val interval = RhythmicInterval(length, initNoteHeads.toMutableMap(), startUnit)
        voiceIntervals.add(interval)
    }

    /**
     * Creates a voice with the given "Id", fills it with rests according to the bar's time signature,
     * and adds it to the voices of the bar instance.
     * Is private because it's only used when creating an empty bar via [makeEmpty] and when adding a note
     * in a UI voice mode of a voice that does not exist yet in [addNote].
     *
     * @param voice Id of the voice to add.
     * @throws IllegalArgumentException when the specified voice already exists.
     */
    fun addEmptyVoice(voice : Int){

        if (voices[voice] != null){
            throw IllegalArgumentException("The given voice does already exist and should not be overwritten.")
        }

        // List of rhythmic lengths which can be rests to fill a new bar with rests.
        // Because the time signatures of bars can only have halfs, quarters or eighths as denominators, only certain
        // lengths are required here.
        val restLengthsForEmptyBars : List<RhythmicLength> = listOf(
                RhythmicLength(BasicRhythmicLength.WHOLE, LengthModifier.DOTTED),
                RhythmicLength(BasicRhythmicLength.WHOLE),
                RhythmicLength(BasicRhythmicLength.HALF, LengthModifier.DOTTED),
                RhythmicLength(BasicRhythmicLength.HALF),
                RhythmicLength(BasicRhythmicLength.QUARTER, LengthModifier.DOTTED),
                RhythmicLength(BasicRhythmicLength.QUARTER),
                RhythmicLength(BasicRhythmicLength.EIGHTH)
        )

        val newVoiceIntervals = mutableListOf<RhythmicInterval>()
        var remainingBarUnits = timeSignature.units

        // Add rests the voice until no units remain in the bar.
        var idxOfLastFitting = 0
        while (remainingBarUnits > 0){
            for (i in idxOfLastFitting..restLengthsForEmptyBars.size){
                if (i == restLengthsForEmptyBars.size){
                    throw IllegalStateException("No rest length fits the rest of the bar.")
                }
                val currentRestLength = restLengthsForEmptyBars[i]
                if (currentRestLength.lengthInUnits <= remainingBarUnits){
                    appendRest(newVoiceIntervals, currentRestLength)
                    remainingBarUnits -= currentRestLength.lengthInUnits
                    idxOfLastFitting = i
                    break
                }
            }
        }

        voices[voice] = Voice(newVoiceIntervals, timeSignature)
    }

    fun addRest(voiceNum: Int, length: RhythmicLength, intervalIdx: Int){
        if (voiceNum !in 1..4){
            throw IllegalArgumentException("Only voices numbered 1 to 4 can exist.")
        }

        if (voices[voiceNum] == null){
            addEmptyVoice(voiceNum)
        }

        val voice = voices[voiceNum]

        if (voice != null){
            if (intervalIdx >= voice.intervals.size){
                throw IllegalArgumentException("The given interval index exceeds the amount of existing intervals in the specified voice.")
            }
            val intervalAtIdx = voice.intervals[intervalIdx]
            if (length.lengthInUnits != intervalAtIdx.getLengthCopy().lengthInUnits){
                changeIntervalLength(voice.intervals, length, intervalIdx)
                voice.recalculateSubGroupsFrom(intervalIdx)
            }
        }
    }

    /**
     * Adds a note of the specified [RhythmicLength], [NoteHeadType] and [height] to the given voice and the given interval position
     * If the length is lesser than the one of the interval currently at the specified position, rests will be added to fill the gap.
     * If the length is greater, following intervals will be removed or shortened accordingly.
     *
     * @throws IllegalArgumentException When an interval does not exist at the given index. (index out of bounds)
     * @throws IllegalArgumentException When the given rhythmic length exceeds the length of the bar's time signature when placed at the
     * position given by intervalIdx.
     */
    fun addNote(voiceNum: Int, length: RhythmicLength, type: NoteHeadType, height: Int, intervalIdx: Int){

        if (voiceNum !in 1..4){
            throw IllegalArgumentException("Only voices numbered 1 to 4 can exist.")
        }

        if (voices[voiceNum] == null){
            addEmptyVoice(voiceNum)
        }

        val voice = voices[voiceNum]

        if (voice != null){
            if (intervalIdx >= voice.intervals.size){
                throw IllegalArgumentException("The given interval index exceeds the amount of existing intervals in the specified voice.")
            }
            val intervalAtIdx = voice.intervals[intervalIdx]
            intervalAtIdx.addNoteHead(height, type)
            if (length.lengthInUnits != intervalAtIdx.getLengthCopy().lengthInUnits){
                changeIntervalLength(voice.intervals, length, intervalIdx)
                voice.recalculateSubGroupsFrom(intervalIdx)
            }
            calculateVoiceStemDirections()
        }
    }

    private fun changeIntervalLength(voiceIntervals: MutableList<RhythmicInterval>, length: RhythmicLength, intervalIdx: Int){
        if (intervalIdx >= voiceIntervals.size){
            throw IllegalArgumentException("The given interval index exceeds the amount of existing intervals in the specified voice.")
        }

        val currentIntervalAtIdx = voiceIntervals[intervalIdx]
        if (currentIntervalAtIdx.getLengthCopy().lengthInUnits == length.lengthInUnits){
            throw IllegalArgumentException("The given length is not different from the current one of the interval.")
        }
        else if (currentIntervalAtIdx.getLengthCopy().lengthInUnits > length.lengthInUnits){
            changeIntervalToSmaller(voiceIntervals, length, intervalIdx)
        }
        // (currentIntervalAtIdx.length.lengthInUnits < length.lengthInUnits) == true
        else {
            changeIntervalToLarger(voiceIntervals, length, intervalIdx)
        }
    }

    /**
     * Changes an interval of a voice given by [voiceIntervals] to a smaller length.
     * Rests are inserted to fill the emerging gap.
     */
    private fun changeIntervalToSmaller(voiceIntervals: MutableList<RhythmicInterval>, length: RhythmicLength, intervalIdx: Int){

        val intervalAtIdx = voiceIntervals[intervalIdx]

        // calculate rhythmic length in units remaining of replaced instance when replaced with interval of param length
        // and divide it into rhythmic length instances
        val unitLengthOfRemainder = intervalAtIdx.getLengthCopy().lengthInUnits - length.lengthInUnits
        val rhythmicLengthsForRemainder = lengthsFromUnitLengthAsc(unitLengthOfRemainder)

        intervalAtIdx.setLength(length)

        // insert intervals that are rests for the remaining rhythmic length after the replacing interval
        var insertRestIdx = intervalIdx + 1
        var insertRestStartUnit = voiceIntervals[intervalIdx].startUnit + length.lengthInUnits
        for (restLength in rhythmicLengthsForRemainder){
            voiceIntervals.add(insertRestIdx, RhythmicInterval.makeRest(restLength, insertRestStartUnit))
            insertRestIdx += 1
            insertRestStartUnit += restLength.lengthInUnits
        }
    }

    /**
     * Changes an interval of a voice given by [voiceIntervals] to a greater length.
     * Following intervals are removed or shortened accordingly.
     */
    private fun changeIntervalToLarger(voiceIntervals: MutableList<RhythmicInterval>, length: RhythmicLength, intervalIdx: Int){

        val intervalAtIdx = voiceIntervals[intervalIdx]
        // fault tolerance for too large input
        if (intervalAtIdx.startUnit + length.lengthInUnits > timeSignature.units){
            throw IllegalArgumentException("The given interval exceeds the rhythmic length of the bar when placed in the position given by intervalIdx.")
        }

        // Find following intervals fully or partially (cutInTwoInterval) swallowed by the growing interval.
        // Don't delete the intervals that will be fully replaced yet, so unmoved cutInTwoInterval can be handled.
        val replacingEndUnit = intervalAtIdx.startUnit + (length.lengthInUnits - 1)
        var cutInTwoInterval : RhythmicInterval? = null
        var lastReplacedIdx = intervalIdx + 1
        var potentiallyAlsoReplacedIdx = intervalIdx + 1
        while (voiceIntervals[potentiallyAlsoReplacedIdx].startUnit <= replacingEndUnit){
            val potentiallyAlsoReplaced = voiceIntervals[potentiallyAlsoReplacedIdx]
            if (potentiallyAlsoReplaced.endUnit <= replacingEndUnit){
                lastReplacedIdx = potentiallyAlsoReplacedIdx
            }
            else {
                cutInTwoInterval = potentiallyAlsoReplaced
                break
            }
            potentiallyAlsoReplacedIdx++
        }

        // Shorten the interval that's only partially swallowed by the changed one.
        if (cutInTwoInterval != null){

            // Calculate the length to shorten to and rhythmic lengths that can be combined to fit it.
            val remainingUnits = cutInTwoInterval.endUnit - replacingEndUnit
            val remainderLengths = lengthsFromUnitLengthAsc(remainingUnits)
            var newStartUnit = replacingEndUnit + 1

            // Shorten the length of the already existing interval.
            cutInTwoInterval.startUnit = newStartUnit
            val newCutInTwoLength = remainderLengths.removeAt(0)
            cutInTwoInterval.setLength(newCutInTwoLength)

            // Add following rests if the units remaining of the original shortened interval can be only expressed in multiple rhythmic lengths,
            // and therefore intervals.
            newStartUnit += cutInTwoInterval.getLengthCopy().lengthInUnits
            var newRestIdx = lastReplacedIdx + 1
            for (newRestLength in remainderLengths){
                voiceIntervals.add(newRestIdx, RhythmicInterval.makeRest(newRestLength, newStartUnit))
                newStartUnit += newRestLength.lengthInUnits
                newRestIdx++
            }

        }
        intervalAtIdx.setLength(length)

        // Remove intervals fully swallowed by the grown interval.
        for (i in (intervalIdx+1)..lastReplacedIdx){
            voiceIntervals.removeAt(intervalIdx + 1)
        }
    }

    /**
     * Calculates and sets the common stem direction of all bar voices. If only one voice exists,
     * its subgroups can decide their stem direction, therefore it'll be null.
     */
    private fun calculateVoiceStemDirections(){
        if (voices.size <= 1){
            voices.values.forEach { voice -> voice.stemDirection = null }
        }
        else {
            if (voices.size > 4){
                throw IllegalStateException("No more than four voices should be able to exist!")
            }
            else {
                val voicesByAvgHeightAsc = voices.values.sortedBy { it.getAvgNoteHeight() }
                voicesByAvgHeightAsc.first().stemDirection = StemDirection.UP
                voicesByAvgHeightAsc.last().stemDirection = StemDirection.DOWN
                when (voices.size){
                    3 -> {
                        voicesByAvgHeightAsc[1].stemDirection = StemDirection.UP
                    }
                    4 -> {
                        voicesByAvgHeightAsc[1].stemDirection = StemDirection.UP
                        voicesByAvgHeightAsc[2].stemDirection = StemDirection.DOWN
                    }
                }
            }
        }
    }

    companion object {

        /**
         * Creates an "empty" bar of a certain time signature, which contains only one complete voice of rests.
         *
         * @param barNr The number which the bar would have in a score. Can change later.
         * @param timeSignature The time signature of the bar, which will be filled with rests.
         * @return The "empty" bar instance full of rests.
         */
        fun makeEmpty(barNr: Int, timeSignature: TimeSignature): Bar {
            val bar = Bar(barNr, timeSignature, mapOf<Int, MutableList<RhythmicInterval>>())
            bar.addEmptyVoice(1)
            return bar
        }
    }
}