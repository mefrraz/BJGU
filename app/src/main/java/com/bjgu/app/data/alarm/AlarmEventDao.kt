package com.bjgu.app.data.alarm

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para a entidade AlarmEventEntity.
 *
 * Gere o histórico de eventos de alarme para o ecrã de estatísticas.
 */
@Dao
interface AlarmEventDao {

    /** Devolve todos os eventos ordenados do mais recente para o mais antigo. */
    @Query("SELECT * FROM alarm_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<AlarmEventEntity>>

    /** Devolve os eventos dos últimos 7 dias. */
    @Query("SELECT * FROM alarm_events WHERE timestamp > :sevenDaysAgo ORDER BY timestamp DESC")
    fun getRecentEvents(sevenDaysAgo: Long): Flow<List<AlarmEventEntity>>

    /** Insere um novo evento. */
    @Insert
    suspend fun insert(event: AlarmEventEntity)

    /** Conta total de eventos. */
    @Query("SELECT COUNT(*) FROM alarm_events")
    suspend fun count(): Int
}
