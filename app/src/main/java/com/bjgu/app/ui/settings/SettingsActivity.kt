package com.bjgu.app.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.bjgu.app.R
import com.bjgu.app.databinding.ActivitySettingsBinding

/**
 * Ecrã de definições.
 *
 * Permite configurar o contacto e o tempo limite para o modo accountability.
 * Guarda usando SharedPreferences.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    companion object {
        const val PREF_PHONE = "accountability_phone"
        const val PREF_TIMEOUT_MIN = "accountability_timeout_min"
        const val DEFAULT_TIMEOUT_MIN = 5
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.settings_title)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // Carregar valores guardados
        binding.inputPhone.setText(prefs.getString(PREF_PHONE, ""))
        val savedTimeout = prefs.getInt(PREF_TIMEOUT_MIN, DEFAULT_TIMEOUT_MIN)
        binding.seekTimeout.progress = savedTimeout - 1  // seekbar 0..28 → 1..29 min
        binding.textTimeoutValue.text = "${savedTimeout} min"

        binding.seekTimeout.setOnSeekBarChangeListener(
            object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    val minutes = progress + 1  // min 1, max 29
                    binding.textTimeoutValue.text = "${minutes} min"
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            }
        )

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
}
