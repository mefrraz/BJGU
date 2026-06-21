package com.bjgu.app

import android.app.Application

/**
 * Classe Application do BJGU.
 * Inicializa a base de dados Room e o singleton da aplicação.
 *
 * @author BJGU
 * @since 1.0.0
 */
class BJGUApplication : Application() {

    /** Base de dados Room — singleton lazy para evitar criação desnecessária */
    val database: com.bjgu.app.data.alarm.AlarmDatabase by lazy {
        com.bjgu.app.data.alarm.AlarmDatabase.getInstance(this)
    }

    /** Repositório de alarmes — singleton lazy */
    val alarmRepository: com.bjgu.app.data.alarm.AlarmRepository by lazy {
        com.bjgu.app.data.alarm.AlarmRepository(database.alarmDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        /** Singleton da aplicação para acesso rápido ao contexto e repositório */
        lateinit var instance: BJGUApplication
            private set
    }
}
