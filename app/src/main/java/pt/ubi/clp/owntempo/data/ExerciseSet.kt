package pt.ubi.clp.owntempo.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

// One set of an exercise, with its own reps and weight (e.g. set 1: 12 x 50 kg)
@Entity(
    tableName = "exercise_set",
    foreignKeys = [ForeignKey(
        entity = Exercise::class,
        parentColumns = ["id"],
        childColumns = ["exerciseId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseId: Int,
    val setNumber: Int, // 1-based order of the set inside the exercise
    val reps: Int? = null,
    val weightKg: Double? = null
)

// An exercise together with its sets, as shown on the day detail screen
data class ExerciseWithSets(
    @Embedded val exercise: Exercise,
    @Relation(parentColumn = "id", entityColumn = "exerciseId")
    val sets: List<ExerciseSet>
) {
    // Sets in display order (Room does not guarantee the order of a relation)
    val orderedSets: List<ExerciseSet> get() = sets.sortedBy { it.setNumber }
}