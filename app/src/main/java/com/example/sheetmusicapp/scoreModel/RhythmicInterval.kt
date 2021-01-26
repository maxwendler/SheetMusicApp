package com.example.sheetmusicapp.scoreModel

import java.io.Serializable

/**
 * Enumeration of the different note head types supported by the app.
 *
 * @author Max Wendler
 */
enum class NoteHeadType{
    ELLIPTIC,
    CROSS
}

/**
 * Instances of RhythmicInterval are horizontal slices of a score voice. They can contain multiple notes in different vertical positions.
 *
 * @property length The rhythmic length of the instance. The referenced instance can be changed later, the reference can not.
 * can be modified by the setLength function, a copy of the instance is available via getLengthCopy.
 * @property noteHeads Map of notes of this rhythmic interval. Use via [getNoteHeadsCopy], to prevent manipulations from outside the bar.
 * @property startUnit Current starting position of an interval instance in a [Voice] in a [Bar].
 * @property endUnit Current ending position of an interval instance in a [Voice] in a [Bar]
 * @property isRest Boolean determining if the interval is viewed as a rest or not.
 * @constructor Creates an instance of the given length with the given notes: (key:height, val:note head type).
 * Note height must be between 0 and 12. Will represent a rest if empty map is given.
 * @author Max Wendler
 */
class RhythmicInterval(private val length : RhythmicLength, private val noteHeads: MutableMap<Int, NoteHeadType>, initStartUnit: Int) : Serializable{
    // Initialize as rest if constructed without notes.
    var isRest = noteHeads.isEmpty()
        private set

    var startUnit = initStartUnit
        set(value) {
            endUnit = startUnit + (length.lengthInUnits - 1)
            field = value
        }

    var endUnit = startUnit + (length.lengthInUnits - 1)
        private set

    var isCrossed = false
    init {
        for (noteHead in noteHeads){
            if (noteHead.value == NoteHeadType.CROSS) {
                isCrossed = true
                if (length.basicLength !in listOf(BasicRhythmicLength.SIXTEENTH, BasicRhythmicLength.EIGHTH, BasicRhythmicLength.QUARTER)){
                    throw IllegalArgumentException("Cross note head notes are only supported for 16ths, 8ths and quarters!")
                }
                break
            }
        }
    }

    fun setLength(newLength: RhythmicLength) {
        if (isCrossed && newLength.basicLength !in listOf(BasicRhythmicLength.SIXTEENTH, BasicRhythmicLength.EIGHTH, BasicRhythmicLength.QUARTER)){
            throw IllegalArgumentException("Cross note head notes are only supported for 16ths, 8ths and quarters!")
        }

        length.change(newLength)
        endUnit = startUnit + (length.lengthInUnits - 1)
    }

    fun getLengthCopy() : RhythmicLength{
        return RhythmicLength(length.basicLength, length.lengthModifier)
    }

    fun getNoteHeadsCopy() : MutableMap<Int, NoteHeadType> {
        return noteHeads.toMutableMap()
    }

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

        if (type == NoteHeadType.CROSS){
            isCrossed = true
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
        isCrossed = false
        for (noteHead in noteHeads){
            if (noteHead.value == NoteHeadType.CROSS) {
                isCrossed = true
                break
            }
        }
    }

    /**
     * Makes the interval into a rest, deleting all its notes.
     *
     * @throws IllegalStateException When the interval already is a rest.
     */
    fun makeRest(){
        noteHeads.clear()
        isRest = true
    }

    companion object {
        /**
         * Static function which creates RhythmicInterval instance which is a rest / has no notes, of the given rhytmic length.
         *
         * @param length The rhythmic length the rest to be created shall have.
         * @return Rest RhythmicInterval instance of specified length.
         */
        fun makeRest(length: RhythmicLength, startUnit: Int) : RhythmicInterval{
            return RhythmicInterval(length, mutableMapOf<Int, NoteHeadType>(), startUnit)
        }
    }
}