package pt.ubi.clp.owntempo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.launch
import pt.ubi.clp.owntempo.data.AppSettings
import pt.ubi.clp.owntempo.data.ExerciseWithSets
import pt.ubi.clp.owntempo.data.TrainingDay
import pt.ubi.clp.owntempo.data.categoryType
import pt.ubi.clp.owntempo.ui.ConfirmDialog
import pt.ubi.clp.owntempo.ui.DayDialog
import pt.ubi.clp.owntempo.ui.ExerciseAdapter
import pt.ubi.clp.owntempo.ui.ExerciseDialog
import pt.ubi.clp.owntempo.ui.TrainingViewModel
import pt.ubi.clp.owntempo.ui.displayTitle
import pt.ubi.clp.owntempo.ui.formatDuration
import pt.ubi.clp.owntempo.ui.formatNumber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class DayDetailActivity : AppCompatActivity() {

    private val viewModel: TrainingViewModel by viewModels()
    private val settings by lazy { AppSettings(this) }
    private lateinit var adapter: ExerciseAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var metricsText: TextView
    private lateinit var notesText: TextView
    private lateinit var noExercisesText: TextView
    private lateinit var exercisesSection: View
    private lateinit var layoutWeightStep: View

    private var currentWeightStep: Double = 2.5
    private var dayId: Int = -1
    private var currentDay: TrainingDay? = null // latest value emitted by the database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day_detail)

        dayId = intent.getIntExtra(EXTRA_DAY_ID, -1)
        if (dayId == -1) {
            finish()
            return
        }

        // The toolbar shows the day title and date, with a back arrow
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        metricsText = findViewById(R.id.textDetailMetrics)
        notesText = findViewById(R.id.textDetailNotes)
        noExercisesText = findViewById(R.id.textNoExercises)
        exercisesSection = findViewById(R.id.layoutExercises)
        layoutWeightStep = findViewById(R.id.layoutWeightStep)

        currentWeightStep = settings.weightStep
        setUpWeightStepToggle()

        adapter = ExerciseAdapter(
            weightStep = { currentWeightStep },
            onEdit = { item ->
                ExerciseDialog.show(this, item) { name, sets, reps, weight, durationSeconds ->
                    viewModel.updateExerciseDetails(item, name, sets, reps, weight, durationSeconds)
                }
            },
            onDelete = { exercise ->
                ConfirmDialog.show(
                    this,
                    getString(R.string.confirm_delete_exercise_title),
                    getString(R.string.confirm_delete_exercise_message, exercise.name)
                ) { viewModel.deleteExercise(exercise) }
            },
            // Quick +/- adjustments of one set are saved immediately
            onSetChange = { updated -> viewModel.updateSet(updated) },
            onAddSet = { item -> viewModel.addSet(item) },
            onRemoveSet = { item -> confirmRemoveLastSet(item) },
            onStartDrag = { viewHolder -> itemTouchHelper.startDrag(viewHolder) }
        )

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerExercises)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                if (fromPos != RecyclerView.NO_POSITION && toPos != RecyclerView.NO_POSITION) {
                    adapter.onItemMove(fromPos, toPos)
                }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun isLongPressDragEnabled(): Boolean = true

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewModel.reorderExercises(adapter.currentList)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        findViewById<Button>(R.id.buttonAddExercise).setOnClickListener {
            ExerciseDialog.show(this, null) { name, sets, reps, weight, durationSeconds ->
                viewModel.addExercise(dayId, name, sets, reps, weight, durationSeconds)
            }
        }

        findViewById<Button>(R.id.buttonEditDay).setOnClickListener {
            val day = currentDay ?: return@setOnClickListener
            lifecycleScope.launch {
                val suggestions = viewModel.loadSuggestions()
                DayDialog.show(this@DayDetailActivity, LocalDate.ofEpochDay(day.date), day, suggestions) { input ->
                    viewModel.updateDay(input.applyTo(day))
                }
            }
        }

        findViewById<Button>(R.id.buttonDeleteDay).setOnClickListener {
            currentDay?.let { day ->
                ConfirmDialog.show(
                    this,
                    getString(R.string.confirm_delete_day_title),
                    getString(R.string.confirm_delete_day_message)
                ) { viewModel.deleteDay(day) } // exercises and sets are removed too (ForeignKey CASCADE)
            }
        }

        // Observe the day, its exercises and errors only while the screen is visible
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.getDayById(dayId).collect { day ->
                        currentDay = day
                        if (day == null) finish() // the day was deleted, go back to the calendar
                        else showDay(day)
                    }
                }
                launch {
                    viewModel.getExercisesWithSets(dayId).collect { exercises ->
                        adapter.submitList(exercises)
                        val hasExercises = exercises.isNotEmpty()
                        noExercisesText.visibility = if (hasExercises) View.GONE else View.VISIBLE
                        layoutWeightStep.visibility = if (hasExercises) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.errors.collect {
                        Toast.makeText(this@DayDetailActivity, it, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setUpWeightStepToggle() {
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.toggleWeightStep)
        val initialCheckedId = when (currentWeightStep) {
            0.5 -> R.id.buttonStep05
            1.0 -> R.id.buttonStep1
            2.5 -> R.id.buttonStep25
            5.0 -> R.id.buttonStep5
            else -> R.id.buttonStep25
        }
        toggleGroup.check(initialCheckedId)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val step = when (checkedId) {
                R.id.buttonStep05 -> 0.5
                R.id.buttonStep1 -> 1.0
                R.id.buttonStep25 -> 2.5
                R.id.buttonStep5 -> 5.0
                else -> 2.5
            }
            currentWeightStep = step
            settings.weightStep = step
        }
    }

    private fun confirmRemoveLastSet(item: ExerciseWithSets) {
        val last = item.orderedSets.lastOrNull() ?: return
        ConfirmDialog.show(
            this,
            getString(R.string.confirm_remove_set_title),
            getString(R.string.confirm_remove_set_message, last.setNumber, item.exercise.name)
        ) { viewModel.removeLastSet(item) }
    }

    private fun showDay(day: TrainingDay) {
        supportActionBar?.title = day.displayTitle(this)
        supportActionBar?.subtitle = LocalDate.ofEpochDay(day.date)
            .format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.getDefault()))
            .replaceFirstChar { it.uppercase() }

        // Activity fields (distance, time, calories) that were filled in, one per line
        val metrics = mutableListOf<String>()
        day.distanceKm?.let { metrics += getString(R.string.metric_distance, formatNumber(it)) }
        day.durationMinutes?.let { metrics += getString(R.string.metric_duration, formatDuration(it)) }
        day.calories?.let { metrics += getString(R.string.metric_calories, it) }
        metricsText.text = metrics.joinToString("\n")
        metricsText.visibility = if (metrics.isEmpty()) View.GONE else View.VISIBLE

        notesText.text = day.notes.orEmpty()
        notesText.visibility = if (day.notes.isNullOrBlank()) View.GONE else View.VISIBLE

        // Exercises and sets only exist for categories that use them (gym, box)
        exercisesSection.visibility = if (day.categoryType.hasExercises) View.VISIBLE else View.GONE
    }

    companion object {
        private const val EXTRA_DAY_ID = "extra_day_id"

        fun newIntent(context: Context, dayId: Int): Intent =
            Intent(context, DayDetailActivity::class.java).putExtra(EXTRA_DAY_ID, dayId)
    }
}