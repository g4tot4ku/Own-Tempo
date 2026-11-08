package pt.ubi.clp.owntempo.logic

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

// Monday of the week that contains the given date (weeks run from Monday to Sunday)
fun mondayOf(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

data class StreakInfo(
    val currentStreak: Int, // consecutive weeks that reached the goal
    val bestStreak: Int,    // longest run of such weeks ever
    val thisWeekCount: Int, // trainings logged so far this week
    val weeklyGoal: Int
)

object StreakCalculator {

    // Minimum number of trainings for a week to count towards the streak
    const val WEEKLY_GOAL = 3

    fun calculate(
        trainingDays: List<Long>, // epoch day of every logged training
        today: LocalDate = LocalDate.now(),
        goal: Int = WEEKLY_GOAL
    ): StreakInfo {
        // Count trainings per week (identified by its Monday); future days do not count yet
        val countPerWeek = trainingDays
            .distinct()
            .filter { it <= today.toEpochDay() }
            .groupingBy { mondayOf(LocalDate.ofEpochDay(it)) }
            .eachCount()
        val goalWeeks = countPerWeek.filterValues { it >= goal }.keys
        val thisMonday = mondayOf(today)

        // Current streak: walk back week by week. A week still in progress that has
        // not reached the goal yet does not break the streak.
        var current = 0
        var week = if (thisMonday in goalWeeks) thisMonday else thisMonday.minusWeeks(1)
        while (week in goalWeeks) {
            current++
            week = week.minusWeeks(1)
        }

        // Best streak: longest run of consecutive weeks that reached the goal
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (w in goalWeeks.sorted()) {
            run = if (previous != null && w == previous.plusWeeks(1)) run + 1 else 1
            best = maxOf(best, run)
            previous = w
        }

        return StreakInfo(current, best, countPerWeek[thisMonday] ?: 0, goal)
    }
}