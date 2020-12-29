package com.example.sheetmusicapp.scoreModel

import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

// List of rhythmic lengths which can be rests to fill a new bar with rests.
// Because the time signatures of bars can only have halfs, quarters or eighths as denominators, only certain
// lengths are required here.
val restLengthsForEmptyBars : List<RhythmicLength> = listOf(
        RhythmicLength(BasicRhythmicLength.WHOLE).toggleDotted(),
        RhythmicLength(BasicRhythmicLength.WHOLE),
        RhythmicLength(BasicRhythmicLength.HALF).toggleDotted(),
        RhythmicLength(BasicRhythmicLength.HALF),
        RhythmicLength(BasicRhythmicLength.QUARTER).toggleDotted(),
        RhythmicLength(BasicRhythmicLength.QUARTER),
        RhythmicLength(BasicRhythmicLength.EIGHTH)
)

// Constant specifying the combined percentage of width of the padding elements on the right and the left of a UI bar.
const val BAR_LEFTRIGHT_PADDING_PERCENT = 10
// Constant specifying the combined percentage of width of the padding elements between the subgroups of a UI bar.
const val BAR_SUBGROUP_PADDING_PERCENT = 10
// Constant specifying the combined percentage of width of all notes, depending on the padding fractions.
const val BAR_NOTES_PERCENT = 100 - BAR_LEFTRIGHT_PADDING_PERCENT - BAR_SUBGROUP_PADDING_PERCENT

/**
 * Bar instances are the elements of a drum score. They have a certain time signature and consist of
 * voices out of different rhythmic intervals with their respective notes.
 *
 * @property barNr The number of the bar in a score. Can change after creation.
 * @property timeSignature The time signature of the bar. Can change after creation.
 * @property voices Map of voices of rhythmic intervals in this bar.
 * @constructor Creates a bar which contains the given voices, with the given time signature and bar number.
 * @author Max Wendler
 */
class Bar(var barNr: Int, var timeSignature: TimeSignature, initVoices: Map<Int,MutableList<RhythmicInterval>>) {

    val voices = initVoices.toMutableMap()

    /**
     * Calculates the percentage of width of an UI bar an interval UI object of [RhythmicLength] [length], based on its [startUnit] and the bar's [timeSignature].
     *
     * Private to ensure that interfaces of [Bar] can implement interval calculation which always
     * precomputes the UI [RhythmicInterval.widthPercent] of RhytmicInterval instances.
     */
    private fun widthPercentOfRhythmicLength(length: RhythmicLength, startUnit: Int) : Double {

        if (startUnit > timeSignature.units) {
            throw IllegalArgumentException("The start unit exceeds the length of the bar in units.")
        }

        val endUnit = startUnit + length.lengthInUnits
        if (endUnit > timeSignature.units){
            throw IllegalArgumentException("The end unit exceeds the length of the bar in units.")
        }

        var subgroupPaddingWidthPercent : Double = 0.0
        val noteWidthPercent : Double
        val numberOfSubgroups = timeSignature.numberOfSubgroups
        val unitsPerSubgroup = timeSignature.subGroupUnits

        // Bars of some kinds of time signatures don't have subgroups, so there's no need for subgroup padding.
        if (unitsPerSubgroup != null && numberOfSubgroups != null){

            // Calculate difference in subgroups by cutting off remainders when mapping each subgroup to an interval 0.0 to 0.999.., 1.0 to 1.999.., ... (division by unitsPerSubgroup)
            val subgroupDifference = ((endUnit - 1)/ unitsPerSubgroup) - ((startUnit - 1)/ unitsPerSubgroup)
            // There's one less padding element than there are subgroups.
            subgroupPaddingWidthPercent = ((subgroupDifference / (numberOfSubgroups - 1).toDouble()) * BAR_SUBGROUP_PADDING_PERCENT)
            // Padding-independent width is specified by the contained fraction of a bar's units.
            noteWidthPercent = ((endUnit - startUnit) * BAR_NOTES_PERCENT / timeSignature.units.toDouble())
        }
        else {
            // Padding-independent width is specified by the contained fraction of a bar's units.
            noteWidthPercent = (endUnit - startUnit) * (BAR_NOTES_PERCENT + BAR_SUBGROUP_PADDING_PERCENT) / timeSignature.units.toDouble()
        }

        return noteWidthPercent + subgroupPaddingWidthPercent
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
            startUnit = lastInterval.startUnit + lastInterval.length.lengthInUnits
        }

        if (startUnit > timeSignature.units) {
            throw IllegalArgumentException("The start unit exceeds the length of the bar in units.")
        }

        val endUnit = startUnit + length.lengthInUnits
        if (endUnit > timeSignature.units){
            throw IllegalArgumentException("The end unit exceeds the length of the bar in units.")
        }

        // Create and add interval with width from widthPercentOfRhythmicLength.
        val interval = RhythmicInterval(length, initNoteHeads, startUnit, widthPercentOfRhythmicLength(length, startUnit))
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
    private fun addEmptyVoice(voice : Int){

        if (voices[voice] != null){
            throw IllegalArgumentException("The given voice does already exist and should not be overwritten.")
        }

        val newVoiceIntervals = mutableListOf<RhythmicInterval>()
        var remainingBarUnits = timeSignature.units

        // Add rests the voice until no units remain in the bar.
        while (remainingBarUnits > 0){
            for (i in 0..restLengthsForEmptyBars.size){
                val currentRestLength = restLengthsForEmptyBars[i]
                if (currentRestLength.lengthInUnits <= remainingBarUnits){
                    appendRest(newVoiceIntervals, currentRestLength)
                    remainingBarUnits -= currentRestLength.lengthInUnits
                    break
                }
                if (i == restLengthsForEmptyBars.size){
                    throw IllegalStateException("No rest length fits the rest of the bar.")
                }
            }
        }

        voices[voice] = newVoiceIntervals
    }

    // Full implementation in next commit.
    fun addNote(voice: Int, length: RhythmicLength, type: NoteHeadType, height: Int, intervalIdx: Int){
        if (voices[voice] == null){
            addEmptyVoice(voice)
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