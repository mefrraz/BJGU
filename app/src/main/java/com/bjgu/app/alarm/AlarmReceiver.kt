package com.bjgu.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bjgu.app.ui.ringing.AlarmRingingActivity

/**
 * BroadcastReceiver que recebe o disparo do AlarmManager.
 * Usa startActivity direto — mais compatível com Xiaomi/Huawei.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", -1)
        val difficulty = intent.getIntExtra("alarm_difficulty", 0)
        val soundUri = intent.getStringExtra("alarm_sound_uri")
        val escalated = intent.getBooleanExtra("escalated", false)
        val snoozeCount = intent.getIntExtra("snooze_count", 0)
        val challengeType = intent.getIntExtra("challenge_type", 0)
        val qrCodeHash = intent.getStringExtra("qr_code_hash")

        if (alarmId == -1L) return

        val ringingIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("alarm_difficulty", difficulty)
            putExtra("alarm_sound_uri", soundUri)
            putExtra("escalated", escalated)
            putExtra("snooze_count", snoozeCount)
            putExtra("challenge_type", challengeType)
            putExtra("qr_code_hash", qrCodeHash)

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                addCategory("android.intent.category.ALARM")
            }
        }

        context.startActivity(ringingIntent)
    }
}
