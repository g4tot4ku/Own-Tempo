package pt.ubi.clp.owntempo.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pt.ubi.clp.owntempo.R
import pt.ubi.clp.owntempo.data.TrainingDay
import pt.ubi.clp.owntempo.data.categoryType

private val DAY_SESSION_DIFF = object : DiffUtil.ItemCallback<TrainingDay>() {
    override fun areItemsTheSame(oldItem: TrainingDay, newItem: TrainingDay) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: TrainingDay, newItem: TrainingDay) = oldItem == newItem
}

class DayOverviewAdapter(
    private val onClick: (TrainingDay) -> Unit,
    private val onDelete: (TrainingDay) -> Unit
) : ListAdapter<TrainingDay, DayOverviewAdapter.SessionViewHolder>(DAY_SESSION_DIFF) {

    class SessionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryChip: TextView = view.findViewById(R.id.textCategoryChip)
        val title: TextView = view.findViewById(R.id.textSessionTitle)
        val subtitle: TextView = view.findViewById(R.id.textSessionSubtitle)
        val notes: TextView = view.findViewById(R.id.textSessionNotes)
        val deleteButton: Button = view.findViewById(R.id.buttonDeleteSession)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_day_overview, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context
        val category = item.categoryType

        holder.categoryChip.text = context.getString(category.labelRes)
        holder.categoryChip.backgroundTintList = ColorStateList.valueOf(context.getColor(category.colorRes))

        holder.title.text = item.displayTitle(context)

        val subtitle = item.displaySubtitle(context)
        holder.subtitle.text = subtitle
        holder.subtitle.visibility = if (subtitle.isEmpty()) View.GONE else View.VISIBLE

        holder.notes.text = item.notes.orEmpty()
        holder.notes.visibility = if (item.notes.isNullOrBlank()) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener { onClick(item) }
        holder.deleteButton.setOnClickListener { onDelete(item) }
    }
}