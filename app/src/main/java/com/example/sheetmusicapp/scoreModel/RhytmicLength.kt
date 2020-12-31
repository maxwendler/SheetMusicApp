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
 * List of all rhythmic length instances that can exist in the app.
 * Currently only used in [lengthsFromUnitLength].
 */
val allRhythmicLengthsOrderedDesc : List<RhythmicLength> = listOf(
    RhythmicLength(BasicRhythmicLength.WHOLE).toggleDotted(),       // 72 units
    RhythmicLength(BasicRhythmicLength.WHOLE),                      // 48
    RhythmicLength(BasicRhythmicLength.HALF).toggleDotted(),        // 36
    RhythmicLength(BasicRhythmicLength.WHOLE).toggleTriplet(),      // 32
    RhythmicLength(BasicRhythmicLength.HALF),                       // 24
    RhythmicLength(BasicRhythmicLength.QUARTER).toggleDotted(),     // 18
    RhythmicLength(BasicRhythmicLength.HALF).toggleTriplet(),       // 16
    RhythmicLength(BasicRhythmicLength.QUARTER),                    // 12
    RhythmicLength(BasicRhythmicLength.EIGHTH).toggleDotted(),      // 9
    RhythmicLength(BasicRhythmicLength.QUARTER).toggleTriplet(),    // 8
    RhythmicLength(BasicRhythmicLength.EIGHTH),                     // 6
    RhythmicLength(BasicRhythmicLength.EIGHTH).toggleTriplet(),     // 4
    RhythmicLength(BasicRhythmicLength.SIXTEENTH),                  // 3
    RhythmicLength(BasicRhythmicLength.SIXTEENTH).toggleTriplet()   // 2
)

/**
 * Class of which the instances can be any kind of supported rhythmic length.
 *
 * @property basicLength Basic length type on which the potentially modified length instance is based on.
 * @property isDotted Boolean representing if the length is modified by a dot (length * 1.5). Can only be (un)set via [toggleDotted].
 * @property isTriplet Boolean representing if the note is a triplet note (length * 2/3). Can only be (un)set via [toggleTriplet]
 * @property lengthInUnits Length of the instance in units, where 1 unit = 1/48 rhythmically.
 * @constructor Creates an unmodified (no triplet / dotted note) instance with the length of the basic length type.
 * @throws IllegalStateException When a given BasicRhytmicLength instance has no mapped value in units in basicRhytmicLengthsInUnits.
 * @author Max Wendler
 */
class RhythmicLength(val basicLength: BasicRhythmicLength) {

    var isDotted = false
        private set

    var isTriplet = false
        private set

    var lengthInUnits : Int = basicRhythmicLengthsInUnits[basicLength]
            ?: throw IllegalStateException("Given basic length parameter has no mapped length in units in basicRhyhtmicLengthsInUnits")
        private set

    /**
     * Makes the length instance a triplet note, if it isn't, and vice versa.
     *
     * @return Returns the modified instance.
     */
    fun toggleTriplet(): RhythmicLength {
        if (isDotted){
            throw IllegalStateException("Dotted triplet notes should be disabled")
        }

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
        if (isTriplet){
            throw IllegalStateException("Dotted triplet notes should be disabled")
        }

        if (basicLength == BasicRhythmicLength.SIXTEENTH) {
            throw IllegalStateException("Dotted sixteenth notes should be disabled.")
        }

        if (isDotted) lengthInUnits = (lengthInUnits * 2/3.0).toInt()
        else lengthInUnits = (lengthInUnits * 1.5).toInt()
        isDotted = !isDotted

        return this
    }
}

/**
 * Transforms a length given in units into a list / sequence of [RhythmicLength] instances which
 * fill the given length when combined.
 *
 * @param units A rhythmic length in units.
 * @throws IllegalStateException When the given length can't be filled with available RhythmicLength instances for some reason.
 */
fun lengthsFromUnitLength(units: Int) : List<RhythmicLength>{
    val rhythmicLengths = mutableListOf<RhythmicLength>()
    var remainingUnits = units
    var idxOfLastFitting = 0
    while (remainingUnits > 0){
        for (i in idxOfLastFitting..allRhythmicLengthsOrderedDesc.size){
            if (i == allRhythmicLengthsOrderedDesc.size){
                throw IllegalStateException("No rhythmic length fits the remaining units.")
            }
            val currentRhythmicLength = allRhythmicLengthsOrderedDesc[i]
            if (currentRhythmicLength.lengthInUnits < remainingUnits){
                rhythmicLengths.add(currentRhythmicLength)
                remainingUnits -= currentRhythmicLength.lengthInUnits
                idxOfLastFitting = i
                break
            }
        }
    }

    return rhythmicLengths
}