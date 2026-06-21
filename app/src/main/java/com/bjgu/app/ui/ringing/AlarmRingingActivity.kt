package com.bjgu.app.ui.ringing

import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.window.OnBackInvokedCallback
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
 * Características:
 *  - Abre por cima do ecrã de bloqueio (showWhenLocked + turnScreenOn).
 *  - Bloqueio absoluto do botão "voltar" e do gesto swipe-back (Android 10+).
 *  - Som do alarme em loop via Ringtone do sistema.
 *  - Vibração contínua com padrão repetitivo.
 *  - Desafio matemático que DEVE ser resolvido antes de mostrar o botão de desligar.
 *  - Se errar, gera nova pergunta (diferente em pergunta E resposta da anterior).
 */
class AlarmRingingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmRingingBinding

    // Dados recebidos do AlarmReceiver
    private var alarmId: Long = -1
    private var difficulty: Int = 0
    private var soundUri: String? = null

    // Som e vibração
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    // Desafio atual
    private var currentChallenge: MathChallenge? = null
    private var previousChallenge: MathChallenge? = null
    private val challengeGenerator = ChallengeGenerator()

    // Callback para bloquear o gesto "back" no Android 13+
    private var backInvokedCallback: OnBackInvokedCallback? = null

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

        // ── Iniciar som e vibração ──
        startAlarmSound()
        startVibration()

        // ── Gerar primeiro desafio ──
        generateNewChallenge()

        // ── Configurar botões ──
        binding.btnCheck.setOnClickListener { checkAnswer() }
        binding.btnStop.setOnClickListener { stopAlarm() }

        // ── Configurar Enter no teclado numérico ──
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

    /**
     * Bloqueia o botão "voltar" físico.
     * Sem chamar super.onBackPressed(), o botão é completamente ignorado.
     */
    override fun onBackPressed() {
        // Intencionalmente vazio — NÃO chamar super.onBackPressed()
    }

    /**
     * Captura teclas de sistema adicionais (volume, home, etc.).
     * O botão HOME não pode ser bloqueado no Android por razões de segurança,
     * mas podemos capturar KEYCODE_BACK e teclas de volume.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> true   // Bloquear back
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> true  // Bloquear mudança de volume
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

    /** Configura as flags de janela para abrir por cima do ecrã de bloqueio. */
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

        // Android 13+ (API 33+): Bloquear o gesto "swipe back"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backInvokedCallback = OnBackInvokedCallback {
                // Intencionalmente vazio — bloquear o gesto
            }
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                backInvokedCallback!!
            )
        }
    }

    // ─── Som ────────────────────────────────────────────────────────

    /** Inicia o som do alarme em loop. */
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

    /** Para o som do alarme. */
    private fun stopAlarmSound() {
        ringtone?.stop()
        ringtone = null
    }

    // ─── Vibração ───────────────────────────────────────────────────

    /** Inicia vibração contínua com padrão repetitivo. */
    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        // Padrão: vibra 500ms, pausa 300ms, repete
        val pattern = longArrayOf(0, 500, 300)
        val effect = VibrationEffect.createWaveform(pattern, 0) // 0 = repete infinito

        vibrator?.vibrate(effect)
    }

    /** Para a vibração. */
    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    // ─── Desafio ────────────────────────────────────────────────────

    /** Gera um novo desafio matemático garantindo que é diferente do anterior. */
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

        // Esconder botão de desligar até a resposta estar certa
        binding.btnStop.visibility = View.GONE
        binding.btnCheck.visibility = View.VISIBLE
        binding.btnCheck.isEnabled = true
    }

    /** Verifica a resposta do utilizador. */
    private fun checkAnswer() {
        val input = binding.inputAnswer.text?.toString()?.trim() ?: ""
        if (input.isEmpty()) return

        val userAnswer = input.toIntOrNull()
        if (userAnswer == null) {
            binding.textFeedback.text = getString(R.string.wrong_answer)
            binding.textFeedback.setTextColor(getColor(R.color.red_wrong))
            binding.textFeedback.visibility = View.VISIBLE
            return
        }

        val challenge = currentChallenge ?: return
        val isCorrect = challengeGenerator.checkAnswer(challenge, userAnswer)

        if (isCorrect) {
            // Resposta correta!
            binding.textFeedback.text = getString(R.string.correct_answer)
            binding.textFeedback.setTextColor(getColor(R.color.green_correct))
            binding.textFeedback.visibility = View.VISIBLE

            // Mostrar botão de desligar, esconder botão de verificar
            binding.btnCheck.visibility = View.GONE
            binding.btnStop.visibility = View.VISIBLE
            binding.inputAnswer.isEnabled = false
        } else {
            // Resposta errada — gerar nova pergunta
            binding.textFeedback.text = getString(R.string.wrong_answer)
            binding.textFeedback.setTextColor(getColor(R.color.red_wrong))
            binding.textFeedback.visibility = View.VISIBLE

            generateNewChallenge()
        }
    }

    // ─── Desligar alarme ────────────────────────────────────────────

    /** Para o alarme, cancela o agendamento e fecha a Activity. */
    private fun stopAlarm() {
        // Parar som e vibração
        stopAlarmSound()
        stopVibration()

        // Cancelar alarme no sistema
        CoroutineScope(Dispatchers.IO).launch {
            AlarmScheduler.cancelAlarm(this@AlarmRingingActivity, alarmId)

            withContext(Dispatchers.Main) {
                // Remover callback do back gesture (API 33+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    backInvokedCallback?.let {
                        onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)
                    }
                }

                // Fechar a Activity
                finishAndRemoveTask()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Garantir que som e vibração param mesmo se a Activity for destruída
        stopAlarmSound()
        stopVibration()
    }

    override fun onUserLeaveHint() {
        // Bloquear o comportamento de "home" — voltar a trazer a Activity ao topo
        // Nota: o Android não permite bloquear verdadeiramente o botão HOME,
        // mas podemos trazer a Activity de volta se o utilizador tentar sair.
    }
}
