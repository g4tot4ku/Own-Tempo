package pt.ubi.clp.owntempo.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pt.ubi.clp.owntempo.R
import pt.ubi.clp.owntempo.data.TrainingDay
import pt.ubi.clp.owntempo.data.categoryType
import java.time.LocalDate

// One cell of the month grid: a date, whether it belongs to the shown month, and its list of trainings
data class MonthCell(val date: LocalDate, val inMonth: Boolean, val trainings: List<TrainingDay>)

private val CELL_DIFF = object : DiffUtil.ItemCallback<MonthCell>() {
    override fun areItemsTheSame(oldItem: MonthCell, newItem: MonthCell) = oldItem.date == newItem.date
    override fun areContentsTheSame(oldItem: MonthCell, newItem: MonthCell) = oldItem == newItem
}

class MonthAdapter(private val onClick: (MonthCell) -> Unit) :
    ListAdapter<MonthCell, MonthAdapter.CellViewHolder>(CELL_DIFF) {

    class CellViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val number: TextView = view.findViewById(R.id.textDayNumber)
        val category: TextView = view.findViewById(R.id.textDayCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_month_day, parent, false)
        return CellViewHolder(view)
    }

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        val cell = getItem(position)
        val context = holder.itemView.context

        holder.number.text = cell.date.dayOfMonth.toString()
        if (cell.date == LocalDate.now()) {
            // Today: number inside a filled circle
            holder.number.setBackgroundResource(R.drawable.bg_today)
            holder.number.setTextColor(context.getColor(R.color.md_on_primary))
        } else {
            holder.number.background = null
            holder.number.setTextColor(context.getColor(R.color.md_on_surface))
        }
        // Days of the previous/next month are dimmed
        holder.itemView.alpha = if (cell.inMonth) 1f else 0.4f

        val trainings = cell.trainings
        if (trainings.isEmpty()) {
            holder.category.visibility = View.GONE
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
        }

        holder.itemView.setOnClickListener { onClick(cell) }
    }
}