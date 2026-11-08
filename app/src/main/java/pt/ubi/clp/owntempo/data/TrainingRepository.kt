package pt.ubi.clp.owntempo.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

// Routines used before, per category key, offered as suggestions when creating a day
data class Suggestions(
    val routinesByCategory: Map<String, List<String>> = emptyMap()
)

class TrainingRepository(private val db: AppDatabase) {
    private val trainingDayDao = db.trainingDayDao()
    private val exerciseDao = db.exerciseDao()

    fun getAllDays(): Flow<List<TrainingDay>> = trainingDayDao.getAll()

    fun getDaysInRange(start: Long, end: Long): Flow<List<TrainingDay>> =
        trainingDayDao.getForRange(start, end)

    fun getDaysForDate(date: Long): Flow<List<TrainingDay>> =
        trainingDayDao.getForDate(date)

    fun getDayById(id: Int): Flow<TrainingDay?> = trainingDayDao.getById(id)

    fun getExercisesWithSets(dayId: Int): Flow<List<ExerciseWithSets>> =
        exerciseDao.getExercisesWithSets(dayId)

    // Creates a day. If its category has exercises and it has a routine, the exercises (with the
    // reps and weight of every set) of the last earlier session with the same category + routine
    // are copied into it, all in one transaction.
    suspend fun createDay(day: TrainingDay): Long = db.withTransaction {
        val newId = trainingDayDao.insert(day)
        val routine = day.routine
        if (day.categoryType.hasExercises && !routine.isNullOrBlank()) {
            val last = trainingDayDao.getLastSession(day.category, routine, day.date)
            if (last != null) {
                for (item in exerciseDao.getExercisesWithSetsOnce(last.id)) {
                    val newExerciseId = exerciseDao.insert(
                        item.exercise.copy(id = 0, trainingDayId = newId.toInt())
                    ).toInt()
                    exerciseDao.insertSets(
                        item.sets.map { it.copy(id = 0, exerciseId = newExerciseId) }
                    )
                }
            }
        }
        newId
    }

    suspend fun updateDay(day: TrainingDay) = trainingDayDao.update(day)
    suspend fun deleteDay(day: TrainingDay) = trainingDayDao.delete(day)

    // Creates an exercise with the given number of sets (1 if not given), all with the same reps and weight
    suspend fun addExercise(
        dayId: Int,
        name: String,
        sets: Int?,
        reps: Int?,
        weightKg: Double?,
        durationSeconds: Int? = null
    ) = db.withTransaction {
        val count = exerciseDao.getExercisesWithSetsOnce(dayId).size
        val exerciseId = exerciseDao.insert(
            Exercise(
                trainingDayId = dayId,
                name = name,
                displayOrder = count,
                durationSeconds = durationSeconds
            )
        ).toInt()
        val newSets = (1..(sets ?: 1)).map { number ->
            ExerciseSet(exerciseId = exerciseId, setNumber = number, reps = reps, weightKg = weightKg)
        }
        exerciseDao.insertSets(newSets)
    }

    suspend fun updateExerciseDetails(
        item: ExerciseWithSets,
        newName: String,
        newSetsCount: Int?,
        newReps: Int?,
        newWeightKg: Double?,
        newDurationSeconds: Int?
    ) = db.withTransaction {
        if (item.exercise.name != newName || item.exercise.durationSeconds != newDurationSeconds) {
            exerciseDao.update(
                item.exercise.copy(
                    name = newName,
                    durationSeconds = newDurationSeconds
                )
            )
        }

        val currentSets = item.orderedSets.map { exerciseSet ->
            exerciseSet.copy(
                reps = newReps ?: exerciseSet.reps,
                weightKg = newWeightKg ?: exerciseSet.weightKg
            )
        }
        for (s in currentSets) {
            exerciseDao.updateSet(s)
        }

        if (newSetsCount != null && newSetsCount > 0 && newSetsCount != currentSets.size) {
            if (newSetsCount > currentSets.size) {
                val lastSet = currentSets.lastOrNull()
                val extraCount = newSetsCount - currentSets.size
                for (i in 1..extraCount) {
                    val setNum = currentSets.size + i
                    exerciseDao.insertSet(
                        ExerciseSet(
                            exerciseId = item.exercise.id,
                            setNumber = setNum,
                            reps = newReps ?: lastSet?.reps,
                            weightKg = newWeightKg ?: lastSet?.weightKg
                        )
                    )
                }
            } else {
                val toRemoveCount = currentSets.size - newSetsCount
                val setsToRemove = currentSets.takeLast(toRemoveCount)
                for (s in setsToRemove) {
                    exerciseDao.deleteSet(s)
                }
            }
        }
    }

    suspend fun updateExercise(exercise: Exercise) = exerciseDao.update(exercise)
    suspend fun deleteExercise(exercise: Exercise) = exerciseDao.delete(exercise)

    suspend fun reorderExercises(items: List<ExerciseWithSets>) = db.withTransaction {
        val updated = items.mapIndexed { idx, item ->
            item.exercise.copy(displayOrder = idx)
        }
        exerciseDao.updateExercises(updated)
    }

    suspend fun updateSet(exerciseSet: ExerciseSet) = exerciseDao.updateSet(exerciseSet)

    // New set: copies the reps and weight of the last set, which is usually what comes next
    suspend fun addSet(item: ExerciseWithSets) {
        val last = item.orderedSets.lastOrNull()
        exerciseDao.insertSet(
            ExerciseSet(
                exerciseId = item.exercise.id,
                setNumber = (last?.setNumber ?: 0) + 1,
                reps = last?.reps,
                weightKg = last?.weightKg
            )
        )
    }

    suspend fun removeLastSet(item: ExerciseWithSets) {
        item.orderedSets.lastOrNull()?.let { exerciseDao.deleteSet(it) }
    }

    suspend fun getSuggestions(): Suggestions {
        val routines = trainingDayDao.getCategoryRoutines()
            .filter { it.routine != null && it.routine.isNotBlank() }
            .groupBy({ it.category }, { it.routine.orEmpty() })
            .mapValues { (_, names) -> names.distinctBy { it.lowercase() }.sortedBy { it.lowercase() } }
        return Suggestions(routines)
    }
}