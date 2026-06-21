package com.bjgu.app.ui.stats

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bjgu.app.R
import com.bjgu.app.data.alarm.AlarmEventEntity
import com.bjgu.app.databinding.ActivityStatsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Ecrã de estatísticas — mostra histórico de respostas e médias.
 */
class StatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatsBinding
    private lateinit var viewModel: StatsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.stats_title)

        viewModel = ViewModelProvider(this)[StatsViewModel::class.java]

        val adapter = StatsAdapter()
        binding.recyclerStats.layoutManager = LinearLayoutManager(this)
        binding.recyclerStats.adapter = adapter

        // Observar eventos
        lifecycleScope.launch {
            viewModel.recentEvents.collectLatest { events ->
                adapter.submitList(events)
                binding.textEmptyStats.visibility =
                    if (events.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerStats.visibility =
                    if (events.isEmpty()) View.GONE else View.VISIBLE

                updateSummary(events)
            }
        }
    }

    private fun updateSummary(events: List<AlarmEventEntity>) {
        if (events.isEmpty()) {
            binding.textAvgTime.text = "--"
            binding.textBestTime.text = "--"
            return
        }

        val avgMs = events.map { it.responseTimeMs }.average().toLong()
        val bestMs = events.map { it.responseTimeMs }.min()

        binding.textAvgTime.text = formatSeconds(avgMs)
        binding.textBestTime.text = formatSeconds(bestMs)
    }

    private fun formatSeconds(ms: Long): String {
        val seconds = ms / 1000
        return "${seconds}s"
    }
}
