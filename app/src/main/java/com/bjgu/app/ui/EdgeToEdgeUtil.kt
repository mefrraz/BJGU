package com.bjgu.app.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Utilitário para edge-to-edge — barras do sistema transparentes
 * com padding automático para não colidir com conteúdo.
 */
object EdgeToEdgeUtil {

    /**
     * Aplica edge-to-edge à Activity.
     * As barras de status e navegação ficam transparentes,
     * e o conteúdo recebe padding via WindowInsets.
     */
    fun setup(activity: androidx.appcompat.app.AppCompatActivity, rootView: View) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
    }
}
