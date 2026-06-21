package com.bjgu.app.ui.alarms

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bjgu.app.R
import com.bjgu.app.alarm.AlarmScheduler
import com.bjgu.app.alarm.PermissionManager
import com.bjgu.app.data.alarm.AlarmEntity
import com.bjgu.app.databinding.ActivityMainBinding
import com.bjgu.app.ui.create.CreateAlarmActivity

/**
 * Ecrã principal da app BJGU.
 *
 * Mostra a lista de alarmes criados, gere permissões críticas na primeira execução,
 * e permite adicionar, ligar/desligar e apagar alarmes.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: AlarmViewModel
    private lateinit var adapter: AlarmAdapter

    /** Launcher para obter o resultado da criação de um alarme. */
    private val createAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // AlarmScheduler já foi chamado na CreateAlarmActivity
            // A lista atualiza automaticamente via LiveData
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ViewModel
        viewModel = ViewModelProvider(this)[AlarmViewModel::class.java]

        // Toolbar
        setSupportActionBar(binding.toolbar)

        // RecyclerView
        setupRecyclerView()

        // Observar lista de alarmes
        viewModel.alarms.observe(this) { alarms ->
            adapter.submitList(alarms)
            binding.textEmpty.visibility = if (alarms.isEmpty()) View.VISIBLE else View.GONE
        }

        // FAB para adicionar alarme
        binding.fabAdd.setOnClickListener {
            val intent = Intent(this, CreateAlarmActivity::class.java)
            createAlarmLauncher.launch(intent)
        }

        // Pedir permissões na primeira execução (em cadeia)
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        // Re-agendar alarmes ao voltar ao ecrã principal (segurança)
        viewModel.alarms.observe(this) { alarms ->
            AlarmScheduler.rescheduleAllEnabled(this, alarms.filter { it.enabled })
        }
    }

    // ─── Métodos privados ────────────────────────────────────────────

    /** Configura a RecyclerView com o Adapter e LayoutManager. */
    private fun setupRecyclerView() {
        adapter = AlarmAdapter(
            onToggle = { alarm, enabled -> viewModel.toggleAlarm(alarm, enabled) },
            onLongClick = { alarm -> showDeleteConfirmation(alarm) }
        )
        binding.recyclerAlarms.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    /** Mostra diálogo de confirmação antes de apagar um alarme. */
    private fun showDeleteConfirmation(alarm: AlarmEntity) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_alarm)
            .setMessage(R.string.delete_alarm_confirm)
            .setPositiveButton(R.string.yes) { _, _ -> viewModel.deleteAlarm(alarm) }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    /** Verifica e pede permissões críticas em cadeia. */
    private fun checkPermissions() {
        // 1. Permissão de alarme exato (API 31+)
        if (!PermissionManager.hasExactAlarmPermission(this)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.permission_exact_alarm_title)
                .setMessage(R.string.permission_exact_alarm_message)
                .setPositiveButton(R.string.permission_grant) { _, _ ->
                    PermissionManager.requestExactAlarmPermission(this)
                }
                .setNegativeButton(R.string.permission_skip, null)
                .show()
            return  // Continuar na próxima vez que abrir a app
        }

        // 2. Isenção de otimização de bateria
        if (!PermissionManager.isBatteryOptimizationIgnored(this)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.permission_battery_title)
                .setMessage(R.string.permission_battery_message)
                .setPositiveButton(R.string.permission_grant) { _, _ ->
                    PermissionManager.requestIgnoreBatteryOptimizations(this)
                }
                .setNegativeButton(R.string.permission_skip, null)
                .show()
        }
    }
}
