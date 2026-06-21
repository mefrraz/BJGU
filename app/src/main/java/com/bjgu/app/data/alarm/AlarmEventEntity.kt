package com.bjgu.app.data.alarm

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room para eventos de alarme (histórico).
 *
 * Guarda cada vez que um alarme disparou e foi desligado com sucesso,
 * registando o tempo que o utilizador demorou a resolver o desafio.
 *
 * @property id Identificador único auto-gerado.
 * @property alarmId ID do alarme original (0 = one-shot escalada/snooze).
 * @property timestamp Momento em que o alarme disparou (millis desde epoch).
 * @property responseTimeMs Tempo em milissegundos até resolver o desafio.
 * @property difficulty Nível de dificuldade do desafio (0-2).
 * @property wasEscalated Se este evento foi de um alarme escalado.
 */
@Entity(tableName = "alarm_events")
data class AlarmEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alarmId: Long,
    val timestamp: Long,
    val responseTimeMs: Long,
    val difficulty: Int,
    val wasEscalated: Boolean = false,
    val usedSnooze: Boolean = false
)
