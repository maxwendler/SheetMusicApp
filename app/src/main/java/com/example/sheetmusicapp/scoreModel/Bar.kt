package com.example.sheetmusicapp.scoreModel

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

    companion object {

        /**
         * Creates an "empty" bar of a certain time signature, which contains only one complete voice of rests.
         *
         * @param barNr The number which the bar would have in a score. Can change later.
         * @param timeSignature The time signature of the bar, which will be filled with rests.
         * @return The "empty" bar instance full of rests.
         * @throws IllegalStateException When filling the bar went wrong, and no rhythmic length could eventually fill the bar of time signature.
         * Will happen because of missing rhytmic lengths in restLengthsForEmpty bars or not supported time signatures.
         */
        fun makeEmpty(barNr: Int, timeSignature: TimeSignature): Bar {

            var remainingBarUnits = timeSignature.units
            val voiceOfRests = mutableListOf<RhythmicInterval>()

            // Add rests the voice until no units remain in the bar.
            while (remainingBarUnits > 0){
                for (i in 0..restLengthsForEmptyBars.size){
                    val currentRestLength = restLengthsForEmptyBars[i]
                    if (currentRestLength.lengthInUnits <= remainingBarUnits){
                        voiceOfRests.add(RhythmicInterval.makeRest(currentRestLength))
                        remainingBarUnits -= currentRestLength.lengthInUnits
                        break
                    }
                    if (i == restLengthsForEmptyBars.size){
                        throw IllegalStateException("No rest length fits the rest of the bar.")
                    }
                }
            }
            return Bar(barNr, timeSignature, mapOf(1 to voiceOfRests))
        }
    }
}