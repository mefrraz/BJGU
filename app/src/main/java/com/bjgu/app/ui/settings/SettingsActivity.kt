package com.bjgu.app.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bjgu.app.R
import com.bjgu.app.databinding.ActivitySettingsBinding
import com.bjgu.app.ui.EdgeToEdgeUtil

/**
 * Ecrã de definições — accountability, idioma, sobre.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREF_PHONE = "accountability_phone"
        const val PREF_TIMEOUT_MIN = "accountability_timeout_min"
        const val PREF_LANGUAGE = "app_language"
        const val DEFAULT_TIMEOUT_MIN = 5
        const val GITHUB_URL = "https://github.com/mefrraz/BJGU"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        EdgeToEdgeUtil.setup(this, binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.settings_title)

        prefs = getSharedPreferences("bjgu_prefs", MODE_PRIVATE)

        // Carregar valores
        binding.inputPhone.setText(prefs.getString(PREF_PHONE, ""))
        val savedTimeout = prefs.getInt(PREF_TIMEOUT_MIN, DEFAULT_TIMEOUT_MIN)
        binding.seekTimeout.progress = savedTimeout - 1
        binding.textTimeoutValue.text = "${savedTimeout} min"

        // Idioma atual
        val currentLang = prefs.getString(PREF_LANGUAGE, "system") ?: "system"
        binding.btnLanguage.text = languageLabel(currentLang)

        binding.seekTimeout.setOnSeekBarChangeListener(
            object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    binding.textTimeoutValue.text = "${progress + 1} min"
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            }
        )

        // Seletor de idioma
        binding.btnLanguage.setOnClickListener {
            showLanguageDialog()
        }

        // Guardar
        binding.btnSaveSettings.setOnClickListener {
            val phone = binding.inputPhone.text?.toString()?.trim() ?: ""
            val timeoutMin = binding.seekTimeout.progress + 1

            prefs.edit()
                .putString(PREF_PHONE, phone)
                .putInt(PREF_TIMEOUT_MIN, timeoutMin)
                .apply()

            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showLanguageDialog() {
        val languages = listOf(
            "system" to getString(R.string.lang_system),
            "pt" to "Português (PT)",
            "pt-BR" to "Português (BR)",
            "en" to "English",
            "es" to "Español",
            "fr" to "Français"
        )
        val labels = languages.map { it.second }.toTypedArray()
        val currentLang = prefs.getString(PREF_LANGUAGE, "system") ?: "system"
        val checkedIndex = languages.indexOfFirst { it.first == currentLang }.coerceAtLeast(0)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.settings_language)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                val code = languages[which].first
                prefs.edit().putString(PREF_LANGUAGE, code).apply()
                binding.btnLanguage.text = languageLabel(code)
                Toast.makeText(this, R.string.lang_restart_hint, Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
            .show()
    }

    private fun languageLabel(code: String): String = when (code) {
        "pt" -> "Português (PT)"
        "pt-BR" -> "Português (BR)"
        "en" -> "English"
        "es" -> "Español"
        "fr" -> "Français"
        else -> getString(R.string.lang_system)
    }
}
