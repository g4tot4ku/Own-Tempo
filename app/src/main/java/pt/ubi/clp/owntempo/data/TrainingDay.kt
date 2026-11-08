package pt.ubi.clp.owntempo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_day")
data class TrainingDay(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,                    // LocalDate.toEpochDay()
    val category: String,              // key of a predefined Category, e.g. "gym", "running"
    val routine: String? = null,       // optional routine for categories with exercises, e.g. "Legs"
    val notes: String? = null,
    val durationMinutes: Int? = null,  // activity time
    val distanceKm: Double? = null,
    val calories: Int? = null
)