package com.example.sheetmusicapp.data

data class BarData(val barNr: Int, val timeSignature: TimeSignatureData,
                   val voices: Map<Int, MutableList<RhythmicIntervalData>>)
