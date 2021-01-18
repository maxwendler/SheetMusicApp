package com.example.sheetmusicapp.ui

import com.example.sheetmusicapp.scoreModel.RhythmicLength

const val eighthWidthToHeightRatio = 0.5195

/**
 * Converts given [height] of rest view, which should be derived from the bar height, into width,
 * for different [restLength]s.
 */
fun restWidthFromHeight(restLength: RhythmicLength, height: Double) : Double{
    // only applies to eighth rests
    return (height * eighthWidthToHeightRatio)
}