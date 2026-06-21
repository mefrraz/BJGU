package com.bjgu.app.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bjgu.app.R
import com.bjgu.app.data.alarm.AlarmEventEntity
import com.bjgu.app.databinding.ActivityStatsBinding
import com.bjgu.app.ui.EdgeToEdgeUtil
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Ecrã de estatísticas — calendário, streak, saudação com nome.
 */
class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private lateinit var viewModel: StatsViewModel
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        EdgeToEdgeUtil.setup(this, binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val name = prefs.getString("user_name", "")?.trim() ?: ""
        val title = if (name.isNotEmpty()) {
            getString(R.string.stats_title_name, name)
        } else {
            getString(R.string.stats_title)
        }
        supportActionBar?.title = title

        viewModel = ViewModelProvider(this)[StatsViewModel::class.java]

        val adapter = StatsAdapter()
        binding.recyclerStats.layoutManager = LinearLayoutManager(this)
        binding.recyclerStats.adapter = adapter

        lifecycleScope.launch {
            viewModel.recentEvents.collectLatest { events ->
                adapter.submitList(events)
                binding.textEmptyStats.visibility =
                    if (events.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerStats.visibility =
                    if (events.isEmpty()) View.GONE else View.VISIBLE
                updateSummary(events)
                buildCalendar(events)
            }
        }
    }

    private fun updateSummary(events: List<AlarmEventEntity>) {
        if (events.isEmpty()) {
            binding.textAvgTime.text = "--"
            binding.textBestTime.text = "--"
            return
        }

        val successes = events.filter { !it.usedSnooze }
        val avgMs = if (successes.isNotEmpty()) successes.map { it.responseTimeMs }.average().toLong() else 0
        val bestMs = if (successes.isNotEmpty()) successes.map { it.responseTimeMs }.min() else 0

        binding.textAvgTime.text = formatSeconds(avgMs)
        binding.textBestTime.text = formatSeconds(bestMs)

        // Streak — dias consecutivos (a partir de hoje) com pelo menos 1 sucesso
        val successDays = events
            .filter { !it.usedSnooze }
            .map { dateFormat.format(Date(it.timestamp)) }
            .toSet()
        val snoozeDays = events
            .filter { it.usedSnooze }
            .map { dateFormat.format(Date(it.timestamp)) }
            .toSet()
        val skippedDays = events
            .map { dateFormat.format(Date(it.timestamp)) }
            .toSet()

        var streak = 0
        val cal = Calendar.getInstance()
        while (true) {
            val dayStr = dateFormat.format(cal.time)
            if (dayStr !in successDays) break
            streak++
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        binding.textStreak.text = "$streak"
    }

    /** Constrói um calendário simples dos últimos 30 dias. */
    private fun buildCalendar(events: List<AlarmEventEntity>) {
        val successDays = events.filter { !it.usedSnooze }
            .map { dateFormat.format(Date(it.timestamp)) }.toSet()
        val snoozeDays = events.filter { it.usedSnooze }
            .map { dateFormat.format(Date(it.timestamp)) }.toSet()

        binding.calendarGrid.removeAllViews()

        val dayHeaders = listOf("S", "M", "T", "W", "T", "F", "S")
        val headerRow = TableRow(this)
        for (h in dayHeaders) {
            val tv = TextView(this).apply {
                text = h
                gravity = Gravity.CENTER
                setTextColor(getColor(R.color.text_secondary))
                textSize = 11f
                setPadding(0, 4, 0, 4)
            }
            headerRow.addView(tv)
        }
        binding.calendarGrid.addView(headerRow)

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -29)
        val today = Calendar.getInstance()
        val todayStr = dateFormat.format(today.time)

        // Ajustar para começar no domingo
        var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)  // 1=Dom..7=Sáb
        // Preencher dias vazios antes do primeiro dia
        val rows = mutableListOf<TableRow>()
        var currentRow = TableRow(this)
        for (i in 1 until dayOfWeek) {
            currentRow.addView(TextView(this))
        }

        for (i in 0 until 30) {
            val dayStr = dateFormat.format(cal.time)
            val day = cal.get(Calendar.DAY_OF_MONTH)

            val dot = TextView(this).apply {
                text = "$day"
                gravity = Gravity.CENTER
                setPadding(0, 6, 0, 6)
                textSize = 12f
                setTextColor(getColor(R.color.text_primary))

                val bg = when {
                    dayStr in successDays -> R.drawable.dot_green
                    dayStr in snoozeDays -> R.drawable.dot_yellow
                    dayStr == todayStr -> R.drawable.dot_today
                    else -> 0
                }
                if (bg != 0) setBackgroundResource(bg)
            }
            currentRow.addView(dot)

            cal.add(Calendar.DAY_OF_MONTH, 1)
            val newDow = cal.get(Calendar.DAY_OF_WEEK)
            if (newDow == Calendar.SUNDAY || i == 29) {
                rows.add(currentRow)
                if (i < 29) currentRow = TableRow(this)
            }
        }

        for (row in rows) binding.calendarGrid.addView(row)
    }

    private fun formatSeconds(ms: Long): String {
        if (ms == 0L) return "--"
        return "${ms / 1000}s"
    }
}
