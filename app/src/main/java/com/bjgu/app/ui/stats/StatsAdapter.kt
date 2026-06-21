package com.bjgu.app.ui.stats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bjgu.app.data.alarm.AlarmEventEntity
import com.bjgu.app.databinding.ItemStatsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter para a lista de eventos no ecrã de estatísticas.
 */
class StatsAdapter : ListAdapter<AlarmEventEntity, StatsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStatsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemStatsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

        fun bind(event: AlarmEventEntity) {
            binding.textStatsDate.text = dateFormat.format(Date(event.timestamp))
            val seconds = event.responseTimeMs / 1000
            binding.textStatsTime.text = "${seconds}s"

            val diffLabel = when (event.difficulty) {
                0 -> binding.root.context.getString(com.bjgu.app.R.string.easy)
                1 -> binding.root.context.getString(com.bjgu.app.R.string.medium)
                2 -> binding.root.context.getString(com.bjgu.app.R.string.hard)
                else -> "?"
            }
            val escalatedLabel = if (event.wasEscalated) " ⚠" else ""
            binding.textStatsDifficulty.text = "$diffLabel$escalatedLabel"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AlarmEventEntity>() {
        override fun areItemsTheSame(old: AlarmEventEntity, new: AlarmEventEntity) =
            old.id == new.id

        override fun areContentsTheSame(old: AlarmEventEntity, new: AlarmEventEntity) =
            old == new
    }
}
