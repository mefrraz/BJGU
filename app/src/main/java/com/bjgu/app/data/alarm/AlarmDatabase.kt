package com.bjgu.app.data.alarm

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de dados Room da app BJGU.
 *
 * Contém uma única tabela "alarms" definida pela entidade [AlarmEntity].
 * Usa o padrão singleton para garantir uma única instância em toda a app.
 *
 * @author BJGU
 * @since 1.0.0
 */
@Database(
    entities = [AlarmEntity::class, AlarmEventEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AlarmDatabase : RoomDatabase() {

    /** Devolve o DAO para operações sobre a tabela de alarmes. */
    abstract fun alarmDao(): AlarmDao

    /** Devolve o DAO para operações sobre a tabela de eventos (estatísticas). */
    abstract fun alarmEventDao(): AlarmEventDao

    companion object {
        @Volatile
        private var INSTANCE: AlarmDatabase? = null

        /**
         * Obtém (ou cria) a instância singleton da base de dados.
         *
         * @param context Contexto da aplicação (usa applicationContext para evitar leaks).
         * @return Instância única de AlarmDatabase.
         */
        fun getInstance(context: Context): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "bjgu_database"
                )
                    .fallbackToDestructiveMigration()  // MVP: recria DB em vez de migrar
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
