package pt.ubi.clp.owntempo.ui

import android.content.Context
import pt.ubi.clp.owntempo.R
import pt.ubi.clp.owntempo.data.TrainingDay
import pt.ubi.clp.owntempo.data.categoryType
import java.util.Locale

// "Gym · Legs" when a routine is set, otherwise just the category name
fun TrainingDay.displayTitle(context: Context): String {
    val label = context.getString(categoryType.labelRes)
    return if (routine.isNullOrBlank()) label else context.getString(R.string.title_with_routine, label, routine)
}

// Format duration in minutes into user-friendly "1h 30min", "2h", or "45min"
fun formatDuration(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}min"
        h > 0 -> "${h}h"
        else -> "${m}min"
    }
}

// Format exercise duration in total seconds into "1h 02m 30s", "02m 05s", or "45s"
fun formatExerciseDuration(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        h > 0 -> String.format(Locale.getDefault(), "%dh %02dm %02ds", h, m, s)
        m > 0 -> String.format(Locale.getDefault(), "%02dm %02ds", m, s)
        else -> String.format(Locale.getDefault(), "%ds", s)
    }
}

// Second line of a day row: the routine, or the activity numbers that were filled in
fun TrainingDay.displaySubtitle(context: Context): String {
    if (!routine.isNullOrBlank()) return routine
    val parts = mutableListOf<String>()
    distanceKm?.let { parts += context.getString(R.string.value_distance_short, formatNumber(it)) }
    durationMinutes?.let { parts += context.getString(R.string.value_duration_short, formatDuration(it)) }
    calories?.let { parts += context.getString(R.string.value_calories_short, it) }
    return parts.joinToString(context.getString(R.string.metrics_separator))
}

// 60.0 -> "60", 62.5 -> "62.5"
fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()