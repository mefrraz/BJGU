package com.bjgu.app.data.alarm

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para a entidade AlarmEntity.
 *
 * Define todas as operações de leitura/escrita sobre a tabela "alarms".
 * As queries de leitura devolvem Flow para integração com LiveData/ViewModel.
 */
@Dao
interface AlarmDao {

    /** Devolve todos os alarmes ordenados por hora e minuto, como Flow reativo. */
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    /** Devolve um alarme pelo ID (usado para obter dados após disparo). */
    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    /** Insere um novo alarme e devolve o ID gerado. */
    @Insert
    suspend fun insert(alarm: AlarmEntity): Long

    /** Atualiza um alarme existente. */
    @Update
    suspend fun update(alarm: AlarmEntity)

    /** Apaga um alarme pelo ID. */
    @Delete
    suspend fun delete(alarm: AlarmEntity)

    /** Alterna o estado enabled de um alarme (liga/desliga). */
    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
