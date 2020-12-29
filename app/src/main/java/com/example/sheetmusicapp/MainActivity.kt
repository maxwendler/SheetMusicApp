package com.example.sheetmusicapp

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout
import com.devs.vectorchildfinder.VectorChildFinder
import com.example.sheetmusicapp.scoreModel.Score
import com.example.sheetmusicapp.scoreModel.TimeSignature

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set callback for resetting stroke widths of bar drawable after scaling.
        val mainConstraintLayout = findViewById<ConstraintLayout>(R.id.main)
        mainConstraintLayout.doOnLayout {
            scaleBarLineStrokeWidth()
        }

        val exampleScore = Score.makeEmpty(bars = 32, timeSignature =  TimeSignature(4, 4))
    }

    /**
     * Sets the widths of all strokes of the bar drawable to be equal again, after they changed due to scaling.
     */
    fun scaleBarLineStrokeWidth(){
        val barImageView = findViewById<ImageView>(R.id.barImageView)
        val barDrawableChildFinder = VectorChildFinder(this, R.drawable.ic_bar, barImageView)
        val leftVertical = barDrawableChildFinder.findPathByName("leftVertical")
        val rightVertical = barDrawableChildFinder.findPathByName("rightVertical")
        val exampleHorizontal = barDrawableChildFinder.findPathByName("exampleHorizontal")
        leftVertical.strokeWidth = exampleHorizontal.strokeWidth
        rightVertical.strokeWidth = exampleHorizontal.strokeWidth
    }
}