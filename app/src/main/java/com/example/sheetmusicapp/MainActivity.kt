package com.example.sheetmusicapp

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.gson.GsonBuilder
import java.util.*
import kotlin.concurrent.fixedRateTimer


const val CREATE_FILE = 1
const val PICK_FILE = 2
const val SELECT_BAR = 3
const val RC_SIGN_IN = 4
const val OPEN_CLOUD_LIST = 5

class MainActivity : AppCompatActivity(),
        TimeSignatureDialogFragment.NewSignatureListener,
        LengthSelectionDialogFragment.NewLengthListener,
        BarEditingOverlayLayout.GridActionUpListener,
        SetTitleDialogFragment.SetTitleListener,
        DeleteInsertBarDialogFragment.BarsModListener{
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
    var cloudFile: DatabaseReference? = null
    private lateinit var auth: FirebaseAuth
    lateinit var timer: Timer
    var networkStatus: Boolean = true
    var isCloudFile: Boolean = false

    lateinit var toggleOpenedButton: ImageButton
    lateinit var toggleNoteHeadButton: ImageButton
    lateinit var toggleDottedButton: ImageButton
    lateinit var changeLengthButton: Button

    private fun initParser() {
        parser.registerTypeAdapter(Score::class.java, ScoreSerializer())
        parser.registerTypeAdapter(Score::class.java, ScoreDeserializer())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initMenu() {
        val menuWidth = (mainWidth * 0.15).toInt()
        val menuLayout = layoutInflater.inflate(R.layout.button_menu, null)
        menuLayout.layoutParams = ViewGroup.LayoutParams(
            menuWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val mainLayout = findViewById<ConstraintLayout>(R.id.main)
        mainLayout.addView(menuLayout)
        val constraintSet = ConstraintSet()
        constraintSet.clone(mainLayout)
        constraintSet.connect(menuLayout.id, ConstraintSet.LEFT, mainLayout.id, ConstraintSet.LEFT)
        constraintSet.connect(menuLayout.id, ConstraintSet.TOP, mainLayout.id, ConstraintSet.TOP)
        constraintSet.applyTo(mainLayout)

        val menuButton: Button = findViewById(R.id.menuButton)
        val openFileButton: Button = findViewById(R.id.button_file_open)
        val openFileCloudButton: Button = findViewById(R.id.button_file_open_cloud)
        val saveFileButton: Button = findViewById(R.id.button_file_save)
        val saveFileCloudButton: Button = findViewById(R.id.button_file_save_cloud)
        val overviewButton: Button = findViewById(R.id.overviewButton)
        val barsModButton: Button = findViewById(R.id.barsModButton)
        val logoutButton: Button = findViewById(R.id.button_logout)

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
        openFileCloudButton.setOnClickListener {
            openCloudFile()
        }

        saveFileButton.setOnClickListener {
            showSetTitleDialog()
        }
        saveFileCloudButton.setOnClickListener {
            saveToCloud()
        }
        overviewButton.setOnClickListener {
            openOverview()
        }
        barsModButton.setOnClickListener {
            showBarsModificationDialog()
        }
        logoutButton.setOnClickListener {
            logout()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_FILE && resultCode == RESULT_OK) {
            data?.data?.also { uri ->
                // Perform operations on the document using its URI.
                val currentScoreEditingLayout = scoreEditingLayout
                        ?: throw IllegalStateException("Can't change score title if scoreEditingLayout is null!")
                val score = currentScoreEditingLayout.score;
                val json = parser.setPrettyPrinting().create().toJson(score)
                val file = contentResolver.openOutputStream(uri)
                file?.write(json.toByteArray())
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Save file to cloud")
                    .setMessage("Do you want to upload file to the cloud?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("YES",
                        DialogInterface.OnClickListener { dialog, id ->
                            saveToCloudWithMessage()
                        })
                    .setNegativeButton("NO", null).show()
            }
        }
        if (requestCode == PICK_FILE && resultCode == RESULT_OK) {
            data?.data?.also { uri ->
                // Perform operations on the document using its URI.
                val file = contentResolver.openInputStream(uri)
                val jsonRaw = file?.readBytes()
                val json = jsonRaw?.let { String(it) }
                if (json != null) {
                    loadFile(json)
                    isCloudFile = false
                }
            }
        }
        if (requestCode == SELECT_BAR && resultCode == RESULT_OK){
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
            updateTimeSignatureLayout(currentScoreEditingLayout.bar.timeSignature)
        }
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser
                println(user)
                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
        if (requestCode == OPEN_CLOUD_LIST && resultCode == RESULT_OK){
            if (data == null){
                throw IllegalArgumentException("No title data was provided!")
            }
            val title = data.getStringExtra("title")
                ?: throw IllegalStateException("No title data was provided as data intent extra.")
            loadFromCloud(title)
        }
    }


    fun setMenuButtonsVisibility(shouldBeVisible: Boolean){
        val openFileButton: Button = findViewById(R.id.button_file_open)
        val openFileCloudButton: Button = findViewById(R.id.button_file_open_cloud)
        val saveFileButton: Button = findViewById(R.id.button_file_save)
        val saveFileCloudButton: Button = findViewById(R.id.button_file_save_cloud)
        val overviewButton: Button = findViewById(R.id.overviewButton)
        val barsModButton: Button = findViewById(R.id.barsModButton)
        val logoutButton: Button = findViewById(R.id.button_logout)
        if (shouldBeVisible){
            openFileButton.visibility = View.VISIBLE
            openFileCloudButton.visibility = View.VISIBLE
            saveFileButton.visibility = View.VISIBLE
            saveFileCloudButton.visibility = View.VISIBLE
            overviewButton.visibility = View.VISIBLE
            barsModButton.visibility = View.VISIBLE
            logoutButton.visibility = View.VISIBLE
            menuIsVisible = true
        }
        else {
            openFileButton.visibility = View.INVISIBLE
            openFileCloudButton.visibility = View.INVISIBLE
            saveFileButton.visibility = View.INVISIBLE
            saveFileCloudButton.visibility = View.INVISIBLE
            overviewButton.visibility = View.INVISIBLE
            barsModButton.visibility = View.INVISIBLE
            logoutButton.visibility = View.INVISIBLE
            menuIsVisible = false
        }
    }

    fun toggleMenuButtonsVisibility(){
        setMenuButtonsVisibility(!menuIsVisible)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.database.setPersistenceEnabled(true)
        auth = Firebase.auth
        actionBar?.hide()
        initParser()
        initNetwork()

        val newScore : Score =
                if (savedInstanceState != null){
                    val scoreSerializable = savedInstanceState.getSerializable("score")
                    if (scoreSerializable != null){
                        scoreSerializable as Score
                    }
                    else Score.makeEmpty(1, TimeSignature(4, 4))
                }
                else Score.makeEmpty(1, TimeSignature(4, 4))

        setContentView(R.layout.activity_main)

        val mainConstraintLayout = findViewById<ConstraintLayout>(R.id.main)
        mainConstraintLayout.doOnLayout {
            scoreEditingLayout = addScoreEditingLayout(newScore)
            timeSignatureLayout = addTimeSignatureLayout(newScore.barList[0].timeSignature)
            addNoteInputSelectionLayout()
            getStatusBarHeight()
            initMenu()
        }
        startTimer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putSerializable("score", scoreEditingLayout?.score)
        }

        super.onSaveInstanceState(outState)
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
            throw IllegalStateException("'main' layout has not been laid out yet!")
        }

        mainWidth = mainLayout.width
        mainHeight = mainLayout.height

        val horizontalBarMargin = (mainLayout.width * 0.15).toInt()
        val width = (mainLayout.width * 0.7).toInt()
        val barHeight = mainLayout.height * 0.25

        val newScoreEditingLayout = ScoreEditingLayout(
            this,
            findViewById(R.id.prevButton),
            barHeight.toInt(),
            score
        )
        newScoreEditingLayout.id = ViewGroup.generateViewId()
        newScoreEditingLayout.tag = "editableBarLayout"
        newScoreEditingLayout.layoutParams = ViewGroup.LayoutParams(
            width,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        mainLayout.addView(newScoreEditingLayout)

        val constraintSet = ConstraintSet()
        constraintSet.clone(mainLayout)
        constraintSet.connect(
            newScoreEditingLayout.id,
            ConstraintSet.LEFT,
            R.id.main,
            ConstraintSet.LEFT,
            horizontalBarMargin
        )
        constraintSet.connect(
            newScoreEditingLayout.id,
            ConstraintSet.RIGHT,
            R.id.main,
            ConstraintSet.RIGHT,
            horizontalBarMargin
        )
        constraintSet.applyTo(mainLayout)

        return newScoreEditingLayout
    }

    private fun addTimeSignatureLayout(timeSignature: TimeSignature) : TimeSignatureLayout{
        val mainLayout = findViewById<ConstraintLayout>(R.id.main)
        if (mainLayout.height == 0 || mainLayout.width == 0){
            throw IllegalStateException("'main' layout has not been laid out yet!")
        }
        val barHeight = mainLayout.height * 0.25

        val currentScoreEditingLayout = scoreEditingLayout
                ?: throw IllegalStateException("Can't add time signature vis without score editing layout to constrain to!")

        val timeSignatureLayout = TimeSignatureLayout(this, timeSignature)
        timeSignatureLayout.id = ViewGroup.generateViewId()
        timeSignatureLayout.layoutParams = ViewGroup.LayoutParams(
            (barHeight / 3).toInt(),
            barHeight.toInt()
        )
        timeSignatureLayout.setOnClickListener { showTimeSignatureDialog(it) }
        mainLayout.addView(timeSignatureLayout)

        val constraintSet = ConstraintSet()
        constraintSet.clone(mainLayout)
        constraintSet.connect(
            timeSignatureLayout.id,
            ConstraintSet.RIGHT,
            currentScoreEditingLayout.id,
            ConstraintSet.LEFT,
            16
        )
        constraintSet.connect(
            timeSignatureLayout.id,
            ConstraintSet.TOP,
            mainLayout.id,
            ConstraintSet.TOP
        )
        constraintSet.connect(
            timeSignatureLayout.id,
            ConstraintSet.BOTTOM,
            mainLayout.id,
            ConstraintSet.BOTTOM
        )
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
            val newTimeSignature = TimeSignature(
                newNumeratorText.toString().toInt(),
                newDenominatorText.toString().toInt()
            )
            currentScoreEditingLayout.changeCurrentBarTimeSignature(newTimeSignature)
            updateTimeSignatureLayout(newTimeSignature)
        }
    }

    fun addNoteInputSelectionLayout(){
        val mainLayout = findViewById<ConstraintLayout>(R.id.main)
        if (mainLayout.height == 0 || mainLayout.width == 0){
            throw IllegalStateException("'main' layout has not been laid out yet!")
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
                    if (inputBasicLength in listOf(
                            BasicRhythmicLength.SIXTEENTH,
                            BasicRhythmicLength.EIGHTH,
                            BasicRhythmicLength.QUARTER
                        )
                    ) {
                        imageButton.setImageResource(R.drawable.ic_x_notehead)
                        inputNoteHeadType = NoteHeadType.CROSS
                    } else {
                        Toast.makeText(
                            this,
                            "Cross note heads are only supported for quarters, 8ths and 16ths!",
                            Toast.LENGTH_LONG
                        ).show()
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
                    changeLengthButton.setText(
                        when (newLength) {
                            BasicRhythmicLength.WHOLE -> "1/1"
                            BasicRhythmicLength.HALF -> "1/2"
                            BasicRhythmicLength.QUARTER -> "1/4"
                            BasicRhythmicLength.EIGHTH -> "1/8"
                            BasicRhythmicLength.SIXTEENTH -> "1/16"
                        }
                    )
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
        toggleOpenedButton.setImageResource(
            when (inputNoteHeadType) {
                NoteHeadType.ELLIPTIC -> when (inputBasicLength) {
                    BasicRhythmicLength.WHOLE -> R.drawable.ic_whole
                    BasicRhythmicLength.HALF -> R.drawable.ic_half
                    BasicRhythmicLength.QUARTER -> R.drawable.ic_quarter
                    BasicRhythmicLength.EIGHTH -> R.drawable.ic_eighth
                    BasicRhythmicLength.SIXTEENTH -> R.drawable.ic_sixteenth
                }
                NoteHeadType.CROSS -> when (inputBasicLength) {
                    BasicRhythmicLength.QUARTER -> R.drawable.ic_x_quarter
                    BasicRhythmicLength.EIGHTH -> R.drawable.ic_x_eighth
                    BasicRhythmicLength.SIXTEENTH -> R.drawable.ic_x_sixteenth
                    else -> throw IllegalStateException("Cross note heads are only supported for quarters, 8ths and 16ths!")
                }
                null -> when (inputBasicLength) {
                    BasicRhythmicLength.WHOLE, BasicRhythmicLength.HALF -> R.drawable.ic_rest_half
                    BasicRhythmicLength.QUARTER -> R.drawable.ic_rest_quarter
                    BasicRhythmicLength.EIGHTH -> R.drawable.ic_rest_eighth
                    BasicRhythmicLength.SIXTEENTH -> R.drawable.ic_rest_sixteenth
                }
            }
        )

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
                        if (inputIsDotted) RhythmicLength(
                            inputBasicLength,
                            LengthModifier.DOTTED
                        )
                        else RhythmicLength(inputBasicLength)
                    val currentInputNoteHeadType = inputNoteHeadType

                    if (currentInputNoteHeadType == null) {
                        currentScoreEditingLayout.bar.addRest(voiceNum, inputLength, intervalIdx)
                    } else {
                        currentScoreEditingLayout.bar.addNote(
                            voiceNum,
                            inputLength,
                            currentInputNoteHeadType,
                            musicHeight,
                            intervalIdx
                        )
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
                            if (inputIsDotted) RhythmicLength(
                                inputBasicLength,
                                LengthModifier.DOTTED
                            )
                            else RhythmicLength(inputBasicLength)
                    currentScoreEditingLayout.bar.addNote(
                        voiceNum,
                        inputLength,
                        currentInputNoteHeadType,
                        musicHeight,
                        0
                    )

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

    fun deleteBar(){
        val currentScoreEditingLayout = scoreEditingLayout
                ?: throw IllegalStateException("Can't edit score bars because scoreEditingLayout is null!")
        if (currentScoreEditingLayout.bars.size <= 1){
            throw IllegalStateException("Deletion should be disabled if only one bar remains.")
        }

        if (currentScoreEditingLayout.barIdx == 0){
            // go to next bar and delete
            for (i in (currentScoreEditingLayout.barIdx + 1) until currentScoreEditingLayout.bars.size){
                currentScoreEditingLayout.bars[i].barNr--
            }
            nextBar(View(this))
            currentScoreEditingLayout.bars.removeAt(0)
        }
        else {
            for (i in (currentScoreEditingLayout.barIdx) until currentScoreEditingLayout.bars.size){
                currentScoreEditingLayout.bars[i].barNr--
            }
            previousBar(View(this))
            if (currentScoreEditingLayout.bars.size >= currentScoreEditingLayout.barIdx + 2) {
                currentScoreEditingLayout.bars.removeAt(currentScoreEditingLayout.barIdx + 1)
            }
        }
    }

    fun insertBar(){
        val currentScoreEditingLayout = scoreEditingLayout
                ?: throw IllegalStateException("Can't edit score bars because scoreEditingLayout is null!")
        for (i in (currentScoreEditingLayout.barIdx + 1) until currentScoreEditingLayout.bars.size){
            currentScoreEditingLayout.bars[i].barNr++
        }
        currentScoreEditingLayout.bars.add(
            currentScoreEditingLayout.barIdx + 1, Bar.makeEmpty(
                currentScoreEditingLayout.bar.barNr + 1,
                currentScoreEditingLayout.bar.timeSignature
            )
        )
        nextBar(View(this))
    }

    fun showSetTitleDialog() {
        val currentScoreEditingLayout = scoreEditingLayout
                ?: throw IllegalStateException("Can't get title of score when scoreEditingLayout is null!")
        val dialog = SetTitleDialogFragment(currentScoreEditingLayout.score.title)
        dialog.show(supportFragmentManager, "SetTitleDialog")
    }

    override fun saveWithTitle(title: String) {
        val currentScoreEditingLayout = scoreEditingLayout
                ?: throw IllegalStateException("Can't change score title if scoreEditingLayout is null!")
        currentScoreEditingLayout.score.title = title

        // save to cloud with changed title

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "$title.json")
        }
        startActivityForResult(intent, CREATE_FILE)
    }

    fun saveToCloudWithMessage() {
        if (auth.currentUser == null) {
            login()
            return
        }
        val currentScoreEditingLayout = scoreEditingLayout
                ?: throw IllegalStateException("Can't change score title if scoreEditingLayout is null!")
        val user = auth.currentUser ?: throw IllegalStateException("User must login!")
        val database = Firebase.database.reference
        val myRef = database.child("storage").child(user.uid).child(currentScoreEditingLayout.score.title)

        val score = currentScoreEditingLayout.score;
        val json = parser.setPrettyPrinting().create().toJson(score)
        myRef.setValue(json).addOnSuccessListener {
            println("upload success")
            Toast.makeText(this, "Upload success", Toast.LENGTH_LONG).show()
        }
        .addOnFailureListener {
            // Write failed
            // ...
            println("upload failed")
            Toast.makeText(this, "Upload failed", Toast.LENGTH_LONG).show()
        }
    }

    fun saveToCloud() {
        if (auth.currentUser == null) {
            login()
            return
        }
        AlertDialog.Builder(this@MainActivity)
            .setTitle("Save To Cloud")
            .setMessage("Do you really want to save it to the cloud?")
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton("YES",
                DialogInterface.OnClickListener { dialog, id ->
                    isCloudFile = true
                    saveToCloudWithMessage()
                })
            .setNegativeButton("NO", null).show()
    }

    fun saveToCloudAuto() {
        val currentScoreEditingLayout = scoreEditingLayout
            ?: throw IllegalStateException("Can't change score title if scoreEditingLayout is null!")
        val user = auth.currentUser ?: throw IllegalStateException("User must login!")
        val database = Firebase.database.reference
        val myRef = database.child("storage").child(user.uid).child(currentScoreEditingLayout.score.title)

        val score = currentScoreEditingLayout.score;
        val json = parser.setPrettyPrinting().create().toJson(score)
        myRef.setValue(json)
    }

    fun loadFromCloud(title: String) {
        val user = auth.currentUser ?: throw IllegalStateException("User must login!")

        val database = Firebase.database.reference
        cloudFile = database.child("storage").child(user.uid).child(title)

        val fileListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get Post object and use the values to update the UI
                val json = dataSnapshot.getValue<String>()
                if (json == null) {
                    Toast.makeText(this@MainActivity, "Not found in cloud", Toast.LENGTH_LONG).show()
                    return
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Load from file")
                    .setMessage("Do you really want to load cloud version?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("YES",
                        DialogInterface.OnClickListener { dialog, id ->
                            loadFile(json)
                            isCloudFile = true
                        })
                    .setNegativeButton("NO", null).show()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Toast.makeText(applicationContext, "Load from cloud fail!", Toast.LENGTH_LONG).show()
            }
        }
        cloudFile!!.addListenerForSingleValueEvent(fileListener)
    }

    fun loadFile(json: String) {
        val test = parser.create().fromJson(json, Score::class.java)
        if (test == null) {
            Toast.makeText(this, "Format not correct!", Toast.LENGTH_LONG).show()
        }

        val currentScoreEditingLayout = scoreEditingLayout
            ?: throw IllegalStateException("Can't update bar vis if scoreEditingLayout is null!")
        currentScoreEditingLayout.removeAllViews()

        val currentTimeSignatureLayout = timeSignatureLayout
            ?: throw IllegalStateException("Can't change score title if timeSignatureLayout is null!")
        currentTimeSignatureLayout.removeAllViews()

        val mainConstraintLayout = findViewById<ConstraintLayout>(R.id.main)
        mainConstraintLayout.doOnLayout {
            scoreEditingLayout = addScoreEditingLayout(test)
            timeSignatureLayout = addTimeSignatureLayout(test.barList[0].timeSignature)
            getStatusBarHeight()
        }
    }

    fun openCloudFile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            login()
            Toast.makeText(this@MainActivity, "Please login first and then open again", Toast.LENGTH_LONG).show()
        } else {
            val intent = Intent(this, CloudFileListActivity::class.java)
            startActivityForResult(intent, OPEN_CLOUD_LIST)
        }
    }

    fun login() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN)
    }

    fun logout() {
        if (auth.currentUser != null) {
            Firebase.auth.signOut()
            Toast.makeText(this@MainActivity, "Log out!", Toast.LENGTH_LONG).show()
        } else {
            login()
        }
    }

    fun initNetwork() {
        val cm:ConnectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val builder: NetworkRequest.Builder = NetworkRequest.Builder()

        cm.registerNetworkCallback(
            builder.build(),
            object : ConnectivityManager.NetworkCallback() {

                override fun onAvailable(network: Network) {
                    Log.i("MainActivity", "onAvailable!")
                    // check if NetworkCapabilities has TRANSPORT_WIFI
                    networkStatus = true
//                    val isWifi:Boolean = cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                }

                override fun onLost(network: Network) {
                    networkStatus = false
                    Log.i("MainActivity", "onLost!")
                }
            }
        )
    }
    fun startTimer() {
        timer = fixedRateTimer("", false, 0, 60000) {
            if (auth.currentUser != null && networkStatus && isCloudFile) saveToCloudAuto()

        }
    }
    fun endTimer() {
        timer.cancel()
    }

    fun showBarsModificationDialog(){
        val currentScoreEditingLayout = scoreEditingLayout
            ?: throw IllegalStateException("Can't check if only one bar exists when scoreEditingLayout is null")
        val deleteEnabled = currentScoreEditingLayout.bars.size > 1
        val dialogFrag = DeleteInsertBarDialogFragment(deleteEnabled)
        dialogFrag.show(supportFragmentManager, "barsModificationDialog")
    }

    override fun onBarDeleteButtonClick() {
        deleteBar()
    }

    override fun onBarInsertButtonClick() {
        insertBar()
    }
}