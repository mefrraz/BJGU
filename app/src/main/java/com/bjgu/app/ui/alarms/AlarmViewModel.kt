package com.bjgu.app.ui.alarms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.bjgu.app.BJGUApplication
import com.bjgu.app.alarm.AlarmScheduler
import com.bjgu.app.data.alarm.AlarmEntity
import com.bjgu.app.data.alarm.AlarmRepository
import kotlinx.coroutines.launch

/**
 * ViewModel do ecrã principal.
 *
 * Expõe a lista de alarmes como LiveData para observação reativa pela UI.
 * Gere ações: toggle (ligar/desligar), apagar, e re-agendamento no sistema.
 */
class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    /** Repositório obtido da Application (evita leaks de Context). */
    private val repository: AlarmRepository =
        (application as BJGUApplication).alarmRepository

    /** Lista reativa de alarmes observada pela RecyclerView. */
    val alarms: LiveData<List<AlarmEntity>> = repository.allAlarms.asLiveData()

    /**
     * Liga ou desliga um alarme e re-agenda/cancela no sistema.
     *
     * @param alarm O alarme a alterar.
     * @param enabled Novo estado (true = ligado, false = desligado).
     */
    fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(alarm.id, enabled)
            val updated = alarm.copy(enabled = enabled)

            if (enabled) {
                AlarmScheduler.scheduleAlarm(getApplication(), updated)
            } else {
                AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
            }
        }
    }

    /**
     * Apaga um alarme da base de dados e cancela-o no sistema.
     *
     * @param alarm O alarme a apagar.
     */
    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.delete(alarm)
            AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
        }
    }
}
