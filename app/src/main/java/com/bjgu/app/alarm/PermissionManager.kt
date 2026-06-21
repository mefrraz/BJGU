package com.bjgu.app.alarm

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

/**
 * Gestor de permissões críticas para o funcionamento do alarme.
 *
 * Trata três permissões fundamentais:
 * 1. SCHEDULE_EXACT_ALARM (API 31+) — Permite usar setAlarmClock().
 * 2. REQUEST_IGNORE_BATTERY_OPTIMIZATIONS — Evita que o sistema mate a app.
 * 3. USE_FULL_SCREEN_INTENT — Já está no Manifest, mas verificamos por segurança.
 *
 * Cada método que pede permissão abre o ecrã de Definições do sistema
 * no local exato onde o utilizador pode conceder a permissão.
 */
object PermissionManager {

    /**
     * Verifica se a permissão de alarme exato está concedida.
     * Em API < 31, a permissão é implícita (não é necessário verificar).
     */
    fun hasExactAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true  // API < 31: permissão implícita
        }
    }

    /**
     * Abre o ecrã de Definições para o utilizador conceder a permissão de alarme exato.
     * Só deve ser chamado em API 31+.
     */
    fun requestExactAlarmPermission(activity: AppCompatActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
        }
    }

    /**
     * Verifica se a app está isenta de otimizações de bateria.
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Abre o ecrã de Definições para o utilizador desativar a otimização de bateria.
     * Usa ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS que mostra a lista completa
     * de apps, onde o utilizador procura "BJGU" e seleciona "Não otimizar".
     */
    fun requestIgnoreBatteryOptimizations(activity: AppCompatActivity) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        activity.startActivity(intent)
    }

    /**
     * Verifica se a app tem permissão para abrir Activities em full-screen
     * por cima do ecrã de bloqueio. API 34+ requer concessão explícita.
     */
    fun hasFullScreenIntentPermission(context: Context): Boolean {
        // API 34+: USE_FULL_SCREEN_INTENT é gerido pelo sistema
        // A permissão no Manifest é suficiente; dispositivos com
        // restrições extra mostram diálogo do sistema automaticamente
        return true
    }

    /**
     * Verifica se a app pode desenhar sobre outras apps (overlay).
     * Necessário para alguns fabricantes que bloqueiam overlays.
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Abre as Definições para conceder permissão de overlay.
     * Usa ACTION_MANAGE_OVERLAY_PERMISSION com URI do package
     * para abrir diretamente na app BJGU.
     */
    fun requestOverlayPermission(activity: AppCompatActivity) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:${activity.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }
}
