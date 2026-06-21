package com.bjgu.app.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.bjgu.app.BJGUApplication
import com.bjgu.app.data.alarm.AlarmEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * ViewModel para o ecrã de estatísticas.
 * Expõe os eventos dos últimos 7 dias.
 */
class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as BJGUApplication).alarmEventRepository

    /** Eventos dos últimos 7 dias (7 * 24 * 60 * 60 * 1000 ms). */
    val recentEvents: Flow<List<AlarmEventEntity>> =
        repository.getRecentEvents(7L * 24 * 60 * 60 * 1000)
}
