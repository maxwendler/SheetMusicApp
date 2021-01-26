package com.example.sheetmusicapp

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnLayout
import androidx.core.view.setPadding
import com.example.sheetmusicapp.parser.ScoreDeserializer
import com.example.sheetmusicapp.parser.ScoreSerializer
import com.example.sheetmusicapp.scoreModel.*
import com.example.sheetmusicapp.ui.*
import com.google.android.material.snackbar.Snackbar
import com.google.gson.GsonBuilder

const val CREATE_FILE = 1
const val PICK_FILE = 2
const val SELECT_BAR = 3

const val dotToCrossedDotRatio = 0.619

class MainActivity : AppCompatActivity(), TimeSignatureDialogFragment.NewSignatureListener, LengthSelectionDialogFragment.NewLengthListener, BarEditingOverlayLayout.GridActionUpListener {
    var parser = GsonBuilder()
    var scoreEditingLayout : ScoreEditingLayout? = null
    var timeSignatureLayout: TimeSignatureLayout? = null
    var noteInputSelectionVisible = false
    var inputNoteHeadType : NoteHeadType? = NoteHeadType.ELLIPTIC
    var inputIsDotted : Boolean = false
    var inputBasicLength : BasicRhythmicLength = BasicRhythmicLength.QUARTER
    var editingMode: EditingMode = EditingMode.ADD
    var voiceNum : Int = 1
    var mainWidth : Int = 0
    var mainHeight : Int = 0
    var statusBarHeight : Int = 0
    var menuIsVisible = false

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
        if (requestCode == SELECT_BAR && resultCode == Activity.RESULT_OK){
            if (data == null){
                throw IllegalArgumentException("No bar number data was provided!")
            }
            val barNr : Int = data.getIntExtra("barNr", 0)
            if (barNr == 0){
                throw IllegalStateException("No bar number was provided as data intent extra.")
            }
            val currentScoreEditingLayout = scoreEditingLayout
                ?: throw IllegalStateException("Can't update bar vis if scoreEditingLayout is null!")
            currentScoreEditingLayout.goToBar(barNr, editingMode)
            setMenuButtonsVisibility(false)

            val previousBarButton = findViewById<ImageButton>(R.id.prevButton)
            previousBarButton.isClickable = barNr > 1
            currentScoreEditingLayout.previousButtonDisabled = false
            updateTimeSignatureLayout(currentScoreEditingLayout.bar.timeSignature)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initMenu() {
        val menuWidth = (mainWidth * 0.15).toInt()
        val menuLayout = layoutInflater.inflate(R.layout.button_menu, null)
        menuLayout.layoutParams = ViewGroup.LayoutParams(menuWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

        val mainLayout = findViewById<ConstraintLayout>(R.id.main)
        mainLayout.addView(menuLayout)
        val constraintSet = ConstraintSet()
        constraintSet.clone(mainLayout)
        constraintSet.connect(menuLayout.id, ConstraintSet.LEFT, mainLayout.id, ConstraintSet.LEFT)
        constraintSet.connect(menuLayout.id, ConstraintSet.TOP, mainLayout.id, ConstraintSet.TOP)
        constraintSet.applyTo(mainLayout)

        val menuButton: Button = findViewById(R.id.menuButton)
        val openFileButton: Button = findViewById(R.id.button_file_open)
        val saveFileButton: Button = findViewById(R.id.button_file_save)
        val overviewButton: Button = findViewById(R.id.overviewButton)
        menuButton.setOnClickListener {
            toggleMenuButtonsVisibility()
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
        overviewButton.setOnClickListener {
            openOverview()
        }
    }

    fun setMenuButtonsVisibility(shouldBeVisible : Boolean){
        val openFileButton: Button = findViewById(R.id.button_file_open)
        val saveFileButton: Button = findViewById(R.id.button_file_save)
        val overviewButton: Button = findViewById(R.id.overviewButton)
        if (shouldBeVisible){
            openFileButton.visibility = View.VISIBLE
            saveFileButton.visibility = View.VISIBLE
            overviewButton.visibility = View.VISIBLE
            menuIsVisible = true
        }
        else {
            openFileButton.visibility = View.INVISIBLE
            saveFileButton.visibility = View.INVISIBLE
            overviewButton.visibility = View.INVISIBLE
            menuIsVisible = false
        }
    }

    fun toggleMenuButtonsVisibility(){
        setMenuButtonsVisibility(!menuIsVisible)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        initParser()


        val exampleScore = Score.makeEmpty(bars = 128, timeSignature =  TimeSignature(4, 4))
        val exampleBar = Bar.makeEmpty(1, TimeSignature(4, 4))
        exampleScore.barList[0] = exampleBar
        exampleScore.barList[1] = Bar.makeEmpty(2, TimeSignature(6, 4))
        exampleScore.barList[2] = Bar.makeEmpty(3, TimeSignature(6, 8))

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
            getStatusBarHeight()
            initMenu()
        }
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
        voiceNum = newNum
        voiceNumButton.text = newNum.toString()
        scoreEditingLayout?.changeVoiceGrid(newNum, editingMode)
    }

    fun nextBar(view: View){
        val currentScoreEditingLayout = scoreEditingLayout
                ?: throw IllegalStateException("Can't change bar without a score layout!")
        currentScoreEditingLayout.nextBar(editingMode)
        val newTimeSignature = currentScoreEditingLayout.bar.timeSignature
        updateTimeSignatureLayout(newTimeSignature)
    }

    fun previousBar(view: View){
        val currentScoreEditingLayout = scoreEditingLayout
                ?: throw IllegalStateException("Can't change bar without a score layout!")
        currentScoreEditingLayout.previousBar(editingMode)
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
                    if (inputBasicLength in listOf(BasicRhythmicLength.SIXTEENTH, BasicRhythmicLength.EIGHTH, BasicRhythmicLength.QUARTER)) {
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
                if (inputBasicLength != BasicRhythmicLength.SIXTEENTH) {
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
            if (noteInputSelectionVisible){
                if (ev.x <= mainWidth * 0.85 || ev.y >= statusBarHeight + mainHeight * 0.375){
                    toggleNoteInputSelectionVisibility()
                }
            }
            if (menuIsVisible){
                val buttonMenuLayout = findViewById<LinearLayout>(R.id.buttonMenu)
                val menuWidth = buttonMenuLayout.width
                val menuHeight = buttonMenuLayout.height
                if (ev.x > menuWidth || ev.y > statusBarHeight + menuHeight){
                    toggleMenuButtonsVisibility()
                }
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
            if (newLength != inputBasicLength){
                if (newLength != BasicRhythmicLength.SIXTEENTH || !inputIsDotted) {
                    inputBasicLength = newLength
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
            NoteHeadType.ELLIPTIC -> when(inputBasicLength){
                BasicRhythmicLength.WHOLE -> R.drawable.ic_whole
                BasicRhythmicLength.HALF -> R.drawable.ic_half
                BasicRhythmicLength.QUARTER -> R.drawable.ic_quarter
                BasicRhythmicLength.EIGHTH -> R.drawable.ic_eighth
                BasicRhythmicLength.SIXTEENTH -> R.drawable.ic_sixteenth
            }
            NoteHeadType.CROSS -> when(inputBasicLength){
                BasicRhythmicLength.QUARTER -> R.drawable.ic_x_quarter
                BasicRhythmicLength.EIGHTH -> R.drawable.ic_x_eighth
                BasicRhythmicLength.SIXTEENTH -> R.drawable.ic_x_sixteenth
                else -> throw IllegalStateException("Cross note heads are only supported for quarters, 8ths and 16ths!")
            }
            null -> when(inputBasicLength){
                BasicRhythmicLength.WHOLE, BasicRhythmicLength.HALF -> R.drawable.ic_rest_half
                BasicRhythmicLength.QUARTER -> R.drawable.ic_rest_quarter
                BasicRhythmicLength.EIGHTH -> R.drawable.ic_rest_eighth
                BasicRhythmicLength.SIXTEENTH -> R.drawable.ic_rest_sixteenth
            }
        })

        toggleOpenedButton.scaleY =
                if (inputBasicLength == BasicRhythmicLength.WHOLE && inputNoteHeadType == null) -1f
                else 1f

    }

    enum class EditingMode{
        ADD,
        DELETE
    }

    fun toggleEditingMode(view: View){
        val editModeButton = findViewById<Button>(R.id.editModeButton)
        when (editingMode){
            EditingMode.ADD -> {
                editModeButton.text = "delete"
                editingMode = EditingMode.DELETE
            }
            EditingMode.DELETE -> {
                editModeButton.text = "add"
                editingMode = EditingMode.ADD
            }
        }
        val currentScoreEditingLayout = scoreEditingLayout
                ?: throw IllegalStateException("Can't update overlay grids if scoreEditingLayout is null!")
        currentScoreEditingLayout.changeVoiceGrid(voiceNum, editingMode)
    }

    override fun handleActionUp(intervalIdx: Int, musicHeight: Int) {
        if (musicHeight < 0 || musicHeight > 12){
            throw IllegalArgumentException("musicHeight must be in 0 to 12.")
        }

        val currentScoreEditingLayout = scoreEditingLayout
                ?: throw IllegalStateException("Score editing layout must not be null!")
        val voice = currentScoreEditingLayout.bar.voices[voiceNum]

        if (voice != null){
            val voiceIntervals = voice.intervals
            if (intervalIdx >= voiceIntervals.size){
                throw IllegalArgumentException("Given intervalIdx exceeds interval list of voice!")
            }

            when (editingMode){
                EditingMode.ADD -> {
                    val inputLength =
                            if (inputIsDotted) RhythmicLength(inputBasicLength, LengthModifier.DOTTED)
                            else RhythmicLength(inputBasicLength)
                    val currentInputNoteHeadType = inputNoteHeadType

                    if (currentInputNoteHeadType == null){
                        currentScoreEditingLayout.bar.addRest(voiceNum, inputLength, intervalIdx)
                    }
                    else {
                        currentScoreEditingLayout.bar.addNote(voiceNum, inputLength, currentInputNoteHeadType, musicHeight, intervalIdx)
                    }
                }

                EditingMode.DELETE -> {
                    currentScoreEditingLayout.bar.removeNote(voiceNum, musicHeight, intervalIdx)
                    currentScoreEditingLayout.changeVoiceGrid(voiceNum, editingMode)
                }
            }

            val currentBarVisLayout = currentScoreEditingLayout.barVisLayout
                    ?: throw IllegalStateException("Can't update bar vis when barVisLayout is null!")
            currentBarVisLayout.visualizeBar()
        }
        else {
            if (editingMode == EditingMode.ADD){
                val currentInputNoteHeadType = inputNoteHeadType
                if (currentInputNoteHeadType != null){
                    val inputLength =
                            if (inputIsDotted) RhythmicLength(inputBasicLength, LengthModifier.DOTTED)
                            else RhythmicLength(inputBasicLength)
                    currentScoreEditingLayout.bar.addNote(voiceNum, inputLength, currentInputNoteHeadType, musicHeight, 0)

                    val currentBarVisLayout = currentScoreEditingLayout.barVisLayout
                            ?: throw IllegalStateException("Can't update bar vis when barVisLayout is null!")
                    currentBarVisLayout.visualizeBar()
                }
            }
        }
    }

    fun openOverview(){
        val currentScoreEditingLayout = scoreEditingLayout
            ?: throw IllegalStateException("Can't change to overview activity without score from scoreEditingLayout.")

        val intent = Intent(this, OverviewActivity::class.java).apply {
            putExtra("score", currentScoreEditingLayout.score)
        }
        startActivityForResult(intent, SELECT_BAR)
    }
}