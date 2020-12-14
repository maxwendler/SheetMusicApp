package com.example.sheetmusicapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sheetmusicapp.scoreModel.Score
import com.example.sheetmusicapp.scoreModel.TimeSignature

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val exampleScore = Score.makeEmpty(bars = 32, timeSignature =  TimeSignature(4, 4))
    }
}