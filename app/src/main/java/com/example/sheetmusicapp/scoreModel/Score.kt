package com.example.sheetmusicapp.scoreModel

/**
 * Instances are drum scores consisting of a list of bars and a title.
 *
 * @property title Title the user may have given the score. "Title" as default. Can be changed later.
 * @property barList The list of bars of the score. The referenced instance can be changed, the instance cannot.
 * @property length The length of the instance in bars.
 * @constructor Creates a score with the given title and the given list of bars.
 * @author Max Wendler
 */
class Score(bars: List<Bar>, var title: String = "Title") {

    val barList = bars.toMutableList()
    var length = bars.size

    companion object {

        /**
         * Creates an empty score with a given number of bars of the given time signature. A title may be specified, but
         * is "Title" as default.
         *
         * @param bars The number of bars to be in the score.
         * @param timeSignature The time signature all bars in the score will have.
         * @param title The title of the score. "Title" if none is specified.
         */
        fun makeEmpty(bars: Int, timeSignature: TimeSignature, title: String = "Title"): Score {
            val barList = mutableListOf<Bar>()
            for (i in 1..bars){
                barList.add(Bar.makeEmpty(barNr = i, timeSignature = timeSignature))
            }
            return Score(barList, title)
        }

    }
}