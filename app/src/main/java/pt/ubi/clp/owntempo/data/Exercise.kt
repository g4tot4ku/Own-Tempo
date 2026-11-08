package pt.ubi.clp.owntempo.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise",
    foreignKeys = [ForeignKey(
        entity = TrainingDay::class,
        parentColumns = ["id"],
        childColumns = ["trainingDayId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val trainingDayId: Int,
    val name: String,
    val displayOrder: Int = 0,
    val durationSeconds: Int? = null
)