package com.bjgu.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bjgu.app.BJGUApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver executado após o reboot do dispositivo.
 *
 * Re-agenda todos os alarmes enabled para garantir que nenhum alarme
 * se perde após um reinício do telemóvel.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Re-agendar todos os alarmes ativos
        val app = BJGUApplication.instance
        val repository = app.alarmRepository

        CoroutineScope(Dispatchers.IO).launch {
            val alarms = repository.allAlarms.first()  // Obtém a lista atual
            val enabledAlarms = alarms.filter { it.enabled }
            AlarmScheduler.rescheduleAllEnabled(context, enabledAlarms)
        }
    }
}
