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
import androidx.preference.PreferenceManager
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
        updateGreeting()

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
        try {
            viewModel.alarms.observe(this) { alarms ->
                AlarmScheduler.rescheduleAllEnabled(this, alarms.filter { it.enabled })
            }
            checkPermissions()
        } catch (_: Exception) {
            // Evitar crash no arranque
        }
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

    /** Atualiza o título com saudação personalizada. */
    private fun updateGreeting() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val name = prefs.getString("user_name", "")?.trim() ?: ""
        val greeting = if (name.isNotEmpty()) {
            getString(R.string.greeting_morning, name)
        } else {
            getString(R.string.greeting_default)
        }
        supportActionBar?.title = greeting
    }

    /** Verifica e pede permissões críticas em cadeia. */
    private var permissionsDialogShowing = false

    private fun checkPermissions() {
        if (permissionsDialogShowing) return

        val missingExact = !PermissionManager.hasExactAlarmPermission(this)
        val missingBattery = !PermissionManager.isBatteryOptimizationIgnored(this)
        val missingOverlay = !PermissionManager.hasOverlayPermission(this)

        // Mostrar/ocultar banner
        binding.bannerPermissions.visibility = if (missingExact || missingBattery || missingOverlay) View.VISIBLE else View.GONE
        binding.bannerPermissions.setOnClickListener { showPermissionDialog() }
    }

    private fun showPermissionDialog() {
        if (permissionsDialogShowing) return
        permissionsDialogShowing = true

        if (!PermissionManager.hasExactAlarmPermission(this)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.perm_exact_alarm_title)
                .setMessage(R.string.perm_exact_alarm_body)
                .setPositiveButton(R.string.perm_action_allow) { _, _ ->
                    permissionsDialogShowing = false
                    PermissionManager.requestExactAlarmPermission(this)
                }
                .setNegativeButton(R.string.permission_skip, null)
                .setOnDismissListener { permissionsDialogShowing = false }
                .show()
            return
        }

        if (!PermissionManager.isBatteryOptimizationIgnored(this)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.perm_battery_title)
                .setMessage(R.string.perm_battery_body)
                .setPositiveButton(R.string.perm_action_allow) { _, _ ->
                    permissionsDialogShowing = false
                    PermissionManager.requestIgnoreBatteryOptimizations(this)
                }
                .setNegativeButton(R.string.permission_skip, null)
                .setOnDismissListener { permissionsDialogShowing = false }
                .show()
            return
        }

        if (!PermissionManager.hasOverlayPermission(this)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.perm_battery_title)
                .setMessage(R.string.perm_notifications_body)
                .setPositiveButton(R.string.perm_action_settings) { _, _ ->
                    permissionsDialogShowing = false
                    PermissionManager.requestOverlayPermission(this)
                }
                .setNegativeButton(R.string.permission_skip, null)
                .setOnDismissListener { permissionsDialogShowing = false }
                .show()
            return
        }

        permissionsDialogShowing = false
    }
}
