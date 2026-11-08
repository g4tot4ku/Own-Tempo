package pt.ubi.clp.owntempo.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import pt.ubi.clp.owntempo.R
import pt.ubi.clp.owntempo.data.TrainingDay
import pt.ubi.clp.owntempo.data.categoryType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// One row of the weekly list: a date and the list of trainings logged for it
data class DayItem(val date: LocalDate, val trainings: List<TrainingDay>)

private val DAY_DIFF = object : DiffUtil.ItemCallback<DayItem>() {
    override fun areItemsTheSame(oldItem: DayItem, newItem: DayItem) = oldItem.date == newItem.date
    override fun areContentsTheSame(oldItem: DayItem, newItem: DayItem) = oldItem == newItem
}

class DayAdapter(private val onClick: (DayItem) -> Unit) :
    ListAdapter<DayItem, DayAdapter.DayViewHolder>(DAY_DIFF) {

    class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view as MaterialCardView
        val dayName: TextView = view.findViewById(R.id.textDayName)
        val dayDate: TextView = view.findViewById(R.id.textDayDate)
        val category: TextView = view.findViewById(R.id.textCategory)
        val subtitle: TextView = view.findViewById(R.id.textSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context
        val locale = Locale.getDefault()

        holder.dayName.text = item.date.dayOfWeek
            .getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { it.uppercase() }
        holder.dayDate.text = item.date.format(DateTimeFormatter.ofPattern("d MMM", locale))

        val trainings = item.trainings
        if (trainings.isEmpty()) {
            holder.category.visibility = View.GONE
            holder.subtitle.visibility = View.VISIBLE
            holder.subtitle.text = context.getString(R.string.no_training)
        } else {
            val first = trainings.first()
            val category = first.categoryType
            holder.category.visibility = View.VISIBLE
            val label = context.getString(category.labelRes)
            holder.category.text = if (trainings.size > 1) {
                "$label (+${trainings.size - 1})"
            } else {
                label
            }
            holder.category.backgroundTintList = ColorStateList.valueOf(context.getColor(category.colorRes))

            val subtitles = trainings.map { it.displayTitle(context) }
            val subtitleText = subtitles.joinToString(" · ")
            holder.subtitle.text = subtitleText
            holder.subtitle.visibility = if (subtitleText.isEmpty()) View.GONE else View.VISIBLE
        }

        // Today gets a thicker border in the primary color
        val density = context.resources.displayMetrics.density
        if (item.date == LocalDate.now()) {
            holder.card.strokeColor = context.getColor(R.color.md_primary)
            holder.card.strokeWidth = (2 * density).toInt()
        } else {
            holder.card.strokeColor = context.getColor(R.color.card_stroke)
            holder.card.strokeWidth = (1 * density).toInt()
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }
}