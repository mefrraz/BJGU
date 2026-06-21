package com.bjgu.app.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bjgu.app.R
import com.bjgu.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREF_LANGUAGE = "app_language"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.settings_title)

        prefs = getSharedPreferences("bjgu_prefs", MODE_PRIVATE)
        val currentLang = prefs.getString(PREF_LANGUAGE, "system") ?: "system"
        binding.btnLanguage.text = languageLabel(currentLang)

        binding.btnLanguage.setOnClickListener { showLanguageDialog() }
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
