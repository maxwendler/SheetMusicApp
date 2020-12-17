package com.example.sheetmusicapp

import com.example.sheetmusicapp.scoreModel.*
import org.junit.Test
import org.junit.Assert.*
import java.lang.IllegalStateException
import java.lang.reflect.Method

class BarUnitTest {

    @Test
    fun intervalWidthPercent_isCorrect(){

        val fourQuartersBar = Bar(1, TimeSignature(4, 4), mapOf())
        var intervalWidthPercentFun : Method? = null
        for (function in fourQuartersBar.javaClass.declaredMethods){
            if (function.name == "widthPercentOfRhythmicLength"){
                intervalWidthPercentFun = function
                intervalWidthPercentFun.isAccessible = true
            }
        }
        if (intervalWidthPercentFun == null){
            throw IllegalStateException("'appendRest', the function to test, does not exist in the class 'Bar'.")
        }
        else {
            val startUnit = 0

            var widthPercent = intervalWidthPercentFun(fourQuartersBar, RhythmicLength(BasicRhythmicLength.WHOLE), startUnit)
            println("4/4 whole width percent: $widthPercent")
            assertEquals(widthPercent, 90.0)

            widthPercent = intervalWidthPercentFun(fourQuartersBar, RhythmicLength(BasicRhythmicLength.HALF).toggleDotted(), startUnit)
            println("4/4 dotted half width percent: $widthPercent")
            assertEquals(widthPercent, 60.0 + 20.0/3)

            widthPercent = intervalWidthPercentFun(fourQuartersBar, RhythmicLength(BasicRhythmicLength.HALF).toggleTriplet(), startUnit)
            println("4/4 triplet half width percent: $widthPercent")
            assertEquals(widthPercent, 80.0/3 + 10.0/3)

            widthPercent = intervalWidthPercentFun(fourQuartersBar, RhythmicLength(BasicRhythmicLength.HALF), startUnit)
            println("4/4 half width percent: $widthPercent")
            assertEquals(widthPercent, 40.0 + 10.0/3)

            widthPercent = intervalWidthPercentFun(fourQuartersBar, RhythmicLength(BasicRhythmicLength.QUARTER), startUnit)
            println("4/4 quarter width percent: $widthPercent")
            assertEquals(widthPercent, 20.00)

            widthPercent = intervalWidthPercentFun(fourQuartersBar, RhythmicLength(BasicRhythmicLength.QUARTER).toggleTriplet(), startUnit)
            println("4/4 quarter triplet width percent: $widthPercent")
            assertEquals(widthPercent, 80.0/6)
        }

        val sixEighthsBar = Bar(1, TimeSignature(6, 8), mapOf())
        intervalWidthPercentFun = null
        for (function in sixEighthsBar.javaClass.declaredMethods){
            if (function.name == "widthPercentOfRhythmicLength"){
                intervalWidthPercentFun = function
                intervalWidthPercentFun.isAccessible = true
            }
        }
        if (intervalWidthPercentFun == null){
            throw IllegalStateException("'appendRest', the function to test, does not exist in the class 'Bar'.")
        }
        else {
            val startUnit = 0

            var widthPercent = intervalWidthPercentFun(sixEighthsBar, RhythmicLength(BasicRhythmicLength.QUARTER), startUnit)
            println("6/8 quarter width percent: $widthPercent")
            assertEquals(widthPercent, 80.0/3)

            widthPercent = intervalWidthPercentFun(sixEighthsBar, RhythmicLength(BasicRhythmicLength.QUARTER).toggleDotted(), startUnit)
            println("6/8 dotted quarter width percent: $widthPercent")
            assertEquals(widthPercent, 40.0)

            widthPercent = intervalWidthPercentFun(sixEighthsBar, RhythmicLength(BasicRhythmicLength.HALF), startUnit)
            println("6/8 half width percent: $widthPercent")
            assertEquals(widthPercent, 80 * 2.0/3 + 10.0)
        }

        val fiveEighthsBar = Bar(1, TimeSignature(5, 8), mapOf())
        intervalWidthPercentFun = null
        for (function in fiveEighthsBar.javaClass.declaredMethods){
            if (function.name == "widthPercentOfRhythmicLength"){
                intervalWidthPercentFun = function
                intervalWidthPercentFun.isAccessible = true
            }
        }
        if (intervalWidthPercentFun == null){
            throw IllegalStateException("'appendRest', the function to test, does not exist in the class 'Bar'.")
        }
        else {
            val startUnit = 0

            var widthPercent = intervalWidthPercentFun(fiveEighthsBar, RhythmicLength(BasicRhythmicLength.QUARTER), startUnit)
            println("5/8 quarter width percent: $widthPercent")
            assertEquals(widthPercent, 90.0 * 0.4)

            widthPercent = intervalWidthPercentFun(fiveEighthsBar, RhythmicLength(BasicRhythmicLength.QUARTER).toggleDotted(), startUnit)
            println("5/8 dotted quarter width percent: $widthPercent")
            assertEquals(widthPercent, 90.0 * 0.6)

            widthPercent = intervalWidthPercentFun(fiveEighthsBar, RhythmicLength(BasicRhythmicLength.HALF), startUnit)
            println("5/8 half width percent: $widthPercent")
            assertEquals(widthPercent, 90.0 * 0.8)
        }


    }
}