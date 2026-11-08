package pt.ubi.clp.owntempo.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pt.ubi.clp.owntempo.R
import pt.ubi.clp.owntempo.data.Exercise
import pt.ubi.clp.owntempo.data.ExerciseSet
import pt.ubi.clp.owntempo.data.ExerciseWithSets

private val EXERCISE_DIFF = object : DiffUtil.ItemCallback<ExerciseWithSets>() {
    override fun areItemsTheSame(oldItem: ExerciseWithSets, newItem: ExerciseWithSets) =
        oldItem.exercise.id == newItem.exercise.id
    override fun areContentsTheSame(oldItem: ExerciseWithSets, newItem: ExerciseWithSets) =
        oldItem == newItem
}

// A missing weight starts from 0 when increased; reaching 0 clears the weight again
private fun stepWeight(current: Double?, delta: Double): Double? =
    ((current ?: 0.0) + delta).takeIf { it > 0 }

// Reps never go below 1; a missing value stays empty when decreased
private fun stepReps(current: Int?, delta: Int): Int? =
    if (current == null && delta < 0) null else ((current ?: 0) + delta).coerceAtLeast(1)

// Each exercise shows one row per set. Tapping the card edits the exercise details (name, duration, sets, reps, weight),
// the +/- buttons change the reps or weight of that single set, and Add/Remove set change the number of sets.
class ExerciseAdapter(
    private val weightStep: () -> Double, // kg per tap, read dynamically on tap
    private val onEdit: (ExerciseWithSets) -> Unit,
    private val onDelete: (Exercise) -> Unit,
    private val onSetChange: (ExerciseSet) -> Unit,
    private val onAddSet: (ExerciseWithSets) -> Unit,
    private val onRemoveSet: (ExerciseWithSets) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : ListAdapter<ExerciseWithSets, ExerciseAdapter.ExerciseViewHolder>(EXERCISE_DIFF) {

    class ExerciseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.textExerciseName)
        val duration: TextView = view.findViewById(R.id.textExerciseDuration)
        val deleteButton: Button = view.findViewById(R.id.buttonDeleteExercise)
        val setsContainer: LinearLayout = view.findViewById(R.id.layoutSets)
        val addSetButton: Button = view.findViewById(R.id.buttonAddSet)
        val removeSetButton: Button = view.findViewById(R.id.buttonRemoveSet)
        val dragHandle: ImageView = view.findViewById(R.id.imageDragHandle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exercise, parent, false)
        return ExerciseViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val item = getItem(position)
        val exercise = item.exercise

        holder.name.text = exercise.name

        if (exercise.durationSeconds != null && exercise.durationSeconds > 0) {
            holder.duration.text = "⏱ " + formatExerciseDuration(exercise.durationSeconds)
            holder.duration.visibility = View.VISIBLE
        } else {
            holder.duration.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onEdit(item) }
        holder.deleteButton.setOnClickListener { onDelete(exercise) }
        holder.addSetButton.setOnClickListener { onAddSet(item) }
        holder.removeSetButton.setOnClickListener { onRemoveSet(item) }
        // The last remaining set cannot be removed (delete the exercise instead)
        holder.removeSetButton.visibility = if (item.sets.size > 1) View.VISIBLE else View.GONE

        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                onStartDrag(holder)
            }
            false
        }

        // Rebuild one row per set
        holder.setsContainer.removeAllViews()
        val inflater = LayoutInflater.from(holder.itemView.context)
        for (exerciseSet in item.orderedSets) {
            val row = inflater.inflate(R.layout.item_set, holder.setsContainer, false)
            bindSetRow(row, exerciseSet)
            holder.setsContainer.addView(row)
        }
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        val list = currentList.toMutableList()
        if (fromPosition in list.indices && toPosition in list.indices) {
            val moved = list.removeAt(fromPosition)
            list.add(toPosition, moved)
            submitList(list)
        }
    }

    private fun bindSetRow(row: View, exerciseSet: ExerciseSet) {
        val context = row.context
        row.findViewById<TextView>(R.id.textSetNumber).text =
            context.getString(R.string.set_label, exerciseSet.setNumber)
        row.findViewById<TextView>(R.id.textWeight).text = exerciseSet.weightKg
            ?.let { context.getString(R.string.weight_value, formatNumber(it)) }
            ?: context.getString(R.string.value_empty)
        row.findViewById<TextView>(R.id.textReps).text = exerciseSet.reps
            ?.let { context.getString(R.string.reps_value, it) }
            ?: context.getString(R.string.value_empty)

        row.findViewById<Button>(R.id.buttonWeightDown).setOnClickListener {
            val step = weightStep()
            onSetChange(exerciseSet.copy(weightKg = stepWeight(exerciseSet.weightKg, -step)))
        }
        row.findViewById<Button>(R.id.buttonWeightUp).setOnClickListener {
            val step = weightStep()
            onSetChange(exerciseSet.copy(weightKg = stepWeight(exerciseSet.weightKg, step)))
        }
        row.findViewById<Button>(R.id.buttonRepsDown).setOnClickListener {
            onSetChange(exerciseSet.copy(reps = stepReps(exerciseSet.reps, -1)))
        }
        row.findViewById<Button>(R.id.buttonRepsUp).setOnClickListener {
            onSetChange(exerciseSet.copy(reps = stepReps(exerciseSet.reps, 1)))
        }
    }
}