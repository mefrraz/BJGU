package com.bjgu.app.data.alarm

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room que representa um alarme na base de dados.
 *
 * Armazena todos os parâmetros de um alarme: hora, dias da semana,
 * dificuldade do desafio, som seleccionado e estado (ligado/desligado).
 *
 * @property id Identificador único auto-gerado pela base de dados.
 * @property hour Hora do alarme (0–23).
 * @property minute Minuto do alarme (0–59).
 * @property daysOfWeek Bitmask dos dias ativos.
 *                      Bit 0 = Domingo, Bit 1 = Segunda, …, Bit 6 = Sábado.
 *                      Exemplo: 0b0111110 = Seg–Sex (bits 1–5 ligados).
 * @property difficulty Nível de dificuldade do desafio: 0 = fácil, 1 = médio, 2 = difícil.
 * @property enabled Se o alarme está ativo ou pausado.
 * @property alarmSoundUri URI do som escolhido (null = som padrão do sistema).
 */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Int,
    val difficulty: Int,        // 0=fácil, 1=médio, 2=difícil (só usado se challengeType=0)
    val enabled: Boolean = true,
    val alarmSoundUri: String? = null,
    val challengeType: Int = 0, // 0=Matemática, 1=Shake, 2=QR Code
    val qrCodeHash: String? = null
)
