package pt.ubi.clp.owntempo.logic

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    // Fixed "today": Wednesday 4 Nov 2026 (its week starts on Monday 2 Nov)
    private val today = LocalDate.of(2026, 11, 4)

    // Helper: epoch day of a date in Oct/Nov 2026
    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day).toEpochDay()

    @Test
    fun noTrainings_givesEmptyStreak() {
        val info = StreakCalculator.calculate(emptyList(), today)
        assertEquals(0, info.currentStreak)
        assertEquals(0, info.bestStreak)
        assertEquals(0, info.thisWeekCount)
    }

    @Test
    fun goalReachedThisWeek_countsAsOneWeek() {
        val info = StreakCalculator.calculate(listOf(d(11, 2), d(11, 3), d(11, 4)), today)
        assertEquals(1, info.currentStreak)
        assertEquals(3, info.thisWeekCount)
    }

    @Test
    fun weekInProgress_doesNotBreakStreak() {
        // Previous week reached the goal; this week has only 1 training so far
        val info = StreakCalculator.calculate(listOf(d(10, 26), d(10, 27), d(10, 28), d(11, 2)), today)
        assertEquals(1, info.currentStreak)
        assertEquals(1, info.thisWeekCount)
    }

    @Test
    fun missedWeek_breaksCurrentStreakButKeepsBest() {
        // Goal reached on weeks of 12 Oct and 19 Oct, missed 26 Oct, reached again this week
        val days = listOf(
            d(10, 12), d(10, 13), d(10, 14),
            d(10, 19), d(10, 20), d(10, 21),
            d(11, 2), d(11, 3), d(11, 4)
        )
        val info = StreakCalculator.calculate(days, today)
        assertEquals(1, info.currentStreak)
        assertEquals(2, info.bestStreak)
    }

    @Test
    fun futureDays_doNotCount() {
        val info = StreakCalculator.calculate(listOf(d(11, 4), d(11, 5), d(11, 6), d(11, 7)), today)
        assertEquals(1, info.thisWeekCount)
        assertEquals(0, info.currentStreak)
    }
}