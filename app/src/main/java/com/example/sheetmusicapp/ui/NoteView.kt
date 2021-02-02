package com.example.sheetmusicapp.ui

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView
import com.example.sheetmusicapp.scoreModel.BasicRhythmicLength
import com.example.sheetmusicapp.scoreModel.NoteHeadType

/**
 * AppCompatImageView extension for note, note head and rest image views carrying the [NoteHeadType]
 * and [BasicRhythmicLength] to reduce number of parameters in [BarVisLayout] functions.
 */
class NoteView(context: Context, val headType: NoteHeadType, val basicLength: BasicRhythmicLength) : AppCompatImageView(context)