package com.bjgu.app.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bjgu.app.data.alarm.AlarmEntity
import java.util.Calendar

/**
 * Agendador de alarmes — gere o sistema Android AlarmManager.
 *
 * Responsável por agendar e cancelar alarmes de forma fiável,
 * utilizando [AlarmManager.setAlarmClock] que garante:
 *  - Máxima prioridade de disparo (mesmo em modo Doze).
 *  - Ícone de alarme na barra de estado (confiança para o utilizador).
 *  - Ignora restrições de economia de bateria.
 *
 * Cada alarme recebe um [PendingIntent] único com requestCode = alarm.id,
 * permitindo cancelar ou re-agendar individualmente.
 */
object AlarmScheduler {

    private const val ALARM_REQUEST_CODE_BASE = 1000

    /**
     * Agenda um alarme no sistema.
     *
     * Se o alarme tiver dias da semana configurados, calcula o próximo dia/hora
     * que corresponde ao bitmask [AlarmEntity.daysOfWeek].
     * Se for um alarme único (nenhum dia selecionado), dispara na próxima ocorrência.
     *
     * @param context Contexto da aplicação.
     * @param alarm O alarme a agendar.
     */
    fun scheduleAlarm(context: Context, alarm: AlarmEntity) {
        if (!alarm.enabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = buildAlarmIntent(context, alarm)
        val pendingIntent = buildPendingIntent(context, alarm, intent)

        val triggerTime = calculateNextTriggerTime(alarm)

        // setAlarmClock() — método mais fiável para alarmes críticos
        alarmManager.setAlarmClock(
            AlarmClockInfo(triggerTime, pendingIntent),
            pendingIntent
        )
    }

    /**
     * Cancela um alarme agendado.
     *
     * @param context Contexto da aplicação.
     * @param alarmId ID do alarme a cancelar.
     */
    fun cancelAlarm(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE_BASE + alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Re-agenda todos os alarmes ativos.
     * Chamado após reboot do dispositivo ou após mudança de permissões.
     *
     * @param context Contexto da aplicação.
     * @param alarms Lista de alarmes a re-agendar.
     */
    fun rescheduleAllEnabled(context: Context, alarms: List<AlarmEntity>) {
        for (alarm in alarms) {
            if (alarm.enabled) {
                scheduleAlarm(context, alarm)
            }
        }
    }

    /**
     * Agenda um alarme único (one-shot) para daqui a [delayMs] milissegundos.
     * Usado para escalada (2 min) e snooze (5 min).
     *
     * @param context Contexto da aplicação.
     * @param alarmId ID do alarme original (para identificar ao disparar).
     * @param difficulty Nível de dificuldade (0-2).
     * @param soundUri URI do som (null = default).
     * @param delayMs Atraso em milissegundos até disparar.
     * @param escalated Se este é um alarme de verificação (escalada).
     * @param snoozeCount Número de snoozes já usados (0 = primeiro disparo).
     */
    fun scheduleOneShotAlarm(
        context: Context,
        alarmId: Long,
        difficulty: Int,
        soundUri: String?,
        delayMs: Long,
        escalated: Boolean = false,
        snoozeCount: Int = 0,
        shakeToWake: Boolean = false,
        qrCodeMode: Boolean = false,
        qrCodeHash: String? = null
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("alarm_difficulty", difficulty)
            putExtra("alarm_sound_uri", soundUri)
            putExtra("escalated", escalated)
            putExtra("snooze_count", snoozeCount)
            putExtra("shake_to_wake", shakeToWake)
            putExtra("qr_code_mode", qrCodeMode)
            putExtra("qr_code_hash", qrCodeHash)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        // Usar requestCode negativo/offset para one-shot (não colide com alarmes normais)
        val requestCode = ALARM_REQUEST_CODE_BASE + alarmId.toInt() + 10000
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        val triggerTime = System.currentTimeMillis() + delayMs

        alarmManager.setAlarmClock(
            AlarmClockInfo(triggerTime, pendingIntent),
            pendingIntent
        )
    }

    // ─── Métodos privados ────────────────────────────────────────────

    /** Constrói o Intent que o AlarmManager envia para o AlarmReceiver. */
    private fun buildAlarmIntent(context: Context, alarm: AlarmEntity): Intent {
        return Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_difficulty", alarm.difficulty)
            putExtra("alarm_sound_uri", alarm.alarmSoundUri)
            putExtra("shake_to_wake", alarm.shakeToWake)
            putExtra("qr_code_mode", alarm.qrCodeMode)
            putExtra("qr_code_hash", alarm.qrCodeHash)
        }
    }

    /** Constrói o PendingIntent único para este alarme. */
    private fun buildPendingIntent(
        context: Context,
        alarm: AlarmEntity,
        intent: Intent
    ): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE_BASE + alarm.id.toInt(),
            intent,
            flags
        )
    }

    /**
     * Calcula o próximo instante de disparo (em millis desde epoch).
     *
     * Regras:
     *  - Se daysOfWeek == 0 (nenhum dia), dispara hoje se a hora ainda não passou,
     *    senão amanhã.
     *  - Se daysOfWeek != 0, procura o próximo dia da semana com bit ligado.
     */
    private fun calculateNextTriggerTime(alarm: AlarmEntity): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.daysOfWeek == 0) {
            // Alarme único — se já passou hoje, agenda para amanhã
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_MONTH, 1)
            }
            return target.timeInMillis
        }

        // Alarme com dias da semana — procura o próximo dia ativo
        // daysOfWeek: bit 0 = Domingo, bit 1 = Segunda, …, bit 6 = Sábado
        // Calendar.DAY_OF_WEEK: 1 = Domingo, 2 = Segunda, …, 7 = Sábado
        for (dayOffset in 0..6) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = target.timeInMillis
                add(Calendar.DAY_OF_MONTH, dayOffset)
            }
            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)  // 1=Dom … 7=Sáb
            val bitIndex = if (dayOfWeek == Calendar.SUNDAY) 0 else dayOfWeek - 1

            if ((alarm.daysOfWeek and (1 shl bitIndex)) != 0) {
                // Bit está ligado — verificar se é um horário futuro
                if (dayOffset > 0 || candidate.timeInMillis > now.timeInMillis) {
                    return candidate.timeInMillis
                }
            }
        }

        // Fallback: se por algum motivo não encontrou, agenda para amanhã
        target.add(Calendar.DAY_OF_MONTH, 1)
        return target.timeInMillis
    }
}
