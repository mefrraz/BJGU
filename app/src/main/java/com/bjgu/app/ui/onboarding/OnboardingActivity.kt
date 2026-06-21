package com.bjgu.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.bjgu.app.databinding.ActivityOnboardingBinding
import com.bjgu.app.ui.EdgeToEdgeUtil
import com.bjgu.app.ui.alarms.MainActivity
import com.bjgu.app.ui.create.CreateAlarmActivity

/**
 * Ecrã de onboarding — primeiro arranque da app.
 * Pede o nome ao utilizador e sugere criar o primeiro alarme.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    /** Aguarda o resultado da criação do alarme antes de ir para Main. */
    private val createAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Alarm criado (ou não) — seguir para Main
        goToMain()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Se já fez onboarding, saltar para Main
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean("onboarding_done", false)) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        EdgeToEdgeUtil.setup(this, binding.root)

        // Animar entrada
        binding.textOnboardingTitle.translationY = 40f
        binding.textOnboardingTitle.alpha = 0f
        binding.textOnboardingTitle.animate()
            .translationY(0f).alpha(1f)
            .setDuration(700)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        binding.textOnboardingSub.translationY = 30f
        binding.textOnboardingSub.alpha = 0f
        binding.textOnboardingSub.animate()
            .translationY(0f).alpha(1f)
            .setDuration(700).setStartDelay(150)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        binding.inputNameLayout.alpha = 0f
        binding.inputNameLayout.animate().alpha(1f).setDuration(500).setStartDelay(400).start()
        binding.btnOnboardingContinue.alpha = 0f
        binding.btnOnboardingContinue.animate().alpha(1f).setDuration(500).setStartDelay(550).start()

        binding.btnOnboardingContinue.setOnClickListener {
            val name = binding.inputName.text?.toString()?.trim() ?: ""
            saveName(name)
            createAlarmLauncher.launch(Intent(this, CreateAlarmActivity::class.java))
        }

        binding.btnOnboardingSkip.setOnClickListener {
            saveName("")
            goToMain()
        }
    }

    private fun saveName(name: String) {
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putString("user_name", name.take(20))
            .putBoolean("onboarding_done", true)
            .apply()
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
