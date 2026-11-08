package pt.ubi.clp.owntempo.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import pt.ubi.clp.owntempo.R
import pt.ubi.clp.owntempo.data.Category
import pt.ubi.clp.owntempo.data.ExerciseWithSets
import pt.ubi.clp.owntempo.data.Metric
import pt.ubi.clp.owntempo.data.Suggestions
import pt.ubi.clp.owntempo.data.TrainingDay
import pt.ubi.clp.owntempo.data.categoryType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Values collected by the day dialog
data class DayInput(
    val category: Category,
    val routine: String?,
    val notes: String?,
    val durationMinutes: Int?,
    val distanceKm: Double?,
    val calories: Int?
) {
    // A new day on the given date
    fun toNewDay(date: LocalDate) = TrainingDay(
        date = date.toEpochDay(),
        category = category.key,
        routine = routine,
        notes = notes,
        durationMinutes = durationMinutes,
        distanceKm = distanceKm,
        calories = calories
    )

    // The same values applied to an existing day (keeps its id and date)
    fun applyTo(day: TrainingDay) = day.copy(
        category = category.key,
        routine = routine,
        notes = notes,
        durationMinutes = durationMinutes,
        distanceKm = distanceKm,
        calories = calories
    )
}

// Dialog to create a training day or edit an existing one. The category is chosen from the
// predefined list and decides which fields are shown (routine, distance, time, calories).
object DayDialog {

    fun show(
        context: Context,
        date: LocalDate,
        existing: TrainingDay?,
        suggestions: Suggestions,
        onSave: (DayInput) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_day, null)
        val categorySpinner = view.findViewById<Spinner>(R.id.spinnerCategory)
        val routineInput = view.findViewById<AutoCompleteTextView>(R.id.inputRoutine)
        val distanceInput = view.findViewById<EditText>(R.id.inputDistance)
        val layoutDuration = view.findViewById<View>(R.id.layoutDuration)
        val durationHoursInput = view.findViewById<EditText>(R.id.inputDurationHours)
        val durationMinutesInput = view.findViewById<EditText>(R.id.inputDurationMinutes)
        val caloriesInput = view.findViewById<EditText>(R.id.inputCalories)
        val notesInput = view.findViewById<EditText>(R.id.inputNotes)

        // Predefined categories (see Category.kt). The category of an existing day cannot change.
        val categories = Category.entries
        val labelAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            categories.map { context.getString(it.labelRes) }
        )
        labelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = labelAdapter
        categorySpinner.setSelection(categories.indexOf(existing?.categoryType ?: categories.first()))
        categorySpinner.isEnabled = existing == null

        routineInput.setText(existing?.routine ?: "")
        existing?.distanceKm?.let { distanceInput.setText(formatNumber(it)) }
        existing?.durationMinutes?.let { dur ->
            val h = dur / 60
            val m = dur % 60
            if (h > 0) durationHoursInput.setText(h.toString())
            if (m > 0) durationMinutesInput.setText(m.toString())
        }
        existing?.calories?.let { caloriesInput.setText(it.toString()) }
        notesInput.setText(existing?.notes ?: "")

        fun selectedCategory() = categories[categorySpinner.selectedItemPosition]

        // Show only the fields that the selected category uses
        fun updateFields() {
            val category = selectedCategory()
            routineInput.visibility = if (category.hasExercises) View.VISIBLE else View.GONE
            distanceInput.visibility = if (Metric.DISTANCE in category.metrics) View.VISIBLE else View.GONE
            layoutDuration.visibility = if (Metric.DURATION in category.metrics) View.VISIBLE else View.GONE
            caloriesInput.visibility = if (Metric.CALORIES in category.metrics) View.VISIBLE else View.GONE
            routineInput.setAdapter(
                suggestionAdapter(context, suggestions.routinesByCategory[category.key].orEmpty())
            )
        }
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, itemView: View?, position: Int, id: Long) =
                updateFields()

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        updateFields()
        routineInput.showSuggestionsOnFocus()

        val dateText = date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
        val dialog = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.dialog_title, dateText))
            .setView(view)
            .setPositiveButton(R.string.save, null) // overridden below so the dialog stays open on errors
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val category = selectedCategory()

                val distanceText = distanceInput.text.toString().trim()
                val distance = distanceText.replace(',', '.').toDoubleOrNull() // accept "5,2" too

                val hoursText = durationHoursInput.text.toString().trim()
                val minsText = durationMinutesInput.text.toString().trim()
                val durationHours = hoursText.toIntOrNull() ?: 0
                val durationMins = minsText.toIntOrNull() ?: 0
                val totalDurationMinutes = if (durationHours > 0 || durationMins > 0) (durationHours * 60 + durationMins) else null

                val caloriesText = caloriesInput.text.toString().trim()
                val calories = caloriesText.toIntOrNull()

                // Optional numbers: if the category uses them and they are filled in, they must be positive
                var valid = true
                if (Metric.DISTANCE in category.metrics && distanceText.isNotEmpty() &&
                    (distance == null || distance <= 0.0 || !distance.isFinite())
                ) {
                    distanceInput.error = context.getString(R.string.error_number_invalid)
                    valid = false
                }
                if (Metric.DURATION in category.metrics && (hoursText.isNotEmpty() || minsText.isNotEmpty()) &&
                    (totalDurationMinutes == null || totalDurationMinutes <= 0)
                ) {
                    durationMinutesInput.error = context.getString(R.string.error_number_invalid)
                    valid = false
                }
                if (Metric.CALORIES in category.metrics && caloriesText.isNotEmpty() &&
                    (calories == null || calories <= 0)
                ) {
                    caloriesInput.error = context.getString(R.string.error_number_invalid)
                    valid = false
                }
                if (!valid) return@setOnClickListener

                // Text typed in a field that the category does not use is ignored.
                // Reuse the spelling of an existing routine so "legs" and "Legs" stay the same.
                val typedRoutine = routineInput.text.toString().trim()
                val routine = if (category.hasExercises) {
                    suggestions.routinesByCategory[category.key].orEmpty()
                        .firstOrNull { it.equals(typedRoutine, ignoreCase = true) }
                        ?: typedRoutine.ifEmpty { null }
                } else {
                    null
                }

                onSave(
                    DayInput(
                        category = category,
                        routine = routine,
                        notes = notesInput.text.toString().trim().ifEmpty { null },
                        durationMinutes = if (Metric.DURATION in category.metrics) totalDurationMinutes else null,
                        distanceKm = if (Metric.DISTANCE in category.metrics) distance else null,
                        calories = if (Metric.CALORIES in category.metrics) calories else null
                    )
                )
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun suggestionAdapter(context: Context, items: List<String>) =
        ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, items)

    // Opens the suggestion list when the field is focused or tapped, without typing first
    private fun AutoCompleteTextView.showSuggestionsOnFocus() {
        setOnFocusChangeListener { v, hasFocus -> if (hasFocus) v.post { showSuggestions() } }
        setOnClickListener { showSuggestions() }
    }

    private fun AutoCompleteTextView.showSuggestions() {
        if ((adapter?.count ?: 0) > 0) showDropDown()
    }
}

// Dialog to create an exercise or edit an existing one (name, duration hh:mm:ss, sets, reps, weight)
object ExerciseDialog {

    private const val MAX_SETS = 20

    fun show(
        context: Context,
        existing: ExerciseWithSets?,
        onSave: (name: String, sets: Int?, reps: Int?, weightKg: Double?, durationSeconds: Int?) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_exercise, null)
        val nameInput = view.findViewById<EditText>(R.id.inputExerciseName)
        val hoursInput = view.findViewById<EditText>(R.id.inputExerciseHours)
        val minutesInput = view.findViewById<EditText>(R.id.inputExerciseMinutes)
        val secondsInput = view.findViewById<EditText>(R.id.inputExerciseSeconds)

        val defaultsLayout = view.findViewById<View>(R.id.layoutSetDefaults)
        val setsInput = view.findViewById<EditText>(R.id.inputSets)
        val repsInput = view.findViewById<EditText>(R.id.inputReps)
        val weightInput = view.findViewById<EditText>(R.id.inputWeight)

        nameInput.setText(existing?.exercise?.name ?: "")
        setsInput.setText(existing?.sets?.size?.toString() ?: "")
        repsInput.setText(existing?.orderedSets?.firstOrNull()?.reps?.toString() ?: "")
        weightInput.setText(existing?.orderedSets?.firstOrNull()?.weightKg?.let { formatNumber(it) } ?: "")

        existing?.exercise?.durationSeconds?.let { dur ->
            val h = dur / 3600
            val m = (dur % 3600) / 60
            val s = dur % 60
            if (h > 0) hoursInput.setText(h.toString())
            if (m > 0) minutesInput.setText(m.toString())
            if (s > 0) secondsInput.setText(s.toString())
        }

        defaultsLayout.visibility = View.VISIBLE

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.exercise_dialog_title)
            .setView(view)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = nameInput.text.toString().trim()
                val setsText = setsInput.text.toString().trim()
                val repsText = repsInput.text.toString().trim()
                val weightText = weightInput.text.toString().trim()
                val sets = setsText.toIntOrNull()
                val reps = repsText.toIntOrNull()
                val weight = weightText.replace(',', '.').toDoubleOrNull()

                val hoursText = hoursInput.text.toString().trim()
                val minsText = minutesInput.text.toString().trim()
                val secsText = secondsInput.text.toString().trim()
                val h = hoursText.toIntOrNull() ?: 0
                val m = minsText.toIntOrNull() ?: 0
                val s = secsText.toIntOrNull() ?: 0
                val totalSec = if (h > 0 || m > 0 || s > 0) (h * 3600 + m * 60 + s) else null

                var valid = true
                if (name.isEmpty()) {
                    nameInput.error = context.getString(R.string.error_name_required)
                    valid = false
                }
                if (setsText.isNotEmpty() && (sets == null || sets !in 1..MAX_SETS)) {
                    setsInput.error = context.getString(R.string.error_sets_range)
                    valid = false
                }
                if (repsText.isNotEmpty() && (reps == null || reps <= 0)) {
                    repsInput.error = context.getString(R.string.error_number_invalid)
                    valid = false
                }
                if (weightText.isNotEmpty() && (weight == null || weight <= 0.0 || !weight.isFinite())) {
                    weightInput.error = context.getString(R.string.error_number_invalid)
                    valid = false
                }
                if ((hoursText.isNotEmpty() || minsText.isNotEmpty() || secsText.isNotEmpty()) &&
                    (totalSec == null || totalSec <= 0)
                ) {
                    secondsInput.error = context.getString(R.string.error_number_invalid)
                    valid = false
                }
                if (!valid) return@setOnClickListener

                onSave(name, sets, reps, weight, totalSec)
                dialog.dismiss()
            }
        }
        dialog.show()
    }
}

// Generic "are you sure?" dialog used before deleting anything
object ConfirmDialog {

    fun show(context: Context, title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.delete) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}