package com.example.sheetmusicapp.ui

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView
import com.example.sheetmusicapp.scoreModel.BasicRhythmicLength
import com.example.sheetmusicapp.scoreModel.NoteHeadType

class NoteView(context: Context, val headType: NoteHeadType, val basicLength: BasicRhythmicLength) : AppCompatImageView(context)