package com.example.sheetmusicapp.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import java.lang.IllegalStateException

/**
 * Class extending view containing visualization of a bar without any elements, i.e. only
 * vertical and horizontal lines. Dimensions determined by params width and height, stroke width
 * by [strokeWidth].
 */
class BarDrawableView(context: Context, width: Int, height: Int, private val strokeWidth: Int) : View(context) {

    // reused for drawing bar lines from barPath
    private val paint = Paint()

    init {
        if (strokeWidth % 2 == 1){
            throw IllegalStateException("strokeWidth must be dividable by two!")
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth.toFloat()
    }

    // Create bar path according to parameter dimensions.
    private val barPath : Path = run {
        val widthMinusStrokeWidth = (width - strokeWidth).toFloat()
        val heightMinusStrokeWidth = (height - strokeWidth).toFloat()
        val leftAndTopMargin = (strokeWidth / 2).toFloat()

        val path = Path()
        path.addRect(RectF(leftAndTopMargin, leftAndTopMargin, widthMinusStrokeWidth, heightMinusStrokeWidth), Path.Direction.CW)

        var topMargin : Float = leftAndTopMargin
        val quarterHeight : Float = heightMinusStrokeWidth / 4f
        for (i in 1..3){
            topMargin += quarterHeight
            path.moveTo(leftAndTopMargin, topMargin)
            path.lineTo(leftAndTopMargin + widthMinusStrokeWidth, topMargin)
        }

        Path(path)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(barPath, paint)
    }

}