package com.bjgu.app.ui.ringing

import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.appcompat.app.AppCompatActivity
import com.bjgu.app.R
import com.bjgu.app.alarm.AlarmScheduler
import com.bjgu.app.challenges.ChallengeGenerator
import com.bjgu.app.challenges.Difficulty
import com.bjgu.app.challenges.MathChallenge
import com.bjgu.app.databinding.ActivityAlarmRingingBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Ecrã de alarme a tocar — full-screen, inquebrável.
 *
 * v2.0:
 *  - Modo Escalada: se resolver demasiado rápido (<10s), dispara alarme
 *    de verificação 2 min depois com dificuldade máxima.
 *  - Snooze limitado: máximo 1 snooze de 5 min por disparo.
 *  - Estatísticas: regista tempo de resposta ao desligar.
 */
class AlarmRingingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmRingingBinding

    // Dados recebidos do AlarmReceiver
    private var alarmId: Long = -1
    private var difficulty: Int = 0
    private var soundUri: String? = null
    private var escalated: Boolean = false
    private var snoozeCount: Int = 0

    // Tempo de início (para detetar batota)
    private var alarmStartTimeMs: Long = 0

    // Som e vibração
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    // Desafio atual
    private var currentChallenge: MathChallenge? = null
    private var previousChallenge: MathChallenge? = null
    private val challengeGenerator = ChallengeGenerator()

    // Callback para bloquear o gesto "back" no Android 13+
    private var backInvokedCallback: OnBackInvokedCallback? = null

    companion object {
        /** Tempo mínimo (ms) para resolver o desafio sem disparar escalada. */
        private const val MIN_SOLVE_TIME_MS = 10_000L  // 10 segundos

        /** Atraso do alarme de escalada. */
        private const val ESCALATION_DELAY_MS = 2 * 60 * 1000L  // 2 minutos

        /** Atraso do snooze. */
        private const val SNOOZE_DELAY_MS = 5 * 60 * 1000L  // 5 minutos

        /** Máximo de snoozes permitidos. */
        private const val MAX_SNOOZES = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Configurar janela full-screen por cima do bloqueio ──
        setupWindowFlags()

        binding = ActivityAlarmRingingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ── Extrair dados do Intent ──
        alarmId = intent.getLongExtra("alarm_id", -1)
        difficulty = intent.getIntExtra("alarm_difficulty", 0)
        soundUri = intent.getStringExtra("alarm_sound_uri")
        escalated = intent.getBooleanExtra("escalated", false)
        snoozeCount = intent.getIntExtra("snooze_count", 0)

        // Marcar tempo de início
        alarmStartTimeMs = SystemClock.elapsedRealtime()

        // ── Modo escalada: ajustar UI ──
        if (escalated) {
            binding.textEscalatedBanner.visibility = View.VISIBLE
            binding.textTitle.text = getString(R.string.escalated_title)
            // Forçar dificuldade máxima na escalada
            difficulty = 2
        }

        // ── Iniciar som e vibração ──
        startAlarmSound()
        startVibration()

        // ── Gerar primeiro desafio ──
        generateNewChallenge()

        // ── Configurar botões ──
        binding.btnCheck.setOnClickListener { checkAnswer() }
        binding.btnStop.setOnClickListener { stopAlarm() }
        binding.btnSnooze.setOnClickListener { doSnooze() }

        // Se já usou todos os snoozes, esconder o botão
        if (snoozeCount >= MAX_SNOOZES) {
            binding.btnSnooze.visibility = View.GONE
        }

        // ── Enter no teclado numérico ──
        binding.inputAnswer.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                checkAnswer()
                true
            } else {
                false
            }
        }
    }

    // ─── Bloqueio absoluto do botão "voltar" ────────────────────────

    override fun onBackPressed() {
        // Intencionalmente vazio — NÃO chamar super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> true
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> true
            else -> super.onKeyUp(keyCode, event)
        }
    }

    // ─── Janela full-screen ─────────────────────────────────────────

    private fun setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backInvokedCallback = OnBackInvokedCallback {
                // Bloquear o gesto
            }
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                backInvokedCallback!!
            )
        }
    }

    // ─── Som ────────────────────────────────────────────────────────

    private fun startAlarmSound() {
        val uri = if (!soundUri.isNullOrEmpty()) {
            Uri.parse(soundUri)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }

        ringtone = RingtoneManager.getRingtone(this, uri)
        ringtone?.isLooping = true
        ringtone?.play()
    }

    private fun stopAlarmSound() {
        ringtone?.stop()
        ringtone = null
    }

    // ─── Vibração ───────────────────────────────────────────────────

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 500, 300)
        val effect = VibrationEffect.createWaveform(pattern, 0)
        vibrator?.vibrate(effect)
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    // ─── Desafio ────────────────────────────────────────────────────

    private fun generateNewChallenge() {
        val diff = when (difficulty) {
            0 -> Difficulty.EASY
            1 -> Difficulty.MEDIUM
            2 -> Difficulty.HARD
            else -> Difficulty.EASY
        }

        currentChallenge = challengeGenerator.generate(diff, previousChallenge)
        previousChallenge = currentChallenge

        binding.textQuestion.text = currentChallenge?.question ?: "?"
        binding.inputAnswer.setText("")
        binding.textFeedback.visibility = View.INVISIBLE

        binding.btnStop.visibility = View.GONE
        binding.btnCheck.visibility = View.VISIBLE
        binding.btnCheck.isEnabled = true
    }

    private fun checkAnswer() {
        val input = binding.inputAnswer.text?.toString()?.trim() ?: ""
        if (input.isEmpty()) return

        val userAnswer = input.toIntOrNull()
        if (userAnswer == null) {
            showFeedback(false)
            return
        }

        val challenge = currentChallenge ?: return
        val isCorrect = challengeGenerator.checkAnswer(challenge, userAnswer)

        if (isCorrect) {
            onCorrectAnswer()
        } else {
            showFeedback(false)
            generateNewChallenge()
        }
    }

    /** Chamado quando o utilizador acerta a resposta. */
    private fun onCorrectAnswer() {
        binding.textFeedback.text = getString(R.string.correct_answer)
        binding.textFeedback.setTextColor(getColor(R.color.green_correct))
        binding.textFeedback.visibility = View.VISIBLE

        // Mostrar botão de desligar
        binding.btnCheck.visibility = View.GONE
        binding.btnSnooze.visibility = View.GONE
        binding.btnStop.visibility = View.VISIBLE
        binding.inputAnswer.isEnabled = false
    }

    private fun showFeedback(correct: Boolean) {
        binding.textFeedback.text = getString(R.string.wrong_answer)
        binding.textFeedback.setTextColor(getColor(R.color.red_wrong))
        binding.textFeedback.visibility = View.VISIBLE
    }

    // ─── Snooze ─────────────────────────────────────────────────────

    /** Adia o alarme por 5 minutos (máximo 1 vez). */
    private fun doSnooze() {
        if (snoozeCount >= MAX_SNOOZES) {
            binding.btnSnooze.isEnabled = false
            binding.btnSnooze.text = getString(R.string.snooze_used)
            return
        }

        // Agendar one-shot para daqui a 5 min
        AlarmScheduler.scheduleOneShotAlarm(
            context = this,
            alarmId = alarmId,
            difficulty = difficulty,
            soundUri = soundUri,
            delayMs = SNOOZE_DELAY_MS,
            escalated = escalated,
            snoozeCount = snoozeCount + 1
        )

        // Fechar esta Activity sem cancelar o alarme original
        stopAlarmSound()
        stopVibration()
        finishAndRemoveTaskSafely()
    }

    // ─── Desligar alarme ────────────────────────────────────────────

    /** Para o alarme. Verifica escalada se resolveram demasiado rápido. */
    private fun stopAlarm() {
        val elapsedMs = SystemClock.elapsedRealtime() - alarmStartTimeMs

        stopAlarmSound()
        stopVibration()

        CoroutineScope(Dispatchers.IO).launch {
            // Se NÃO é escalado e resolveu demasiado rápido → disparar escalada
            if (!escalated && elapsedMs < MIN_SOLVE_TIME_MS) {
                val escalatedDifficulty = minOf(difficulty + 1, 2) // cap em HARD
                AlarmScheduler.scheduleOneShotAlarm(
                    context = this@AlarmRingingActivity,
                    alarmId = alarmId,
                    difficulty = escalatedDifficulty,
                    soundUri = soundUri,
                    delayMs = ESCALATION_DELAY_MS,
                    escalated = true,
                    snoozeCount = 0
                )
            }

            // Cancelar o alarme atual (só se não for recorrente — mantemos
            // os alarmes com daysOfWeek pois eles re-agendam automaticamente)
            AlarmScheduler.cancelAlarm(this@AlarmRingingActivity, alarmId)

            withContext(Dispatchers.Main) {
                finishAndRemoveTaskSafely()
            }
        }
    }

    private fun finishAndRemoveTaskSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backInvokedCallback?.let {
                onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)
            }
        }
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
        stopVibration()
    }
}
