package com.example.sheetmusicapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sheetmusicapp.scoreModel.Score

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val exampleScore = Score.makeEmpty(bars = 32, timeSignature =  Pair(4, 4))
    }
}