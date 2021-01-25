package com.example.sheetmusicapp

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.example.sheetmusicapp.parser.ScoreDeserializer
import com.example.sheetmusicapp.parser.ScoreSerializer
import com.example.sheetmusicapp.scoreModel.*
import com.example.sheetmusicapp.ui.ScoreEditingLayout
import com.example.sheetmusicapp.ui.TimeSignatureDialogFragment
import com.example.sheetmusicapp.ui.TimeSignatureLayout
import com.google.android.material.snackbar.Snackbar
import com.google.gson.GsonBuilder

const val CREATE_FILE = 1
const val PICK_FILE = 2


class MainActivity : AppCompatActivity(), TimeSignatureDialogFragment.NewSignatureListener {
    var parser = GsonBuilder()
    var scoreEditingLayout : ScoreEditingLayout? = null
    var timeSignatureLayout: TimeSignatureLayout? = null

    private fun initParser() {
        parser.registerTypeAdapter(Score::class.java, ScoreSerializer())
        parser.registerTypeAdapter(Score::class.java, ScoreDeserializer())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                // Perform operations on the document using its URI.
                println(uri)
                val exampleScore = Score.makeEmpty(bars = 32, timeSignature =  TimeSignature(4, 4))
                val json = parser.setPrettyPrinting().create().toJson(exampleScore)
                val file = contentResolver.openOutputStream(uri)
                file?.write(json.toByteArray())
            }
        }
        if (requestCode == PICK_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                println(uri)
                // Perform operations on the document using its URI.
                val file = contentResolver.openInputStream(uri)
                val jsonRaw = file?.readBytes()
                var json = jsonRaw?.let { String(it) }
                val test = parser.create().fromJson(json, Score::class.java)
                if (test == null) {
                    Snackbar.make(
                            findViewById(R.id.main),
                            "Wrong file format",
                            Snackbar.LENGTH_SHORT
                    ).show()
                }
//                println(test)
//                val mainConstraintLayout = findViewById<ConstraintLayout>(R.id.main)
//                mainConstraintLayout.doOnLayout {
//                    scaleBarLineStrokeWidth()
//                    visualizeBar(test.barList[0])
//                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initButtonGroups() {
        val fileButton: Button = findViewById(R.id.button_file)
        val openFileButton: Button = findViewById(R.id.button_file_open)
        val saveFileButton: Button = findViewById(R.id.button_file_save)
        fileButton.setOnClickListener {
            if (openFileButton.isVisible) {
                openFileButton.visibility = View.GONE
                saveFileButton.visibility = View.GONE
            } else {
                openFileButton.visibility = View.VISIBLE
                saveFileButton.visibility = View.VISIBLE
            }

        }
        openFileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            startActivityForResult(intent, PICK_FILE)
        }

        saveFileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "sheet.json")
            }
            startActivityForResult(intent, CREATE_FILE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initParser()


        val exampleScore = Score.makeEmpty(bars = 1, timeSignature =  TimeSignature(4, 4))
        val exampleBar = Bar.makeEmpty(1, TimeSignature(4, 4))
        exampleScore.barList[0] = exampleBar
        exampleScore.barList.add(Bar.makeEmpty(2, TimeSignature(4, 4)))
        exampleScore.barList.add(Bar.makeEmpty(3, TimeSignature(6, 8)))

        exampleBar.addNote(1, RhythmicLength(BasicRhythmicLength.EIGHTH), NoteHeadType.CROSS, 11, 0)
        exampleBar.addNote(1, RhythmicLength(BasicRhythmicLength.EIGHTH), NoteHeadType.CROSS, 11, 1)
        exampleBar.addNote(1, RhythmicLength(BasicRhythmicLength.QUARTER), NoteHeadType.CROSS, 11, 2)
        // exampleBar.addNote(1, RhythmicLength(BasicRhythmicLength.EIGHTH), NoteHeadType.CROSS, 11, 2)
        exampleBar.addNote(1, RhythmicLength(BasicRhythmicLength.EIGHTH), NoteHeadType.CROSS, 11, 3)
        exampleBar.addNote(1, RhythmicLength(BasicRhythmicLength.EIGHTH), NoteHeadType.CROSS, 11, 4)
        exampleBar.addNote(1, RhythmicLength(BasicRhythmicLength.EIGHTH), NoteHeadType.CROSS, 11, 5)
        exampleBar.addNote(1, RhythmicLength(BasicRhythmicLength.EIGHTH), NoteHeadType.CROSS, 11, 6)
        exampleBar.addNote(1, RhythmicLength(BasicRhythmicLength.EIGHTH, LengthModifier.DOTTED), NoteHeadType.CROSS, 11, 0)
        exampleBar.addNote(1, RhythmicLength(BasicRhythmicLength.SIXTEENTH), NoteHeadType.CROSS, 11, 1)
        exampleBar.addNote(1, RhythmicLength(BasicRhythmicLength.QUARTER), NoteHeadType.ELLIPTIC, 7, 2)
        exampleBar.addNote(1, RhythmicLength(BasicRhythmicLength.EIGHTH), NoteHeadType.ELLIPTIC, 7, 5)

        exampleBar.addNote(2, RhythmicLength(BasicRhythmicLength.QUARTER), NoteHeadType.ELLIPTIC, 3, 0)
        exampleBar.addRest(2, RhythmicLength(BasicRhythmicLength.QUARTER), 1)
        exampleBar.addNote(2, RhythmicLength(BasicRhythmicLength.QUARTER, LengthModifier.DOTTED), NoteHeadType.ELLIPTIC, 3, 2)

        // exampleBar.addRest(1, RhythmicLength(BasicRhythmicLength.QUARTER, LengthModifier.DOTTED), 0)

        setContentView(R.layout.activity_main)

        val mainConstraintLayout = findViewById<ConstraintLayout>(R.id.main)
        mainConstraintLayout.doOnLayout {
            scoreEditingLayout = addScoreEditingLayout(exampleScore)
            timeSignatureLayout = addTimeSignatureLayout(exampleScore.barList[0].timeSignature)

        }
        initButtonGroups()
    }

    /**
     * Adds reusable bar layout component and binds the given [bar] to it. It will be displayed when
     * the returned layout it laid out.
     *
     * @throws IllegalStateException When height or width of this activity's main layout is 0, appearing
     * as it has not been laid out yet.
     */
    private fun addScoreEditingLayout(score: Score) : ScoreEditingLayout{

        val mainLayout = findViewById<ConstraintLayout>(R.id.main)
        if (mainLayout.height == 0 || mainLayout.width == 0){
            throw IllegalStateException("'main' layout has not been laid out yet!" )
        }
        val horizontalBarMargin = (mainLayout.width * 0.15).toInt()
        val width = (mainLayout.width * 0.7).toInt()
        val barHeight = mainLayout.height * 0.25

        val editableBarLayout = ScoreEditingLayout(this, findViewById(R.id.prevButton), barHeight.toInt(), score)
        editableBarLayout.id = ViewGroup.generateViewId()
        editableBarLayout.tag = "editableBarLayout"
        editableBarLayout.layoutParams = ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT)
        mainLayout.addView(editableBarLayout)

        val constraintSet = ConstraintSet()
        constraintSet.clone(mainLayout)
        constraintSet.connect(editableBarLayout.id, ConstraintSet.LEFT, R.id.main, ConstraintSet.LEFT, horizontalBarMargin)
        constraintSet.connect(editableBarLayout.id, ConstraintSet.RIGHT, R.id.main, ConstraintSet.RIGHT, horizontalBarMargin)
        constraintSet.applyTo(mainLayout)

        return editableBarLayout
    }

    private fun addTimeSignatureLayout(timeSignature: TimeSignature) : TimeSignatureLayout{
        val mainLayout = findViewById<ConstraintLayout>(R.id.main)
        if (mainLayout.height == 0 || mainLayout.width == 0){
            throw IllegalStateException("'main' layout has not been laid out yet!" )
        }
        val barHeight = mainLayout.height * 0.25

        val currentScoreEditingLayout = scoreEditingLayout
                ?: throw IllegalStateException("Can't add time signature vis without score editing layout to constrain to!")

        val timeSignatureLayout = TimeSignatureLayout(this, timeSignature)
        timeSignatureLayout.id = ViewGroup.generateViewId()
        timeSignatureLayout.layoutParams = ViewGroup.LayoutParams((barHeight / 3).toInt(), barHeight.toInt())
        timeSignatureLayout.setOnClickListener { showTimeSignatureDialog(it) }
        mainLayout.addView(timeSignatureLayout)

        val constraintSet = ConstraintSet()
        constraintSet.clone(mainLayout)
        constraintSet.connect(timeSignatureLayout.id, ConstraintSet.RIGHT, currentScoreEditingLayout.id, ConstraintSet.LEFT, 16)
        constraintSet.connect(timeSignatureLayout.id, ConstraintSet.TOP, mainLayout.id, ConstraintSet.TOP)
        constraintSet.connect(timeSignatureLayout.id, ConstraintSet.BOTTOM, mainLayout.id, ConstraintSet.BOTTOM)
        constraintSet.applyTo(mainLayout)

        return timeSignatureLayout
    }

    fun handleVoiceChange(view: View){
        val voiceNumButton = findViewById<Button>(R.id.voiceNumButton)
        val currentNum = voiceNumButton.text.toString().toInt()
        val newNum = currentNum % 4 + 1
        voiceNumButton.text = newNum.toString()
        scoreEditingLayout?.changeVisibleGrid(newNum)
    }

    fun nextBar(view: View){
        val currentScoreEditingLayout = scoreEditingLayout
                ?: throw IllegalStateException("Can't change bar without a score layout!")
        currentScoreEditingLayout.nextBar()
        val newTimeSignature = currentScoreEditingLayout.bar.timeSignature
        updateTimeSignatureLayout(newTimeSignature)
    }

    fun previousBar(view: View){
        val currentScoreEditingLayout = scoreEditingLayout
                ?: throw IllegalStateException("Can't change bar without a score layout!")
        currentScoreEditingLayout.previousBar()
        val newTimeSignature = currentScoreEditingLayout.bar.timeSignature
        updateTimeSignatureLayout(newTimeSignature)
    }

    fun showTimeSignatureDialog(view: View) {
        val currentScoreEditingLayout = scoreEditingLayout
                ?: throw IllegalStateException("Can't pass current time signature when scoreEditingLayout is null!")

        val dialog = TimeSignatureDialogFragment(currentScoreEditingLayout.bar.timeSignature)
        dialog.show(supportFragmentManager, "TimeSignatureDialog")
    }

    fun updateTimeSignatureLayout(newTimeSignature: TimeSignature){
        val currentTimeSignatureLayout = timeSignatureLayout
                ?: throw IllegalStateException("Can't change update time signature without time signature layout!")
        currentTimeSignatureLayout.updateViews(newTimeSignature)
    }

    override fun onDialogPositiveClick(dialog: TimeSignatureDialogFragment) {
        val currentScoreEditingLayout = scoreEditingLayout
                ?: throw IllegalStateException("Can't change score data when scoreEditingLayout is null!")

        val newNumeratorText = dialog.numeratorEditText.text
        val newDenominatorText = dialog.denominatorEditText.text
        if (newNumeratorText.toString() != "" && newDenominatorText.toString() != ""){
            val newTimeSignature = TimeSignature(newNumeratorText.toString().toInt(), newDenominatorText.toString().toInt())
            currentScoreEditingLayout.changeCurrentBarTimeSignature(newTimeSignature)
            updateTimeSignatureLayout(newTimeSignature)
        }
    }
}