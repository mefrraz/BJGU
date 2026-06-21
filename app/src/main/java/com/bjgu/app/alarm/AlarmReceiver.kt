package com.bjgu.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import com.bjgu.app.ui.ringing.AlarmRingingActivity

/**
 * BroadcastReceiver que recebe o disparo do AlarmManager.
 *
 * Quando o AlarmManager dispara, este receiver:
 * 1. Extrai os dados do alarme do Intent (ID, dificuldade, som).
 * 2. Constrói um Intent para abrir a [AlarmRingingActivity] em full-screen,
 *    mesmo por cima do ecrã de bloqueio.
 * 3. Adiciona flags de ecrã: showWhenLocked, turnScreenOn, keepScreenOn.
 * 4. Inicia a Activity com FLAG_ACTIVITY_NEW_TASK (obrigatório para receivers).
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", -1)
        val difficulty = intent.getIntExtra("alarm_difficulty", 0)
        val soundUri = intent.getStringExtra("alarm_sound_uri")
        val escalated = intent.getBooleanExtra("escalated", false)
        val snoozeCount = intent.getIntExtra("snooze_count", 0)

        if (alarmId == -1L) return  // Dados inválidos, ignorar

        // Construir Intent para a Activity de alarme a tocar
        val ringingIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("alarm_difficulty", difficulty)
            putExtra("alarm_sound_uri", soundUri)
            putExtra("escalated", escalated)
            putExtra("snooze_count", snoozeCount)

            // Flags críticas para abrir por cima de tudo
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }

        context.startActivity(ringingIntent)
    }
}
