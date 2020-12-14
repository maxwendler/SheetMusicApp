package com.example.sheetmusicapp.scoreModel

import java.lang.IllegalStateException

/**
 * Class for instances of immutable time signatures. Can be mapped to different associated properties via member functions.
 * The app only supports denominators 2, 4 and 8. For the denominator 8, only numerators from 1 to 12 are supported.
 *
 * @property numerator
 * @property denominator
 * @throws IllegalArgumentException When an instance is constructed with properties of a time signature not supported.
 * @author Max Wendler
 */
class TimeSignature(val numerator: Int, val denominator: Int) {

    init {
        if (denominator !in listOf(2, 4, 8)){
            throw IllegalArgumentException("Only denominators 2, 4 and 8 are supported.")
        }
        else {
            if (denominator == 8 && (numerator < 1 || numerator > 12)) {
                throw java.lang.IllegalArgumentException("For the denominator 8 only have a numerators from 1 to 12 are supported.")
            }
        }
    }

    /**
     * @return The rhythmic length of one bar of this time signature in the smallest units used in the app (48 per whole note).
     */
    fun toUnits() : Int {
        return numerator * (48 / denominator)
    }

    /**
     * Maps the time signature to a number of subgroups a bar of this time signature should be divided into visually.
     *
     * @return The according number of subgroups, or null if the bar should have no subgroups.
     */
    fun toNumberOfSubGroups() : Int? {
        when (denominator){
            2 -> return denominator * numerator
            4 -> return numerator
            8 -> {
                if (numerator < 1 || numerator > 12){
                    throw IllegalStateException()
                }
                when (numerator){
                    in listOf(1,2,3) -> return 1
                    in listOf(4,6) -> return 2
                    else -> return null
                }
            }
            else -> {
                throw IllegalStateException("This line should not be reached.")
            }
        }
    }
}