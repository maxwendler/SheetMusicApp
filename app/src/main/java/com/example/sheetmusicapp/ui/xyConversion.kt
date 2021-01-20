package com.example.sheetmusicapp.ui

import com.example.sheetmusicapp.scoreModel.BasicRhythmicLength
import com.example.sheetmusicapp.scoreModel.RhythmicLength

const val eighthRestWidthToHeightRatio = 0.5195
const val eighthNoteWidthToHeightRatio = 0.6063
const val eighthNoteHeightToNoteHeadHeightRatio = 1 / 0.2741

const val quarterRestWidthToHeightRatio = 0.3509
const val quarterNoteWidthToHeightRatio = 0.3516
const val quarterNoteHeightToNoteHeadHeightRatio = 3.6439

const val sixteenthRestWidthToHeightRatio = 0.4546
const val sixteenthNoteHeightToNodeHeadHeightRatio = 3.8655
const val sixteenthNoteWidthToHeightRatio = 0.5684

const val halfRestWidthToHeightRatio = 2.9213
const val halfNoteWidthToHeightRatio = 0.3504
const val halfNoteNoteHeightToNoteHeadHeightRatio = 3.6444

const val wholeNoteHeadWithToNoteHeadHeightRatio = 1.4894

/**
 * Converts given [height] of rest view, which should be derived from the bar height, into width,
 * for different [basicLength]s.
 */
fun restWidthFromHeight(basicLength: BasicRhythmicLength, height: Double) : Double{
    var width = 0.0
    when (basicLength){
        BasicRhythmicLength.SIXTEENTH -> width = sixteenthRestWidthToHeightRatio * height
        BasicRhythmicLength.EIGHTH -> width = eighthRestWidthToHeightRatio * height
        BasicRhythmicLength.QUARTER -> width = quarterRestWidthToHeightRatio * height
        BasicRhythmicLength.HALF, BasicRhythmicLength.WHOLE -> width = halfRestWidthToHeightRatio * height
    }
    return width
}

fun noteWidthFromHeight(basicLength: BasicRhythmicLength, height: Double) : Double{
    var width = 0.0
    when (basicLength){
        BasicRhythmicLength.SIXTEENTH -> width = sixteenthNoteWidthToHeightRatio * height
        BasicRhythmicLength.EIGHTH -> width = eighthNoteWidthToHeightRatio * height
        BasicRhythmicLength.QUARTER -> width =  quarterNoteWidthToHeightRatio * height
        BasicRhythmicLength.HALF -> width = halfNoteWidthToHeightRatio * height
        BasicRhythmicLength.WHOLE -> width = wholeNoteHeadWithToNoteHeadHeightRatio * height
    }
    return width
}

fun noteHeightFromNodeHeadHeight(basicLength: BasicRhythmicLength, noteHeadHeight: Double) : Double {
    var height = 0.0
    when (basicLength){
        BasicRhythmicLength.SIXTEENTH -> height = noteHeadHeight * sixteenthNoteHeightToNodeHeadHeightRatio
        BasicRhythmicLength.EIGHTH -> height = noteHeadHeight * eighthNoteHeightToNoteHeadHeightRatio
        BasicRhythmicLength.QUARTER -> height = noteHeadHeight * quarterNoteHeightToNoteHeadHeightRatio
        BasicRhythmicLength.HALF -> height = noteHeadHeight * halfNoteNoteHeightToNoteHeadHeightRatio
        BasicRhythmicLength.WHOLE -> height = noteHeadHeight
    }
    return height
}