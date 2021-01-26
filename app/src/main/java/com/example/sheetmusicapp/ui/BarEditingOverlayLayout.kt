package com.example.sheetmusicapp.ui

import android.content.Context
import android.os.Build
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnLayout
import com.example.sheetmusicapp.R
import java.lang.IllegalArgumentException
import kotlin.IllegalStateException

class BarEditingOverlayLayout(context: Context, val barHeight: Int) : ConstraintLayout(context) {

    val grid : MutableList<List<GridCellView>> = mutableListOf()
    var horizontalMargins : MutableList<Int>? = null
    var highlightedColumnIdx : Int? = null
    var highlightedRowIdx : Int? = null
    lateinit var listener: GridActionUpListener

    init {
        doOnLayout { if (horizontalMargins != null) createOverlay() }
    }

    interface GridActionUpListener {
        fun handleActionUp(intervalIdx: Int, musicHeight: Int)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null){
            if (event.action == MotionEvent.ACTION_DOWN){
                highlightCellColumnAndRow(event.x, event.y)
            }

            if (event.action == MotionEvent.ACTION_MOVE){
                if (event.y >= 0 && event.y <= this.height)
                    highlightCellColumnAndRow(event.x, event.y)
                else if (event.y < 0){
                    highlightCellColumnAndRow(event.x, 1f)
                }
                else if (event.y > this.height){
                    highlightCellColumnAndRow(event.x, height - 1f)
                }
            }

            if (event.action == MotionEvent.ACTION_UP){
                grid.forEach { column ->
                    column.forEach {  cell ->
                        cell.setBackgroundColor(resources.getColor(R.color.black, resources.newTheme()))
                        cell.background.alpha = 0
                    }
                }
                val currentIntervalIdx = highlightedColumnIdx
                val currentMusicHeight = highlightedRowIdx
                if (currentIntervalIdx != null && currentMusicHeight != null){
                    listener.handleActionUp(currentIntervalIdx, currentMusicHeight)
                }
                else {
                    throw IllegalStateException("Highlighted row and column indices must not be null!")
                }
                highlightedColumnIdx = null
                highlightedRowIdx = null
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun findGridCell(x: Int, y: Int): GridCellView?{
        if (grid.isNotEmpty()) {

            var gridCell: GridCellView? = null
            for (column in grid) {
                if (column.isEmpty()) throw IllegalStateException("An empty grid column has been set up!")
                else {
                    if (x < column[0].rightX) {
                        for (cell in column) {
                            if (y < cell.topY) {
                                gridCell = cell
                                break
                            }
                        }
                        break
                    }
                }
            }
            return gridCell
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun highlightCellColumnAndRow(touchDownX: Float, touchDownY: Float){
        val gridCell = findGridCell(touchDownX.toInt(),height - touchDownY.toInt())

        if (gridCell != null){
            highlightedColumnIdx = gridCell.horizontalIdx
            highlightedRowIdx = gridCell.verticalIdx

            gridCell.setBackgroundColor(resources.getColor(R.color.black, resources.newTheme()))
            gridCell.background.alpha = 60

            for (columnIdx in grid.indices){
                if (columnIdx != gridCell.horizontalIdx){
                    grid[columnIdx].forEach { cell ->
                        if (cell.verticalIdx == gridCell.verticalIdx){
                            cell.setBackgroundColor(resources.getColor(R.color.purple_500, resources.newTheme()))
                            cell.background.alpha = 30
                        }
                        else {
                            cell.setBackgroundColor(resources.getColor(R.color.black, resources.newTheme()))
                            cell.background.alpha = 0
                        }
                    }
                }
                else {
                    grid[columnIdx].forEach { cell ->
                        if (cell != gridCell){
                            cell.setBackgroundColor(resources.getColor(R.color.purple_500, resources.newTheme()))
                            cell.background.alpha = 30
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun changeRowHighlighting(touchMoveY: Float){
        val currentHighlightedColumnIdx = highlightedColumnIdx
        if (currentHighlightedColumnIdx != null) {
            val bottomMargin = (height - touchMoveY).toInt()
            var verticalIdx: Int? = null
            for (cellIdx in grid[0].indices) {
                if (bottomMargin < grid[0][cellIdx].topY) {
                    verticalIdx = cellIdx
                    break
                }
            }
            if (verticalIdx == null) {
                throw IllegalStateException("The grid doesn't overlay the whole area of this layout vertically!")
            }

            for (columnIdx in grid.indices){
                if (columnIdx == currentHighlightedColumnIdx){
                    for (cellIdx in grid[columnIdx].indices){
                        val cell = grid[columnIdx][cellIdx]
                        if (cellIdx != verticalIdx){
                            cell.setBackgroundColor(resources.getColor(R.color.purple_500, resources.newTheme()))
                            cell.background.alpha = 30
                        }
                        else {
                            cell.setBackgroundColor(resources.getColor(R.color.black, resources.newTheme()))
                            cell.background.alpha = 60
                        }
                    }
                }
                else {
                    for (cellIdx in grid[columnIdx].indices){
                        val cell = grid[columnIdx][cellIdx]
                        if (cellIdx != verticalIdx){
                            cell.setBackgroundColor(resources.getColor(R.color.black, resources.newTheme()))
                            cell.background.alpha = 0
                        }
                        else {
                            cell.setBackgroundColor(resources.getColor(R.color.purple_500, resources.newTheme()))
                            cell.background.alpha = 30
                        }
                    }
                }
            }

        }
    }

    fun createOverlay(){
        removeAllViews()

        val newGrid = mutableListOf<List<GridCellView>>()
        val currentMargins = horizontalMargins
        if (currentMargins != null) {
            if (currentMargins.isEmpty()) {
                newGrid.add(addConstrainedGridColumn(0, width, 0))
            } else {
                if (currentMargins.last() > width) {
                    throw IllegalArgumentException("Last horizontalMargin exceeds bar width!")
                }
                this.removeAllViews()

                // first grid column
                var leftMargin = 0
                var columnWidth = if (currentMargins.size == 1) width else currentMargins[1]
                newGrid.add(addConstrainedGridColumn(leftMargin, columnWidth, 0))
                currentMargins.removeAt(0)

                for (i in currentMargins.indices) {
                    leftMargin = currentMargins[i]
                    if (i == currentMargins.size - 1) {
                        columnWidth = width - leftMargin
                    } else {
                        columnWidth = currentMargins[i + 1] - leftMargin
                    }
                    newGrid.add(addConstrainedGridColumn(leftMargin, columnWidth, i + 1))
                }
            }
        }
        grid.clear()
        grid.addAll(newGrid)
    }

    private fun addConstrainedGridColumn(leftMargin: Int, width: Int, horizontalIdx: Int) : List<GridCellView>{

        if (this.id == 0){
            throw IllegalStateException("Can't constrain elements because this instance has no id!")
        }

        val verticalStep : Double = barHeight / 8.0
        val smallestVerticalMargin = (height - barHeight) / 2.0 - 1.5 * verticalStep
        val columnViews = mutableListOf<GridCellView>()

        // create & constrain larger bottom cell
        val bottomViewHeight = smallestVerticalMargin.toInt()
        val bottomCellView = GridCellView(context, horizontalIdx, 0, leftMargin + width, bottomViewHeight)
        bottomCellView.id = generateViewId()
        //bottomCellView.background.alpha = 0
        bottomCellView.layoutParams = LayoutParams(width, bottomViewHeight)
        addView(bottomCellView)
        columnViews.add(bottomCellView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(this)
        constraintSet.connect(bottomCellView.id, ConstraintSet.LEFT, this.id, ConstraintSet.LEFT, leftMargin)
        constraintSet.connect(bottomCellView.id, ConstraintSet.BOTTOM, this.id, ConstraintSet.BOTTOM)
        constraintSet.applyTo(this)

        val cellHeight = verticalStep.toInt()
        for (i in 0..10){
            val verticalMargin = (smallestVerticalMargin + verticalStep * i).toInt()
            val middleCellView = GridCellView(context, horizontalIdx, i + 1 ,leftMargin + width, (smallestVerticalMargin + verticalStep * (i + 1)).toInt())
            middleCellView.id = generateViewId()
            //middleCellView.background.alpha = 0
            middleCellView.layoutParams = LayoutParams(width, cellHeight)
            addView(middleCellView)
            columnViews.add(middleCellView)

            constraintSet.clone(this)
            constraintSet.connect(middleCellView.id, ConstraintSet.LEFT, this.id, ConstraintSet.LEFT, leftMargin)
            constraintSet.connect(middleCellView.id, ConstraintSet.BOTTOM, this.id, ConstraintSet.BOTTOM, verticalMargin)
            constraintSet.applyTo(this)
        }

        // create & constrain larger top cell
        val topCellView = GridCellView(context, horizontalIdx, 12, leftMargin + width, height)
        topCellView.id = generateViewId()
        // topCellView.background.alpha = 0
        topCellView.layoutParams = LayoutParams(width, bottomViewHeight)
        addView(topCellView)
        columnViews.add(topCellView)

        constraintSet.clone(this)
        constraintSet.connect(topCellView.id, ConstraintSet.LEFT, this.id, ConstraintSet.LEFT, leftMargin)
        constraintSet.connect(topCellView.id, ConstraintSet.TOP, this.id, ConstraintSet.TOP)
        constraintSet.applyTo(this)

        return columnViews
    }
}

class GridCellView(context: Context, val horizontalIdx: Int, val verticalIdx: Int, val rightX: Int, val topY: Int) : View(context)