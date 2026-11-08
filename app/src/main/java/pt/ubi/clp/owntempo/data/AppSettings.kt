package pt.ubi.clp.owntempo.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import pt.ubi.clp.owntempo.logic.StreakCalculator
import pt.ubi.clp.owntempo.logic.ViewMode

// Day/night choice: follow the device, or force light or dark
enum class ThemeMode(val key: String, val nightMode: Int) {
    SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO),
    DARK("dark", AppCompatDelegate.MODE_NIGHT_YES);

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

// User settings, kept in SharedPreferences (settings only; the trainings live in Room)
class AppSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = ThemeMode.fromKey(prefs.getString(KEY_THEME, null))
        set(value) { prefs.edit().putString(KEY_THEME, value.key).apply() }

    // View the app opens with (month by default)
    var startView: ViewMode
        get() = ViewMode.entries.firstOrNull { it.name == prefs.getString(KEY_START_VIEW, null) }
            ?: ViewMode.MONTH
        set(value) { prefs.edit().putString(KEY_START_VIEW, value.name).apply() }

    // Trainings needed in a week to keep the streak (1 to 7)
    var weeklyGoal: Int
        get() = prefs.getInt(KEY_WEEKLY_GOAL, StreakCalculator.WEEKLY_GOAL).coerceIn(1, 7)
        set(value) { prefs.edit().putInt(KEY_WEEKLY_GOAL, value.coerceIn(1, 7)).apply() }

    // Kilograms added or removed by the + and - buttons of a set
    var weightStep: Double
        get() = prefs.getFloat(KEY_WEIGHT_STEP, DEFAULT_WEIGHT_STEP).toDouble()
        set(value) { prefs.edit().putFloat(KEY_WEIGHT_STEP, value.toFloat()).apply() }

    private companion object {
        const val PREFS_NAME = "own_tempo_settings"
        const val KEY_THEME = "theme_mode"
        const val KEY_START_VIEW = "start_view"
        const val KEY_WEEKLY_GOAL = "weekly_goal"
        const val KEY_WEIGHT_STEP = "weight_step"
        const val DEFAULT_WEIGHT_STEP = 2.5f
    }
}