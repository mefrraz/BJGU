package com.bjgu.app.ui.alarms

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import com.bjgu.app.ui.EdgeToEdgeUtil
import com.bjgu.app.ui.create.CreateAlarmActivity
import com.bjgu.app.ui.stats.StatsActivity

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

        // Edge-to-edge
        EdgeToEdgeUtil.setup(this, binding.root)

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
    }

    override fun onResume() {
        super.onResume()
        // Re-agendar alarmes ao voltar ao ecrã principal (segurança)
        viewModel.alarms.observe(this) { alarms ->
            AlarmScheduler.rescheduleAllEnabled(this, alarms.filter { it.enabled })
        }
        // Verificar permissões — se o utilizador acabou de conceder uma,
        // o onResume dispara e pede a próxima automaticamente
        checkPermissions()
    }

    // ─── Menu da toolbar ──────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_stats -> {
                startActivity(Intent(this, StatsActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, com.bjgu.app.ui.settings.SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ─── Métodos privados ────────────────────────────────────────────

    /** Configura a RecyclerView com o Adapter e LayoutManager. */
    private fun setupRecyclerView() {
        adapter = AlarmAdapter(
            onToggle = { alarm, enabled -> viewModel.toggleAlarm(alarm, enabled) },
            onLongClick = { alarm -> showDeleteConfirmation(alarm) },
            onClick = { alarm -> editAlarm(alarm) }
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

    /** Abre o ecrã de criação em modo edição. */
    private fun editAlarm(alarm: AlarmEntity) {
        val intent = Intent(this, CreateAlarmActivity::class.java).apply {
            putExtra("alarm_id", alarm.id)
        }
        createAlarmLauncher.launch(intent)
    }

    /** Verifica e pede permissões críticas em cadeia. */
    private var permissionsDialogShowing = false

    private fun checkPermissions() {
        if (permissionsDialogShowing) return

        // 1. Permissão de alarme exato (API 31+)
        if (!PermissionManager.hasExactAlarmPermission(this)) {
            permissionsDialogShowing = true
            AlertDialog.Builder(this)
                .setTitle(R.string.permission_exact_alarm_title)
                .setMessage(R.string.permission_exact_alarm_message)
                .setPositiveButton(R.string.permission_grant) { _, _ ->
                    permissionsDialogShowing = false
                    PermissionManager.requestExactAlarmPermission(this)
                }
                .setNegativeButton(R.string.permission_skip) { _, _ ->
                    permissionsDialogShowing = false
                }
                .setOnDismissListener { permissionsDialogShowing = false }
                .show()
            return
        }

        // 2. Isenção de otimização de bateria
        if (!PermissionManager.isBatteryOptimizationIgnored(this)) {
            permissionsDialogShowing = true
            AlertDialog.Builder(this)
                .setTitle(R.string.permission_battery_title)
                .setMessage(R.string.permission_battery_message)
                .setPositiveButton(R.string.permission_grant) { _, _ ->
                    permissionsDialogShowing = false
                    PermissionManager.requestIgnoreBatteryOptimizations(this)
                }
                .setNegativeButton(R.string.permission_skip) { _, _ ->
                    permissionsDialogShowing = false
                }
                .setOnDismissListener { permissionsDialogShowing = false }
                .show()
            return
        }

        // 3. Overlay (API 34+ ou fabricantes que bloqueiam)
        if (!PermissionManager.hasOverlayPermission(this)) {
            permissionsDialogShowing = true
            AlertDialog.Builder(this)
                .setTitle(R.string.permission_overlay_title)
                .setMessage(R.string.permission_overlay_message)
                .setPositiveButton(R.string.permission_grant) { _, _ ->
                    permissionsDialogShowing = false
                    PermissionManager.requestOverlayPermission(this)
                }
                .setNegativeButton(R.string.permission_skip) { _, _ ->
                    permissionsDialogShowing = false
                }
                .setOnDismissListener { permissionsDialogShowing = false }
                .show()
        }
    }
}
