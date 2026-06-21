package com.bjgu.app.data.alarm

import kotlinx.coroutines.flow.Flow

/**
 * Repositório para eventos de alarme (estatísticas).
 */
class AlarmEventRepository(private val eventDao: AlarmEventDao) {

    val allEvents: Flow<List<AlarmEventEntity>> = eventDao.getAllEvents()

    fun getRecentEvents(daysAgo: Long): Flow<List<AlarmEventEntity>> {
        val sevenDaysAgo = System.currentTimeMillis() - daysAgo
        return eventDao.getRecentEvents(sevenDaysAgo)
    }

    suspend fun insert(event: AlarmEventEntity) = eventDao.insert(event)

    suspend fun count(): Int = eventDao.count()
}
