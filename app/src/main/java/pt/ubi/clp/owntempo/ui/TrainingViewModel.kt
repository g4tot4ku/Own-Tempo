package pt.ubi.clp.owntempo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import pt.ubi.clp.owntempo.R
import pt.ubi.clp.owntempo.data.AppDatabase
import pt.ubi.clp.owntempo.data.AppSettings
import pt.ubi.clp.owntempo.data.Exercise
import pt.ubi.clp.owntempo.data.ExerciseSet
import pt.ubi.clp.owntempo.data.ExerciseWithSets
import pt.ubi.clp.owntempo.data.Suggestions
import pt.ubi.clp.owntempo.data.TrainingDay
import pt.ubi.clp.owntempo.data.TrainingRepository
import pt.ubi.clp.owntempo.logic.StreakCalculator
import pt.ubi.clp.owntempo.logic.StreakInfo
import pt.ubi.clp.owntempo.logic.ViewMode
import pt.ubi.clp.owntempo.logic.gridLength
import pt.ubi.clp.owntempo.logic.gridStart
import pt.ubi.clp.owntempo.logic.shiftAnchor
import java.time.LocalDate
import java.time.YearMonth

// What the main screen draws: the visible period and the trainings inside it
data class CalendarState(
    val mode: ViewMode,
    val anchor: LocalDate,      // any date inside the visible period
    val rangeStart: LocalDate,  // first date drawn (always a Monday)
    val trainings: List<TrainingDay>
)

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TrainingRepository(AppDatabase.getInstance(application))
    private val settings = AppSettings(application)

    // The app opens in the start view of the settings (month by default) on today's date
    private val mode = MutableStateFlow(settings.startView)
    private val anchor = MutableStateFlow(LocalDate.now())
    private val weeklyGoal = MutableStateFlow(settings.weeklyGoal)

    val currentMode: ViewMode get() = mode.value

    // Emits the visible period and its trainings every time the period or the data changes
    val calendar: Flow<CalendarState> = combine(mode, anchor) { m, a -> m to a }
        .flatMapLatest { (m, a) ->
            val start = gridStart(m, a)
            val end = start.plusDays(gridLength(m) - 1L)
            repository.getDaysInRange(start.toEpochDay(), end.toEpochDay())
                .map { days -> CalendarState(m, a, start, days) }
        }

    // Recalculated when a training changes or when the weekly goal setting changes
    val streak: Flow<StreakInfo> = combine(repository.getAllDays(), weeklyGoal) { days, goal ->
        StreakCalculator.calculate(days.map { it.date }, goal = goal)
    }

    // One-shot error messages (string resource ids) for the screens to display
    private val _errors = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val errors: SharedFlow<Int> = _errors

    // Runs a database operation and reports a failure instead of crashing the app
    private fun runSafely(block: suspend () -> Unit) = viewModelScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e // never swallow coroutine cancellation
        } catch (e: Exception) {
            _errors.emit(R.string.error_database)
        }
    }

    fun setMode(newMode: ViewMode) {
        // Back on the current month: land on today, so the week view shows the current week
        if (YearMonth.from(anchor.value) == YearMonth.now()) anchor.value = LocalDate.now()
        mode.value = newMode
    }

    fun goPrevious() { anchor.value = shiftAnchor(mode.value, anchor.value, -1) }
    fun goNext() { anchor.value = shiftAnchor(mode.value, anchor.value, 1) }
    fun goToday() { anchor.value = LocalDate.now() }

    // Called when the main screen comes back from the settings
    fun refreshSettings() { weeklyGoal.value = settings.weeklyGoal }

    fun getDayById(id: Int) = repository.getDayById(id)
    fun getDaysForDate(date: Long) = repository.getDaysForDate(date)
    fun getExercisesWithSets(dayId: Int) = repository.getExercisesWithSets(dayId)

    // Routines used before, for the day dialog (empty if the read fails)
    suspend fun loadSuggestions(): Suggestions =
        try {
            repository.getSuggestions()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Suggestions()
        }

    // Creates the day (copying the last session of the same routine, if any) and reports its id
    fun createDay(day: TrainingDay, onCreated: (Int) -> Unit) = runSafely {
        onCreated(repository.createDay(day).toInt())
    }

    fun updateDay(day: TrainingDay) = runSafely { repository.updateDay(day) }
    fun deleteDay(day: TrainingDay) = runSafely { repository.deleteDay(day) }

    fun addExercise(
        dayId: Int,
        name: String,
        sets: Int?,
        reps: Int?,
        weightKg: Double?,
        durationSeconds: Int? = null
    ) = runSafely {
        repository.addExercise(dayId, name, sets, reps, weightKg, durationSeconds)
    }

    fun updateExerciseDetails(
        item: ExerciseWithSets,
        name: String,
        sets: Int?,
        reps: Int?,
        weightKg: Double?,
        durationSeconds: Int? = null
    ) = runSafely {
        repository.updateExerciseDetails(item, name, sets, reps, weightKg, durationSeconds)
    }

    fun renameExercise(exercise: Exercise, name: String) =
        runSafely { repository.updateExercise(exercise.copy(name = name)) }

    fun deleteExercise(exercise: Exercise) = runSafely { repository.deleteExercise(exercise) }

    fun reorderExercises(items: List<ExerciseWithSets>) =
        runSafely { repository.reorderExercises(items) }

    fun updateSet(exerciseSet: ExerciseSet) = runSafely { repository.updateSet(exerciseSet) }
    fun addSet(item: ExerciseWithSets) = runSafely { repository.addSet(item) }
    fun removeLastSet(item: ExerciseWithSets) = runSafely { repository.removeLastSet(item) }
}