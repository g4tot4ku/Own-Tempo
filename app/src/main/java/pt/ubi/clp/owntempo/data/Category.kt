package pt.ubi.clp.owntempo.data

import androidx.annotation.StringRes
import pt.ubi.clp.owntempo.R

// Fields that a category can record for an activity (shown in the day dialog)
enum class Metric { DURATION, DISTANCE, CALORIES }

// Chip colors: each category takes the color at its position in the enum
private val CATEGORY_COLORS = listOf(
    R.color.category_0, R.color.category_1, R.color.category_2, R.color.category_3,
    R.color.category_4, R.color.category_5, R.color.category_6, R.color.category_7,
    R.color.category_8, R.color.category_9
)

// Predefined training categories. To add one, add an entry here and its label in strings.xml.
// `key` is what is stored in the database, so never change the key of an existing entry.
enum class Category(
    val key: String,
    @StringRes val labelRes: Int,
    val hasExercises: Boolean, // exercises with per-set reps and weight (gym style) plus a routine
    val metrics: List<Metric>  // activity fields: time, distance, calories
) {
    GYM("gym", R.string.category_gym, hasExercises = true, metrics = emptyList()),
    RUNNING("running", R.string.category_running, false, listOf(Metric.DISTANCE, Metric.DURATION, Metric.CALORIES)),
    BOX("box", R.string.category_box, true, listOf(Metric.DURATION, Metric.CALORIES)),
    SOCCER("soccer", R.string.category_soccer, false, listOf(Metric.DURATION, Metric.CALORIES)),
    TENNIS("tenis", R.string.category_tennis, false, listOf(Metric.DURATION, Metric.CALORIES)),
    HIKING("hiking", R.string.category_hiking, false, listOf(Metric.DISTANCE, Metric.DURATION, Metric.CALORIES)),
    BASKETBALL("basket", R.string.category_basketball, false, listOf(Metric.DURATION, Metric.CALORIES)),
    PILATES("pilates", R.string.category_pilates, false, listOf(Metric.DURATION, Metric.CALORIES));

    // Color of the category chip in the calendar and in the lists
    val colorRes: Int get() = CATEGORY_COLORS[ordinal % CATEGORY_COLORS.size]

    companion object {
        // Unknown keys fall back to Gym so a removed or renamed category never crashes the app
        fun fromKey(key: String?): Category = entries.firstOrNull { it.key == key } ?: GYM
    }
}

// The predefined category of a stored day
val TrainingDay.categoryType: Category
    get() = Category.fromKey(category)