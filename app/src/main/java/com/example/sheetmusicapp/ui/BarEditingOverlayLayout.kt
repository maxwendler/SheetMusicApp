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

/**
 * ConstraintLayout containing a grid of [GridCellView]s which capture touch events on a bar.
 * Grid created according to bar division into note height spaces 0..12, derived from [barHeight]
 * and horizontal division according to intervals of a bar voice ([horizontalMargins]), which should be set through
 * a callback from [BarVisLayout].
 */
class BarEditingOverlayLayout(context: Context, private val barHeight: Int) : ConstraintLayout(context) {

    private val grid : MutableList<List<GridCellView>> = mutableListOf()
    var horizontalMargins : MutableList<Int>? = null
    var highlightedColumnIdx : Int? = null
    var highlightedRowIdx : Int? = null
    lateinit var listener: GridActionUpListener

    init {
        doOnLayout { if (horizontalMargins != null) createOverlay() }
    }

    // Listener which should handle the selection of a grid cell via MotionEvent.ACTION_MOVE.
    interface GridActionUpListener {
        // intervalIdx = horizontalIdx of grid cell, musicHeight = verticalIdx of grid cell
        fun handleActionUp(intervalIdx: Int, musicHeight: Int)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null){
            // Highlight cell on initial touch event.
            if (event.action == MotionEvent.ACTION_DOWN){
                highlightCellColumnAndRow(event.x, event.y)
            }
            // Highlight cell on successive moves.
            if (event.action == MotionEvent.ACTION_MOVE){
                if (event.y >= 0 && event.y <= this.height)
                    highlightCellColumnAndRow(event.x, event.y)
                // highlight some horizontal cell with maximum vertical index if touch above
                // the grid area
                else if (event.y < 0){
                    highlightCellColumnAndRow(event.x, 1f)
                }
                // highlight some horizontal cell with minimum vertical index if touch above
                // the grid area
                else if (event.y > this.height){
                    highlightCellColumnAndRow(event.x, height - 1f)
                }
            }

            // Remove all highlighting and call listener.
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

    /**
     * Maps coordinates within this layout to grid cell. Only returns null if the grid is empty.
     */
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

    /**
     * Highlights a selected cell in one colour, and its grid column and row in another.
     * Removes highlighting from all other cells.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun highlightCellColumnAndRow(touchDownX: Float, touchDownY: Float){
        val gridCell = findGridCell(touchDownX.toInt(),height - touchDownY.toInt())

        if (gridCell != null){
            highlightedColumnIdx = gridCell.horizontalIdx
            highlightedRowIdx = gridCell.verticalIdx

            gridCell.setBackgroundColor(resources.getColor(R.color.black, resources.newTheme()))
            gridCell.background.alpha = 60

            for (columnIdx in grid.indices){
                // Highlight cells in row of cell.
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
                // Highlight column of cell.
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

    /**
     * Creates a new grid of [GridCellView]s, according to [horizontalMargins], hopefully set by a callback
     * of [BarVisLayout], and division of [barHeight] into musical heights 0..12
     */
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

                // remaining grid columns
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

    /**
     * Adds a colum of [GridCellView]s to [grid] and this layout, with specified [width] and constrained
     * to left according to [leftMargin].
     *
     * @throws IllegalArgumentException When an id for this layout hasn't been set.
     */
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
        bottomCellView.layoutParams = LayoutParams(width, bottomViewHeight)
        addView(bottomCellView)
        columnViews.add(bottomCellView)

        val constraintSet = ConstraintSet()
        constraintSet.clone(this)
        constraintSet.connect(bottomCellView.id, ConstraintSet.LEFT, this.id, ConstraintSet.LEFT, leftMargin)
        constraintSet.connect(bottomCellView.id, ConstraintSet.BOTTOM, this.id, ConstraintSet.BOTTOM)
        constraintSet.applyTo(this)

        // create & constrain equally sized middle cells
        val cellHeight = verticalStep.toInt()
        for (i in 0..10){
            val verticalMargin = (smallestVerticalMargin + verticalStep * i).toInt()
            val middleCellView = GridCellView(context, horizontalIdx, i + 1 ,leftMargin + width, (smallestVerticalMargin + verticalStep * (i + 1)).toInt())
            middleCellView.id = generateViewId()
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

/**
 * View with additional [horizontalIdx] to save represented interval index in voice, [verticalIdx] for represented musical
 * height, and [rightX] & [topY] to find the cell from touch event coordinates.
 */
class GridCellView(context: Context, val horizontalIdx: Int, val verticalIdx: Int, val rightX: Int, val topY: Int) : View(context)