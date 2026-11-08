package pt.ubi.clp.owntempo

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import pt.ubi.clp.owntempo.logic.StreakInfo
import pt.ubi.clp.owntempo.logic.ViewMode
import pt.ubi.clp.owntempo.ui.CalendarState
import pt.ubi.clp.owntempo.ui.DayAdapter
import pt.ubi.clp.owntempo.ui.DayItem
import pt.ubi.clp.owntempo.ui.MonthAdapter
import pt.ubi.clp.owntempo.ui.MonthCell
import pt.ubi.clp.owntempo.ui.TrainingViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val viewModel: TrainingViewModel by viewModels()
    private lateinit var monthAdapter: MonthAdapter
    private lateinit var weekAdapter: DayAdapter
    private lateinit var modeToggle: MaterialButtonToggleGroup
    private lateinit var periodTitle: TextView
    private lateinit var weekdayHeader: LinearLayout
    private lateinit var monthGrid: RecyclerView
    private lateinit var weekList: RecyclerView
    private lateinit var streakCurrent: TextView
    private lateinit var streakBest: TextView
    private lateinit var streakProgress: TextView
    private lateinit var weekProgress: LinearProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_settings) {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            } else {
                false
            }
        }

        modeToggle = findViewById(R.id.toggleView)
        periodTitle = findViewById(R.id.textPeriodTitle)
        weekdayHeader = findViewById(R.id.layoutWeekdays)
        monthGrid = findViewById(R.id.recyclerMonth)
        weekList = findViewById(R.id.recyclerWeek)
        streakCurrent = findViewById(R.id.textStreakCurrent)
        streakBest = findViewById(R.id.textStreakBest)
        streakProgress = findViewById(R.id.textStreakProgress)
        weekProgress = findViewById(R.id.progressWeek)

        // Month / Week switch
        modeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val newMode = if (checkedId == R.id.buttonModeWeek) ViewMode.WEEK else ViewMode.MONTH
            if (newMode != viewModel.currentMode) viewModel.setMode(newMode)
        }
        findViewById<View>(R.id.buttonPrev).setOnClickListener { viewModel.goPrevious() }
        findViewById<View>(R.id.buttonNext).setOnClickListener { viewModel.goNext() }
        findViewById<View>(R.id.buttonToday).setOnClickListener { viewModel.goToday() }

        buildWeekdayHeader()

        monthAdapter = MonthAdapter { cell -> onDateClicked(cell.date) }
        monthGrid.layoutManager = GridLayoutManager(this, 7)
        monthGrid.adapter = monthAdapter

        weekAdapter = DayAdapter { item -> onDateClicked(item.date) }
        weekList.layoutManager = LinearLayoutManager(this)
        weekList.adapter = weekAdapter

        // Collect the flows only while the screen is visible
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.calendar.collect { state -> showCalendar(state) } }
                launch { viewModel.streak.collect { info -> showStreak(info) } }
                launch { viewModel.errors.collect { Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show() } }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The weekly goal may have changed in the settings
        viewModel.refreshSettings()
    }

    // Mon, Tue, ... header of the month grid (localized)
    private fun buildWeekdayHeader() {
        val locale = Locale.getDefault()
        for (day in DayOfWeek.values()) {
            val label = TextView(this).apply {
                text = day.getDisplayName(TextStyle.SHORT, locale)
                gravity = Gravity.CENTER
                textSize = 12f
                setTextColor(getColor(R.color.md_on_surface_variant))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            weekdayHeader.addView(label)
        }
    }

    private fun showCalendar(state: CalendarState) {
        // Keep the switch in sync with the state (for example the start view of the settings)
        val wanted = if (state.mode == ViewMode.WEEK) R.id.buttonModeWeek else R.id.buttonModeMonth
        if (modeToggle.checkedButtonId != wanted) modeToggle.check(wanted)

        val isMonth = state.mode == ViewMode.MONTH
        monthGrid.visibility = if (isMonth) View.VISIBLE else View.GONE
        weekdayHeader.visibility = if (isMonth) View.VISIBLE else View.GONE
        weekList.visibility = if (isMonth) View.GONE else View.VISIBLE

        val locale = Locale.getDefault()
        val byDate = state.trainings.groupBy { it.date }

        if (isMonth) {
            periodTitle.text = state.anchor
                .format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))
                .replaceFirstChar { it.uppercase() }

            // Always 42 cells (six weeks); days outside the shown month are dimmed
            val month = YearMonth.from(state.anchor)
            val cells = (0 until 42).map { offset ->
                val date = state.rangeStart.plusDays(offset.toLong())
                MonthCell(date, YearMonth.from(date) == month, byDate[date.toEpochDay()].orEmpty())
            }
            monthAdapter.submitList(cells)
        } else {
            val formatter = DateTimeFormatter.ofPattern("d MMM", locale)
            periodTitle.text = getString(
                R.string.week_range,
                state.rangeStart.format(formatter),
                state.rangeStart.plusDays(6).format(formatter)
            )

            // Always 7 rows, matching each date with its saved trainings (if any)
            val items = (0 until 7).map { offset ->
                val date = state.rangeStart.plusDays(offset.toLong())
                DayItem(date, byDate[date.toEpochDay()].orEmpty())
            }
            weekAdapter.submitList(items)
        }
    }

    private fun showStreak(info: StreakInfo) {
        streakCurrent.text = if (info.currentStreak > 0) {
            getString(R.string.streak_current, info.currentStreak)
        } else {
            getString(R.string.streak_none)
        }
        streakBest.text = getString(R.string.streak_best, info.bestStreak)
        streakProgress.text = getString(R.string.streak_progress, info.thisWeekCount, info.weeklyGoal)
        weekProgress.max = info.weeklyGoal
        weekProgress.setProgressCompat(info.thisWeekCount.coerceAtMost(info.weeklyGoal), true)
    }

    // Opens the day overview screen for the selected date, listing all workout sessions on that day
    private fun onDateClicked(date: LocalDate) {
        startActivity(DayOverviewActivity.newIntent(this, date.toEpochDay()))
    }
}