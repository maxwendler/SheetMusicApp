package com.example.sheetmusicapp.scoreModel

import java.lang.IllegalArgumentException

/**
 * Enumeration of the different note head types supported by the app.
 *
 * @author Max Wendler
 */
enum class NoteHeadType{
    ELLIPTIC
}

/**
 * Instances of RhythmicInterval are horizontal slices of a score voice. They can contain multiple notes in different vertical positions.
 *
 * @property length The rhythmic length of the instance. The referenced instance can be changed later, the reference can not.
 * @property noteHeads Map of notes of this rhythmic interval.
 * @property startUnit Current starting position of an interval instance in a voice of an instance of [Bar].
 * @property widthPercent Percentage of width of an UI interval instance the RhythmicInterval instance should cover, for precomputation.
 * @property isRest Boolean determining if the interval is viewed as a rest or not.
 * @constructor Creates an instance of the given length with the given notes: (key:height, val:note head type).
 * Note height must be between 0 and 12. Will be represent a rest if empty map is given.
 * @author Max Wendler
 */
class RhythmicInterval(val length: RhythmicLength, initNoteHeads: Map<Int, NoteHeadType>, var startUnit: Int, var widthPercent: Double) {
    private val noteHeads = initNoteHeads.toMutableMap()
    // Initialize as rest if constructed without notes.
    var isRest = noteHeads.isEmpty()

    /**
     * Adds a note to the interval. After adding, the interval instance won't represent a rest anymore.
     *
     * @param height Height of the note. If another note exists on this height, it will be replaced.
     * @param type Type of the note head.
     * @throws IllegalArgumentException When height is less than 0 or larger than 12.
     */
    fun addNoteHead(height: Int, type: NoteHeadType){
        if (0 > height || height > 12){
            throw IllegalArgumentException("Height can't be less than 0 or larger than 12!")
        }

        // Replaces type of note heads on existing heights.
        noteHeads.put(height, type)
        isRest = false
    }

    /**
     * Removes a note from the interval. If no notes exist after removing, the instance will represent a rest.
     *
     * @param height Height of the note to remove.
     * @throws IllegalArgumentException When no note exists on the given height.
     */
    fun removeNoteHead(height: Int){
        if (0 > height || height > 12){
            throw IllegalArgumentException("Height can't be less than 0 or larger than 12!")
        }

        // Throw exception if null is returned.
        noteHeads.remove(height) ?: throw IllegalArgumentException("Removal on height where no note exists!")
        isRest = noteHeads.isEmpty()
    }

    /**
     * Makes the interval into a rest, deleting all its notes.
     *
     * @throws IllegalStateException When the interval already is a rest.
     */
    fun makeRest(){
        if (isRest) throw IllegalStateException("The rhythmic interval already is a rest!")
        else {
            noteHeads.clear()
            isRest = true
        }
    }

    companion object {
        /**
         * Static function which creates RhythmicInterval instance which is a rest / has no notes, of the given rhytmic length.
         *
         * @param length The rhythmic length the rest to be created shall have.
         * @return Rest RhythmicInterval instance of specified length.
         */
        fun makeRest(length: RhythmicLength, startUnit: Int, widthPercent: Double): RhythmicInterval {
            return RhythmicInterval(length, mapOf<Int, NoteHeadType>(), startUnit, widthPercent)
        }
    }
}