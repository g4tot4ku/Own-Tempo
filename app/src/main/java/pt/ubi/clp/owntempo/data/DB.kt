package pt.ubi.clp.owntempo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Sets, reps and weight as they were stored in the exercise row before version 3
private class OldExercise(val id: Int, val sets: Int?, val reps: Int?, val weightKg: Double?)

@Database(
    entities = [TrainingDay::class, Exercise::class, ExerciseSet::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trainingDayDao(): TrainingDayDao
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // v1 -> v2: routine on each day and weight on each exercise (existing data is kept)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE training_day ADD COLUMN routine TEXT")
                db.execSQL("ALTER TABLE exercise ADD COLUMN weightKg REAL")
            }
        }

        // Spellings of the old free-text categories. Frozen here on purpose: a migration must
        // not change its behaviour when the Category enum changes later.
        private val OLD_CATEGORY_ALIASES = mapOf(
            "gym" to listOf("gym", "gimnasio", "ginásio", "ginasio"),
            "running" to listOf("running", "run", "correr", "corrida"),
            "box" to listOf("box", "boxing", "boxe", "boxeo"),
            "soccer" to listOf("soccer", "football", "futebol", "futbol", "fútbol"),
            "tenis" to listOf("tenis", "tennis", "ténis"),
            "hiking" to listOf("hiking", "hike", "senderismo", "caminata", "caminhada"),
            "basket" to listOf("basket", "basketball", "basquete", "baloncesto"),
            "pilates" to listOf("pilates")
        )

        // v2 -> v3: predefined categories, activity fields on the day, and one row per set
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE training_day ADD COLUMN distanceKm REAL")
                db.execSQL("ALTER TABLE training_day ADD COLUMN calories INTEGER")

                val days = mutableListOf<Triple<Int, String, String?>>()
                db.query("SELECT id, category, routine FROM training_day").use { cursor ->
                    while (cursor.moveToNext()) {
                        days += Triple(
                            cursor.getInt(0),
                            cursor.getString(1),
                            if (cursor.isNull(2)) null else cursor.getString(2)
                        )
                    }
                }
                for ((id, oldCategory, routine) in days) {
                    val text = oldCategory.trim()
                    val key = OLD_CATEGORY_ALIASES.entries
                        .firstOrNull { (_, aliases) -> aliases.any { it.equals(text, ignoreCase = true) } }
                        ?.key
                    val newRoutine = if (key == null && routine.isNullOrBlank()) text else routine
                    db.execSQL(
                        "UPDATE training_day SET category = ?, routine = ? WHERE id = ?",
                        arrayOf<Any?>(key ?: "gym", newRoutine, id)
                    )
                }

                val oldExercises = mutableListOf<OldExercise>()
                db.query("SELECT id, sets, reps, weightKg FROM exercise").use { cursor ->
                    while (cursor.moveToNext()) {
                        oldExercises += OldExercise(
                            id = cursor.getInt(0),
                            sets = if (cursor.isNull(1)) null else cursor.getInt(1),
                            reps = if (cursor.isNull(2)) null else cursor.getInt(2),
                            weightKg = if (cursor.isNull(3)) null else cursor.getDouble(3)
                        )
                    }
                }

                db.execSQL(
                    "CREATE TABLE exercise_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "trainingDayId INTEGER NOT NULL, name TEXT NOT NULL, " +
                        "FOREIGN KEY(trainingDayId) REFERENCES training_day(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("INSERT INTO exercise_new (id, trainingDayId, name) SELECT id, trainingDayId, name FROM exercise")
                db.execSQL("DROP TABLE exercise")
                db.execSQL("ALTER TABLE exercise_new RENAME TO exercise")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS exercise_set (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "exerciseId INTEGER NOT NULL, setNumber INTEGER NOT NULL, reps INTEGER, weightKg REAL, " +
                        "FOREIGN KEY(exerciseId) REFERENCES exercise(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )

                for (old in oldExercises) {
                    val count = (old.sets ?: 1).coerceIn(1, 20)
                    for (number in 1..count) {
                        db.execSQL(
                            "INSERT INTO exercise_set (exerciseId, setNumber, reps, weightKg) VALUES (?, ?, ?, ?)",
                            arrayOf<Any?>(old.id, number, old.reps, old.weightKg)
                        )
                    }
                }
            }
        }

        // v3 -> v4: add displayOrder to exercise for drag-and-drop reordering
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v4 -> v5: add durationSeconds to exercise for per-exercise duration tracking
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise ADD COLUMN durationSeconds INTEGER")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "own_tempo_db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { INSTANCE = it }
            }
        }
    }
}