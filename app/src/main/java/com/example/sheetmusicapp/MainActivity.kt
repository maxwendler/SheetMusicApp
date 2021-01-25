package com.example.sheetmusicapp

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.media.Image
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import com.example.sheetmusicapp.parser.ScoreDeserializer
import com.example.sheetmusicapp.parser.ScoreSerializer
import com.example.sheetmusicapp.scoreModel.*
import com.example.sheetmusicapp.ui.LengthSelectionDialogFragment
import com.example.sheetmusicapp.ui.ScoreEditingLayout
import com.example.sheetmusicapp.ui.TimeSignatureDialogFragment
import com.example.sheetmusicapp.ui.TimeSignatureLayout
import com.google.android.material.snackbar.Snackbar
import com.google.gson.GsonBuilder

const val CREATE_FILE = 1
const val PICK_FILE = 2

const val dotToCrossedDotRatio = 0.619

class MainActivity : AppCompatActivity(), TimeSignatureDialogFragment.NewSignatureListener, LengthSelectionDialogFragment.NewLengthListener {
    var parser = GsonBuilder()
    var scoreEditingLayout : ScoreEditingLayout? = null
    var timeSignatureLayout: TimeSignatureLayout? = null
    var noteInputSelectionVisible = false
    var inputNoteHeadType : NoteHeadType? = NoteHeadType.ELLIPTIC
    var inputIsDotted : Boolean = false
    var inputLength : BasicRhythmicLength = BasicRhythmicLength.QUARTER
    var mainWidth : Int = 0
    var mainHeight : Int = 0
    var statusBarHeight : Int = 0

    lateinit var toggleOpenedButton: ImageButton
    lateinit var toggleNoteHeadButton: ImageButton
    lateinit var toggleDottedButton: ImageButton
    lateinit var changeLengthButton: Button

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
        actionBar?.hide()
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
            addNoteInputSelectionLayout()
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

        mainWidth = mainLayout.width
        mainHeight = mainLayout.height

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

    override fun onSignatureDialogPositiveClick(dialog: TimeSignatureDialogFragment) {
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

    fun addNoteInputSelectionLayout(){
        val mainLayout = findViewById<ConstraintLayout>(R.id.main)
        if (mainLayout.height == 0 || mainLayout.width == 0){
            throw IllegalStateException("'main' layout has not been laid out yet!" )
        }

        val width = (mainLayout.width * 0.15).toInt()
        val height = (mainLayout.height * 0.375).toInt()

        val layout : ConstraintLayout = layoutInflater.inflate(R.layout.note_input_selection, null) as ConstraintLayout
        layout.id = ViewGroup.generateViewId()
        layout.layoutParams = ViewGroup.LayoutParams(width, height)
        mainLayout.addView(layout)

        val constraintSet = ConstraintSet()
        constraintSet.clone(mainLayout)
        constraintSet.connect(layout.id, ConstraintSet.RIGHT, mainLayout.id, ConstraintSet.RIGHT)
        constraintSet.connect(layout.id, ConstraintSet.TOP, mainLayout.id, ConstraintSet.TOP)
        constraintSet.applyTo(mainLayout)

        toggleOpenedButton = layout.findViewById<ImageButton>(R.id.toggleOpenedButton)
        toggleDottedButton = layout.findViewById<ImageButton>(R.id.toggleDottedButton)
        toggleNoteHeadButton = layout.findViewById<ImageButton>(R.id.toggleNoteHeadButton)
        changeLengthButton = layout.findViewById<Button>(R.id.changeLengthButton)

        toggleOpenedButton.setOnClickListener {
            toggleNoteInputSelectionVisibility()
        }

        toggleNoteHeadButton.setOnClickListener {
            val imageButton = it as ImageButton
            when (inputNoteHeadType){
                NoteHeadType.ELLIPTIC -> {
                    if (inputLength in listOf(BasicRhythmicLength.SIXTEENTH, BasicRhythmicLength.EIGHTH, BasicRhythmicLength.QUARTER)) {
                        imageButton.setImageResource(R.drawable.ic_x_notehead)
                        inputNoteHeadType = NoteHeadType.CROSS
                    }
                    else {
                        Toast.makeText(this, "Cross note heads are only supported for quarters, 8ths and 16ths!", Toast.LENGTH_LONG).show()
                        imageButton.setImageResource(R.drawable.ic_rest_quarter)
                        inputNoteHeadType = null
                    }
                }
                NoteHeadType.CROSS -> {
                    imageButton.setImageResource(R.drawable.ic_rest_quarter)
                    inputNoteHeadType = null
                }
                null -> {
                    imageButton.setImageResource(R.drawable.ic_full_notehead)
                    inputNoteHeadType = NoteHeadType.ELLIPTIC
                }
            }
            setInputSelectionSummaryImage()
        }

        toggleDottedButton.setOnClickListener {
            val imageButton = it as ImageButton
            if (!inputIsDotted){
                if (inputLength != BasicRhythmicLength.SIXTEENTH) {
                    imageButton.setImageResource(R.drawable.black_circle)
                    imageButton.setPadding(55)
                    inputIsDotted = true
                }
                else {
                    Toast.makeText(this, "Dotted 16ths are not supported!", Toast.LENGTH_LONG).show()
                }
            }
            else {
                imageButton.setImageResource(R.drawable.ic_crossed_black_circle)
                imageButton.setPadding(45)
                inputIsDotted = false
            }
        }

        changeLengthButton.setOnClickListener {
            showLengthSelectionDialog()
        }
    }

    fun toggleNoteInputSelectionVisibility(){
        val newVisibility = if (noteInputSelectionVisible) View.INVISIBLE else View.VISIBLE
        toggleDottedButton.visibility = newVisibility
        toggleNoteHeadButton.visibility = newVisibility
        changeLengthButton.visibility = newVisibility
        noteInputSelectionVisible = !noteInputSelectionVisible
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null){
            if (ev.x <= mainWidth * 0.85 || ev.y >= (statusBarHeight + mainHeight * 0.375) * 1.2){
                if (noteInputSelectionVisible) toggleNoteInputSelectionVisibility()
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    fun getStatusBarHeight(){
        val rectangle = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rectangle)
        statusBarHeight = rectangle.top
    }

    fun showLengthSelectionDialog(){
        val dialog = LengthSelectionDialogFragment(inputNoteHeadType, inputIsDotted)
        dialog.listener = this
        dialog.show(supportFragmentManager, "TimeSignatureDialog")
    }

    override fun onLengthDialogPositiveClick(newLength: BasicRhythmicLength?) {
        if (newLength != null){
            if (newLength != inputLength){
                if (newLength != BasicRhythmicLength.SIXTEENTH || !inputIsDotted) {
                    inputLength = newLength
                    changeLengthButton.setText(when (newLength) {
                        BasicRhythmicLength.WHOLE -> "1/1"
                        BasicRhythmicLength.HALF -> "1/2"
                        BasicRhythmicLength.QUARTER -> "1/4"
                        BasicRhythmicLength.EIGHTH -> "1/8"
                        BasicRhythmicLength.SIXTEENTH -> "1/16"
                    })
                    changeLengthButton.textSize =
                            if (newLength == BasicRhythmicLength.SIXTEENTH) 10f
                            else 14f
                    setInputSelectionSummaryImage()
                }
                else {
                    Toast.makeText(this, "Dotted 16ths are not supported!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun setInputSelectionSummaryImage(){
        toggleOpenedButton.setImageResource(when(inputNoteHeadType){
            NoteHeadType.ELLIPTIC -> when(inputLength){
                BasicRhythmicLength.WHOLE -> R.drawable.ic_whole
                BasicRhythmicLength.HALF -> R.drawable.ic_half
                BasicRhythmicLength.QUARTER -> R.drawable.ic_quarter
                BasicRhythmicLength.EIGHTH -> R.drawable.ic_eighth
                BasicRhythmicLength.SIXTEENTH -> R.drawable.ic_sixteenth
            }
            NoteHeadType.CROSS -> when(inputLength){
                BasicRhythmicLength.QUARTER -> R.drawable.ic_x_quarter
                BasicRhythmicLength.EIGHTH -> R.drawable.ic_x_eighth
                BasicRhythmicLength.SIXTEENTH -> R.drawable.ic_x_sixteenth
                else -> throw IllegalStateException("Cross note heads are only supported for quarters, 8ths and 16ths!")
            }
            null -> when(inputLength){
                BasicRhythmicLength.WHOLE, BasicRhythmicLength.HALF -> R.drawable.ic_rest_half
                BasicRhythmicLength.QUARTER -> R.drawable.ic_rest_quarter
                BasicRhythmicLength.EIGHTH -> R.drawable.ic_rest_eighth
                BasicRhythmicLength.SIXTEENTH -> R.drawable.ic_rest_sixteenth
            }
        })

        toggleOpenedButton.scaleY =
                if (inputLength == BasicRhythmicLength.WHOLE && inputNoteHeadType == null) -1f
                else 1f

    }
}