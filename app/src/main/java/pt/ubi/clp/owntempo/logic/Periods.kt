package pt.ubi.clp.owntempo.logic

import java.time.LocalDate

// How the main screen shows the trainings
enum class ViewMode { MONTH, WEEK }

// First date drawn for the period that contains `anchor`: the Monday of its week or, in the
// month view, the Monday of the week that contains the 1st of the month
fun gridStart(mode: ViewMode, anchor: LocalDate): LocalDate = when (mode) {
    ViewMode.WEEK -> mondayOf(anchor)
    ViewMode.MONTH -> mondayOf(anchor.withDayOfMonth(1))
}

// Number of days drawn: 7 for a week, 42 (six rows of seven) for a month
fun gridLength(mode: ViewMode): Int = if (mode == ViewMode.WEEK) 7 else 42

// Moves the anchor `steps` periods (weeks or months) forward, or back if negative
fun shiftAnchor(mode: ViewMode, anchor: LocalDate, steps: Long): LocalDate = when (mode) {
    ViewMode.WEEK -> anchor.plusWeeks(steps)
    ViewMode.MONTH -> anchor.plusMonths(steps)
}