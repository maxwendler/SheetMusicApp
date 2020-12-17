package com.example.sheetmusicapp.scoreModel

import java.lang.IllegalStateException

/**
* Enumeration of the basic types of rhythmic lengths supported in the app, regardless of usage as triplet or dotted notes.
* @author Max Wendler
* */
enum class BasicRhythmicLength{
    SIXTEENTH,
    EIGHTH,
    QUARTER,
    HALF,
    WHOLE
}

/**
 * Mapping of the basic types of rhythmic lengths to their length in the smallest rhytmic unit: (Int) 1 == 1/48 rhythmically.
 * @author Max Wendler
 */
val basicRhythmicLengthsInUnits: Map<BasicRhythmicLength, Int> = mapOf(
        BasicRhythmicLength.SIXTEENTH to 3,
        BasicRhythmicLength.EIGHTH to 6,
        BasicRhythmicLength.QUARTER to 12,
        BasicRhythmicLength.HALF to 24,
        BasicRhythmicLength.WHOLE to 48
)

/**
 * Class of which the instances can be any kind of supported rhythmic length.
 *
 * @property basicLength Basic length type on which the potentially modified length instance is based on.
 * @property isDotted Boolean representing if the length is modified by a dot (length * 1.5).
 * @property isTriplet Boolean representing if the note is a triplet note (length * 2/3).
 * @property lengthInUnits Length of the instance in units, where 1 unit = 1/48 rhythmically.
 * @constructor Creates an unmodified (no triplet / dotted note) instance with the length of the basic length type.
 * @throws IllegalStateException When a given BasicRhytmicLength instance has no mapped value in units in basicRhytmicLengthsInUnits.
 * @author Max Wendler
 */
class RhythmicLength(var basicLength: BasicRhythmicLength) {

    var isDotted = false
    var isTriplet = false

    var lengthInUnits : Int = basicRhythmicLengthsInUnits[basicLength]
            ?: throw IllegalStateException("Given basic length parameter has no mapped length in units in basicRhyhtmicLengthsInUnits")

    /**
     * Makes the length instance a triplet note, if it isn't, and vice versa.
     *
     * @return Returns the modified instance.
     */
    fun toggleTriplet(): RhythmicLength {
        if (isTriplet) lengthInUnits = (lengthInUnits * 1.5).toInt()
        else lengthInUnits = (lengthInUnits * 2 /3.0).toInt()

        isTriplet = !isTriplet

        return this
    }

    /**
     * Makes the length instance a dotted noted, if it isn't, and vice versa.
     *
     * @return Returns the modified instance.
     * @throws IllegalStateException When dotted (triplet) sixteenth notes shall be created. They are not supported by the app.
     */
    fun toggleDotted() : RhythmicLength {
        if (basicLength == BasicRhythmicLength.SIXTEENTH) {
            throw IllegalStateException("Dotted (triplet) sixteenth notes should be disabled.")
        } else {
            if (isDotted) lengthInUnits = (lengthInUnits * 2/3.0).toInt()
            else lengthInUnits = (lengthInUnits * 1.5).toInt()

            isDotted = !isDotted
        }
        return this
    }
}