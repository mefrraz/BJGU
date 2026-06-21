package com.bjgu.app.ui.alarms

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bjgu.app.R
import com.bjgu.app.data.alarm.AlarmEntity
import com.bjgu.app.databinding.ItemAlarmBinding

/**
 * Adapter da RecyclerView para a lista de alarmes.
 *
 * Usa ListAdapter com DiffUtil para animações automáticas ao adicionar/remover itens.
 * Cada linha mostra: hora, dias da semana abreviados, switch on/off e badge de dificuldade.
 *
 * @property onToggle Chamado quando o utilizador alterna o switch (liga/desliga).
 * @property onLongClick Chamado quando o utilizador faz long-click (apagar alarme).
 */
class AlarmAdapter(
    private val onToggle: (AlarmEntity, Boolean) -> Unit,
    private val onLongClick: (AlarmEntity) -> Unit,
    private val onClick: (AlarmEntity) -> Unit
) : ListAdapter<AlarmEntity, AlarmAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlarmBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ─── ViewHolder ────────────────────────────────────────────────

    inner class ViewHolder(private val binding: ItemAlarmBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(alarm: AlarmEntity) {
            // Hora formatada HH:MM
            val timeText = String.format("%02d:%02d", alarm.hour, alarm.minute)
            binding.textTime.text = timeText

            // Dias da semana abreviados
            binding.textDays.text = formatDaysOfWeek(alarm.daysOfWeek, binding.root.context)

            // Switch on/off
            binding.switchEnabled.setOnCheckedChangeListener(null)  // Evitar loop
            binding.switchEnabled.isChecked = alarm.enabled
            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggle(alarm, isChecked)
            }

            // Badge — reflete o tipo de desafio
            val badgeText = when (alarm.challengeType) {
                0 -> when (alarm.difficulty) {
                    0 -> binding.root.context.getString(R.string.easy)
                    1 -> binding.root.context.getString(R.string.medium)
                    else -> binding.root.context.getString(R.string.hard)
                }
                1 -> binding.root.context.getString(R.string.challenge_shake)
                2 -> binding.root.context.getString(R.string.challenge_qr)
                else -> "?"
            }
            binding.textDifficulty.text = badgeText

            // Click normal → editar
            binding.root.setOnClickListener {
                onClick(alarm)
            }

            // Long click para apagar
            binding.root.setOnLongClickListener {
                onLongClick(alarm)
                true
            }
        }
    }

    // ─── DiffUtil ──────────────────────────────────────────────────

    class DiffCallback : DiffUtil.ItemCallback<AlarmEntity>() {
        override fun areItemsTheSame(oldItem: AlarmEntity, newItem: AlarmEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AlarmEntity, newItem: AlarmEntity): Boolean =
            oldItem == newItem
    }
}

/**
 * Converte o bitmask de dias da semana numa string abreviada.
 * Exemplo: 0b0111110 (Seg–Sex) → "Seg Ter Qua Qui Sex"
 */
internal fun formatDaysOfWeek(daysOfWeek: Int, context: android.content.Context): String {
    if (daysOfWeek == 0) return context.getString(R.string.system_default)
    if (daysOfWeek == 0b1111111) return context.getString(R.string.every_day)  // 7 dias

    val shortNames = listOf(
        context.getString(R.string.sun_short), // dom (bit 0)
        context.getString(R.string.mon_short), // seg (bit 1)
        context.getString(R.string.tue_short), // ter (bit 2)
        context.getString(R.string.wed_short), // qua (bit 3)
        context.getString(R.string.thu_short), // qui (bit 4)
        context.getString(R.string.fri_short), // sex (bit 5)
        context.getString(R.string.sat_short)  // sáb (bit 6)
    )

    return shortNames
        .filterIndexed { index, _ -> (daysOfWeek and (1 shl index)) != 0 }
        .joinToString(" ")
}
