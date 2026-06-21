package com.bjgu.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bjgu.app.databinding.ActivityLanguageBinding
import com.bjgu.app.ui.EdgeToEdgeUtil

class LanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        EdgeToEdgeUtil.setup(this, binding.root)

        binding.btnLangPt.setOnClickListener { selectAndGo("pt") }
        binding.btnLangEn.setOnClickListener { selectAndGo("en") }
        binding.btnLangEs.setOnClickListener { selectAndGo("es") }
        binding.btnLangFr.setOnClickListener { selectAndGo("fr") }
        binding.btnLangBr.setOnClickListener { selectAndGo("pt-BR") }
    }

    private fun selectAndGo(code: String) {
        getSharedPreferences("bjgu_prefs", MODE_PRIVATE)
            .edit().putString("app_language", code).apply()
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }
}
