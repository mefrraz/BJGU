package com.bjgu.app.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bjgu.app.ui.ringing.AlarmRingingActivity

/**
 * BroadcastReceiver que recebe o disparo do AlarmManager.
 *
 * v3.1: Usa PendingIntent.getActivity() para lançamento mais fiável
 * e adiciona CATEGORY_ALARM para prioridade máxima no sistema.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", -1)
        val difficulty = intent.getIntExtra("alarm_difficulty", 0)
        val soundUri = intent.getStringExtra("alarm_sound_uri")
        val escalated = intent.getBooleanExtra("escalated", false)
        val snoozeCount = intent.getIntExtra("snooze_count", 0)
        val shakeToWake = intent.getBooleanExtra("shake_to_wake", false)
        val qrCodeMode = intent.getBooleanExtra("qr_code_mode", false)
        val qrCodeHash = intent.getStringExtra("qr_code_hash")

        if (alarmId == -1L) return

        val ringingIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("alarm_difficulty", difficulty)
            putExtra("alarm_sound_uri", soundUri)
            putExtra("escalated", escalated)
            putExtra("snooze_count", snoozeCount)
            putExtra("shake_to_wake", shakeToWake)
            putExtra("qr_code_mode", qrCodeMode)
            putExtra("qr_code_hash", qrCodeHash)

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

            // CATEGORY_ALARM dá prioridade máxima em alguns fabricantes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                addCategory("android.intent.category.ALARM")
            }
        }

        // Usar PendingIntent.getActivity para lançamento mais fiável
        // que context.startActivity() — o sistema trata com prioridade de alarm clock
        val pendingIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            ringingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            pendingIntent.send()
        } catch (e: PendingIntent.CanceledException) {
            // Fallback para startActivity se o PendingIntent falhar
            context.startActivity(ringingIntent)
        }
    }
}
