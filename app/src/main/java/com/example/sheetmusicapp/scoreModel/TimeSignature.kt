package com.example.sheetmusicapp.scoreModel

import java.lang.IllegalStateException

/**
 * Class for instances of immutable time signatures. Can be mapped to different associated properties via member functions.
 * The app only supports denominators 2, 4 and 8. For the denominator 8, only numerators from 1 to 12 are supported.
 *
 * @property numerator
 * @property denominator
 * @property numberOfSubgroups in which a bar of this time signature should be divided into. Can be 1, 2 or null.
 * @property units Amount of smallest rhytmical lengths this app uses (1/48) in a bar of this time signature.
 * @throws IllegalArgumentException When an instance is constructed with properties of a time signature not supported.
 * @author Max Wendler
 */
class TimeSignature(val numerator: Int, val denominator: Int) {

    val numberOfSubgroups : Int?
    init {
        if (denominator !in listOf(2, 4, 8)){
            throw IllegalArgumentException("Only denominators 2, 4 and 8 are supported.")
        }
        else {
            if (denominator == 8 && (numerator < 1 || numerator > 12)) {
                throw java.lang.IllegalArgumentException("For the denominator 8 only have a numerators from 1 to 12 are supported.")
            }
        }

        when (denominator){
            2 -> numberOfSubgroups = numerator * denominator
            4 -> numberOfSubgroups = numerator
            8 -> {
                when (numerator){
                    in listOf(4,6) -> numberOfSubgroups = 2
                    else -> numberOfSubgroups = null
                }
            }
            else -> {
                throw IllegalStateException("This line should not be reached.")
            }
        }
    }

    val units: Int = numerator * (48 / denominator)
    val subGroupUnits : Int? = if (numberOfSubgroups != null) units / numberOfSubgroups else null
}