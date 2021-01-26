package com.example.sheetmusicapp.scoreModel

import java.io.Serializable
import java.lang.IllegalStateException

/**
 * Class for instances of immutable time signatures. Can be mapped to different associated properties via member functions.
 * The app only supports denominators 2, 4 and 8. For the denominator 8, only numerators from 1 to 12 are supported.
 *
 * @property numerator
 * @property denominator
 * @property numberOfSubGroups in which a bar of this time signature should be divided into. Is in range 1 to 5.
 * @property subGroupEndUnits List of last unit of each sub group. For calculation of subgroup an interval starts and ends in,
 * via [calculateSubGroup] and [calculateLastCoveredSubGroup].
 * @property units Amount of smallest rhythmical lengths this app uses (1/48) in a bar of this time signature.
 * @throws IllegalArgumentException When an instance is constructed with properties of a time signature not supported.
 * @author Max Wendler
 */
class TimeSignature(val numerator: Int, val denominator: Int) : Serializable {

    var numberOfSubGroups : Int = 1
        private set

    init {
        // ERROR DETECTION
        if (denominator !in listOf(2, 4, 8)){
            throw IllegalArgumentException("Only denominators 2, 4 and 8 are supported.")
        }
        else {
            if (denominator == 8 && (numerator < 1 || numerator > 12)) {
                throw java.lang.IllegalArgumentException("For the denominator 8 only have a numerators from 1 to 12 are supported.")
            }
        }

        // set numberOfSubGroups
        when (denominator){
            2 -> numberOfSubGroups = numerator * denominator
            4 -> numberOfSubGroups = numerator
            8 -> {
                // equal subdivisions for 4, 6, 8, 9, 10 and 12/8 signatures
                // for the rest, see calculate sub group end units
                when (numerator){
                    in 1..3 -> numberOfSubGroups = 1
                    in 4..6 -> numberOfSubGroups = 2
                    in listOf(8, 12) -> numberOfSubGroups = 4
                    in listOf(7, 9) -> numberOfSubGroups = 3
                    in 10..11 -> numberOfSubGroups = 5
                }
            }
            else -> {
                throw IllegalStateException("This line should not be reached.")
            }
        }
    }

    val units: Int = numerator * (48 / denominator)
    val subGroupEndUnits : List<Int> = calculateSubGroupEndUnits()

    fun equals(other: TimeSignature): Boolean{
        return this.numerator == other.numerator && this.denominator == other.denominator
    }

    /**
     * For setting [subGroupEndUnits]. While denominator 2 signatures get divided into half sub groups,
     * and denominator 4 signatures into quarter subgroups, there are different rules for denominator 8 ones (see code).
     */
    private fun calculateSubGroupEndUnits(): List<Int>{
        val subGroupEndUnits = mutableListOf<Int>()
        var currentEndUnit = 0
        // calculates end units of all but last sub group
        when (denominator){
            2 -> {
                // end unit of each half
                for (i in 1 until numberOfSubGroups){
                    currentEndUnit += RhythmicLength(BasicRhythmicLength.HALF).lengthInUnits
                    subGroupEndUnits.add(currentEndUnit)
                }
            }
            4 -> {
                // end unit of each quarter
                for (i in 1 until numberOfSubGroups){
                    currentEndUnit += RhythmicLength(BasicRhythmicLength.QUARTER).lengthInUnits
                    subGroupEndUnits.add(currentEndUnit)
                }
            }
            8 -> {
                when (numerator) {
                    // 6, 9, and 12 => end unit of each dotted quarter;
                    // 5/8 = (3 + 2)/8 => end unit after a dotted quarter & bar end
                    in listOf(5, 6, 9, 12) -> {
                        for (i in 1 until numberOfSubGroups) {
                            currentEndUnit += RhythmicLength(BasicRhythmicLength.QUARTER, LengthModifier.DOTTED).lengthInUnits
                            subGroupEndUnits.add(currentEndUnit)
                        }
                    }
                    // 4, 8, 10 => end unit of each quarter
                    // 11/8 = (5*2 + 1)/8 => end unit of each full quarter & bar end
                    in listOf(4, 8, 10, 11) -> {
                        for (i in 1 until numberOfSubGroups){
                            currentEndUnit += RhythmicLength(BasicRhythmicLength.QUARTER).lengthInUnits
                            subGroupEndUnits.add(currentEndUnit)
                        }
                    }
                    // 7/8 = (3 + 2 + 2)/8 => end unit after a dotted quarter, then an unmodified quarter & then bar end
                    7 -> {
                        currentEndUnit += RhythmicLength(BasicRhythmicLength.QUARTER, LengthModifier.DOTTED).lengthInUnits
                        subGroupEndUnits.add(currentEndUnit)
                        currentEndUnit += RhythmicLength(BasicRhythmicLength.QUARTER).lengthInUnits
                        subGroupEndUnits.add(currentEndUnit)
                    }
                }
            }
        }
        // end unit of last sub group = last unit of time signature
        subGroupEndUnits.add(units)
        return subGroupEndUnits
    }

    /**
     * Returns index of sub group in which an interval starts, to which it should belong (see [Voice] for interval-subgroup assignment.)
     * @param interval A [RhythmicInterval] that should not exceed the time signature's length in units.
     * @throws IllegalArgumentException When the given interval's end unit exceeds the units of the time signature instance.
     */
    fun calculateSubGroup(interval: RhythmicInterval) : Int{
        if (interval.endUnit > units){
            throw java.lang.IllegalArgumentException("The given interval exceeds the time signature's units.")
        }

        // Find index of sub group of which the end unit is larger than or equal to the interval's start unit.
        else {
            for (i in subGroupEndUnits.indices){
                if (interval.startUnit <= subGroupEndUnits[i]){
                    return i
                }
            }
        }

        // time signature is always appended as end unit of the last subgroup (subGroupEndUnits should not be empty) -> should not happen
        return 0
    }

    /**
     * Returns index of sub group in which the given end units lies. Used in combination with [calculateSubGroup] to
     * calculate over how many sub groups an interval stretches.
     * @param endUnit An integer which should not exceed the time signature's length in units.
     * @throws IllegalArgumentException When the given interval's end unit exceeds the units of the time signature instance.
     */
    fun calculateLastCoveredSubGroup(endUnit: Int) : Int {
        if (endUnit > units){
            throw java.lang.IllegalArgumentException("The given end unit exceeds the time signature's units.")
        }

        // Find index of sub group of which the end unit is larger than or equal to the interval's end unit.
        for (i in subGroupEndUnits.indices){
            if (endUnit <= subGroupEndUnits[i]){
                return i
            }
        }
        // time signature is always appended as end unit of the last subgroup -> should not happen
        return 0
    }
}