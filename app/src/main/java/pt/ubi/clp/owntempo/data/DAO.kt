package pt.ubi.clp.owntempo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// A category key with one of its routines
data class CategoryRoutine(val category: String, val routine: String?)

@Dao
interface TrainingDayDao {
    @Query("SELECT * FROM training_day ORDER BY date DESC")
    fun getAll(): Flow<List<TrainingDay>>

    @Query("SELECT * FROM training_day WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun getForRange(start: Long, end: Long): Flow<List<TrainingDay>>

    @Query("SELECT * FROM training_day WHERE date = :date ORDER BY id ASC")
    fun getForDate(date: Long): Flow<List<TrainingDay>>

    @Query("SELECT * FROM training_day WHERE id = :id")
    fun getById(id: Int): Flow<TrainingDay?>

    // Every category/routine pair used so far, offered as suggestions when creating a day
    @Query("SELECT DISTINCT category, routine FROM training_day WHERE routine IS NOT NULL")
    suspend fun getCategoryRoutines(): List<CategoryRoutine>

    // Most recent earlier session of the same category and routine (routine ignores upper/lower case)
    @Query("SELECT * FROM training_day WHERE category = :category AND routine = :routine COLLATE NOCASE AND date < :beforeDate ORDER BY date DESC LIMIT 1")
    suspend fun getLastSession(category: String, routine: String, beforeDate: Long): TrainingDay?

    @Insert suspend fun insert(day: TrainingDay): Long
    @Update suspend fun update(day: TrainingDay)
    @Delete suspend fun delete(day: TrainingDay)
}

@Dao
interface ExerciseDao {
    @Transaction
    @Query("SELECT * FROM exercise WHERE trainingDayId = :dayId ORDER BY displayOrder ASC, id ASC")
    fun getExercisesWithSets(dayId: Int): Flow<List<ExerciseWithSets>>

    // One-shot read, used to copy a previous session
    @Transaction
    @Query("SELECT * FROM exercise WHERE trainingDayId = :dayId ORDER BY displayOrder ASC, id ASC")
    suspend fun getExercisesWithSetsOnce(dayId: Int): List<ExerciseWithSets>

    @Insert suspend fun insert(exercise: Exercise): Long
    @Update suspend fun update(exercise: Exercise)
    @Update suspend fun updateExercises(exercises: List<Exercise>)
    @Delete suspend fun delete(exercise: Exercise)

    @Insert suspend fun insertSet(exerciseSet: ExerciseSet)
    @Insert suspend fun insertSets(exerciseSets: List<ExerciseSet>)
    @Update suspend fun updateSet(exerciseSet: ExerciseSet)
    @Delete suspend fun deleteSet(exerciseSet: ExerciseSet)
}