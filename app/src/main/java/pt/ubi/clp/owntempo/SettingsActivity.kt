package pt.ubi.clp.owntempo

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import pt.ubi.clp.owntempo.data.AppSettings
import pt.ubi.clp.owntempo.data.ThemeMode
import pt.ubi.clp.owntempo.logic.ViewMode

class SettingsActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.settings_title)

        settings = AppSettings(this)
        setUpTheme()
        setUpStartView()
        setUpWeeklyGoal()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // Day / night / automatic. Applying the mode recreates the open screens with the new colors.
    private fun setUpTheme() {
        val group = findViewById<MaterialButtonToggleGroup>(R.id.toggleTheme)
        group.check(
            when (settings.themeMode) {
                ThemeMode.SYSTEM -> R.id.buttonThemeSystem
                ThemeMode.LIGHT -> R.id.buttonThemeLight
                ThemeMode.DARK -> R.id.buttonThemeDark
            }
        )
        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.buttonThemeLight -> ThemeMode.LIGHT
                R.id.buttonThemeDark -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
            if (mode != settings.themeMode) {
                settings.themeMode = mode
                AppCompatDelegate.setDefaultNightMode(mode.nightMode)
            }
        }
    }

    // View the app opens with: month (default) or week
    private fun setUpStartView() {
        val group = findViewById<MaterialButtonToggleGroup>(R.id.toggleStartView)
        group.check(if (settings.startView == ViewMode.WEEK) R.id.buttonStartWeek else R.id.buttonStartMonth)
        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                settings.startView = if (checkedId == R.id.buttonStartWeek) ViewMode.WEEK else ViewMode.MONTH
            }
        }
    }

    // Trainings needed in a week to keep the streak
    private fun setUpWeeklyGoal() {
        val slider = findViewById<Slider>(R.id.sliderGoal)
        val valueText = findViewById<TextView>(R.id.textGoalValue)
        fun showGoal(goal: Int) {
            valueText.text = resources.getQuantityString(R.plurals.settings_goal_value, goal, goal)
        }
        slider.value = settings.weeklyGoal.toFloat()
        showGoal(settings.weeklyGoal)
        slider.addOnChangeListener { _, value, _ ->
            settings.weeklyGoal = value.toInt()
            showGoal(value.toInt())
        }
    }
}