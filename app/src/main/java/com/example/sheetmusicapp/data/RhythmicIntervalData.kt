package com.example.sheetmusicapp.data

import com.example.sheetmusicapp.scoreModel.NoteHeadType


data class RhythmicIntervalData(val length: RhythmicLengthData,
                                val noteHeads: Map<Int, NoteHeadType>,
                                val startUnit: Int, val widthPercent: Double)

