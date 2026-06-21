package com.bjgu.app.data.alarm

import kotlinx.coroutines.flow.Flow

/**
 * Repositório de alarmes — camada intermédia entre o ViewModel e o DAO.
 *
 * Encapsula o acesso à base de dados Room e expõe operações suspensas
 * (para escritas/consultas pontuais) e Flow (para observação reativa da lista).
 *
 * @property alarmDao O DAO Room injetado pela Application.
 */
class AlarmRepository(private val alarmDao: AlarmDao) {

    /** Flow reativo com a lista completa de alarmes ordenados. */
    val allAlarms: Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    /** Obtém um alarme específico pelo ID. */
    suspend fun getAlarmById(id: Long): AlarmEntity? = alarmDao.getAlarmById(id)

    /** Insere um alarme novo e devolve o ID gerado. */
    suspend fun insert(alarm: AlarmEntity): Long = alarmDao.insert(alarm)

    /** Atualiza um alarme existente. */
    suspend fun update(alarm: AlarmEntity) = alarmDao.update(alarm)

    /** Apaga um alarme. */
    suspend fun delete(alarm: AlarmEntity) = alarmDao.delete(alarm)

    /** Liga ou desliga um alarme. */
    suspend fun setEnabled(id: Long, enabled: Boolean) = alarmDao.setEnabled(id, enabled)
}
