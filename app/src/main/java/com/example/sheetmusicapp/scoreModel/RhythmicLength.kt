package com.example.sheetmusicapp.scoreModel

import java.lang.IllegalArgumentException
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

enum class LengthModifier{
    DOTTED,
    TRIPLET,
    NONE
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
 * @property lengthModifier Type of length modifier (dotted or triplet) the instance has.
 * @property lengthInUnits Length of the instance in units, where 1 unit = 1/48 rhythmically.
 * @throws IllegalStateException When a given BasicRhytmicLength instance has no mapped value in units in basicRhytmicLengthsInUnits.
 * @author Max Wendler
 */
class RhythmicLength(initBasicLength: BasicRhythmicLength, initLengthModifier: LengthModifier = LengthModifier.NONE) {

    var basicLength = initBasicLength
        private set

    var lengthModifier = initLengthModifier
        private set

    // initialize length in unit to basic length; modification in init block below
    var lengthInUnits : Int = basicRhythmicLengthsInUnits[basicLength]
            ?: throw IllegalStateException("Given basic length parameter has no mapped length in units in basicRhyhtmicLengthsInUnits")
        private set

    init {
        // Modification of length in units, if a non-NONE modifier is given.
        when (lengthModifier){
            LengthModifier.DOTTED -> {
                if (basicLength == BasicRhythmicLength.SIXTEENTH){
                    throw IllegalArgumentException("Dotted sixteenth notes should be disabled.")
                }
                lengthInUnits = (lengthInUnits * 1.5).toInt()
            }
            LengthModifier.TRIPLET -> {
                lengthInUnits = (lengthInUnits * 2/3.toDouble()).toInt()
            }
        }
    }

    /**
     * Reconfigures the instance to represent another rhythmic length and returns it.
     *
     * @throws IllegalArgumentException When no actual change would occur with the given parameters. This function should not
     * callable by an UI event in this way.
     * @throws IllegalStateException When a [BasicRhythmicLength] was used that's not mapped in [basicRhythmicLengthsInUnits].
     * @return [RhythmicLength]
     */
    fun change(newLength: RhythmicLength) : RhythmicLength {

        val newBasicLength = newLength.basicLength
        val newLengthMod = newLength.lengthModifier

        // Setting a new length if necessary & fault tolerance check against params that don't change the state.
        if (newBasicLength != basicLength){
            basicLength = newBasicLength
        }
        // length does not change
        else {
            // nothing changes
            if (newLengthMod == lengthModifier){
                throw IllegalArgumentException("This function should not be called when no actual change occurs.")
            }
        }

        // Reset lengthInUnits to (old or new) basic length (so modification does not need previous modifier state.)
        lengthInUnits = basicRhythmicLengthsInUnits[basicLength]
                ?: throw IllegalStateException("Given basic length parameter has no mapped length in units in basicRhyhtmicLengthsInUnits")

        // Set (old or new) length modifier and calculate new length in units.
        // A new length in units is only calculated when actual changes occur, because
        // "Both params == before state" is checked by IllegalArgumentException block above.
        lengthModifier = newLengthMod
        when (lengthModifier){
            LengthModifier.DOTTED -> {
                if (basicLength == BasicRhythmicLength.SIXTEENTH){
                    throw IllegalArgumentException("Dotted sixteenth notes should be disabled.")
                }
                lengthInUnits = (lengthInUnits * 1.5).toInt()
            }

            LengthModifier.TRIPLET -> {
                lengthInUnits = (lengthInUnits * 2/3.toDouble()).toInt()
            }
        }

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
fun lengthsFromUnitLengthAsc(units: Int) : MutableList<RhythmicLength>{

    // List of all rhythmic length instances that can exist in the app.
    val allRhythmicLengthsOrderedDesc : List<RhythmicLength> = listOf(
            RhythmicLength(BasicRhythmicLength.WHOLE, LengthModifier.DOTTED),       // 72 units
            RhythmicLength(BasicRhythmicLength.WHOLE),                              // 48
            RhythmicLength(BasicRhythmicLength.HALF, LengthModifier.DOTTED),        // 36
            RhythmicLength(BasicRhythmicLength.WHOLE, LengthModifier.TRIPLET),      // 32
            RhythmicLength(BasicRhythmicLength.HALF),                               // 24
            RhythmicLength(BasicRhythmicLength.QUARTER, LengthModifier.DOTTED),     // 18
            RhythmicLength(BasicRhythmicLength.HALF, LengthModifier.TRIPLET),       // 16
            RhythmicLength(BasicRhythmicLength.QUARTER),                            // 12
            RhythmicLength(BasicRhythmicLength.EIGHTH, LengthModifier.DOTTED),      // 9
            RhythmicLength(BasicRhythmicLength.QUARTER, LengthModifier.TRIPLET),    // 8
            RhythmicLength(BasicRhythmicLength.EIGHTH),                             // 6
            RhythmicLength(BasicRhythmicLength.EIGHTH, LengthModifier.TRIPLET),     // 4
            RhythmicLength(BasicRhythmicLength.SIXTEENTH),                          // 3
            RhythmicLength(BasicRhythmicLength.SIXTEENTH, LengthModifier.TRIPLET)   // 2
    )

    val rhythmicLengths = mutableListOf<RhythmicLength>()
    var remainingUnits = units
    var idxOfLastFitting = 0
    while (remainingUnits > 0){
        for (i in idxOfLastFitting..allRhythmicLengthsOrderedDesc.size){
            if (i == allRhythmicLengthsOrderedDesc.size){
                throw IllegalStateException("No rhythmic length fits the remaining units.")
            }
            val currentRhythmicLength = allRhythmicLengthsOrderedDesc[i]
            if (currentRhythmicLength.lengthInUnits <= remainingUnits){
                rhythmicLengths.add(currentRhythmicLength)
                remainingUnits -= currentRhythmicLength.lengthInUnits
                idxOfLastFitting = i
                break
            }
        }
    }

    return rhythmicLengths.asReversed()
}