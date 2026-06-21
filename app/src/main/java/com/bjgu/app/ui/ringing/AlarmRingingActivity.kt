package com.bjgu.app.ui.ringing

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bjgu.app.R
import com.bjgu.app.alarm.AlarmScheduler
import com.bjgu.app.challenges.ChallengeGenerator
import com.bjgu.app.challenges.Difficulty
import com.bjgu.app.challenges.MathChallenge
import com.bjgu.app.challenges.QrCodeUtil
import com.bjgu.app.databinding.ActivityAlarmRingingBinding
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
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
    private var challengeType: Int = 0
    private var qrCodeHash: String? = null

    // Shake to Wake
    private var sensorManager: SensorManager? = null
    private var shakeCount = 0
    private var shakeTarget = 0
    private var lastShakeTime = 0L
    private var shakeEnabled = false

    // QR code fallback timer
    private val handler = Handler(Looper.getMainLooper())
    private var fallbackRunnable: Runnable? = null
    private var accountabilityRunnable: Runnable? = null

    // Animações
    private var pulseAnimator: ValueAnimator? = null

    /** Launcher para o scanner QR Code (ZXing). */
    private val qrScanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            // Se qrCodeHash é null → modo "Any QR", aceita qualquer scan
            val expectedContent = if (qrCodeHash != null) {
                QrCodeUtil.buildQrContent(alarmId, qrCodeHash!!)
            } else {
                null  // null = aceitar qualquer QR
            }
            if (expectedContent == null || result.contents == expectedContent) {
                onQrCodeCorrect()
            } else {
                binding.textFeedback.text = getString(R.string.qr_wrong_code)
                binding.textFeedback.setTextColor(getColor(R.color.error))
                binding.textFeedback.visibility = View.VISIBLE
            }
        }
    }

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
        challengeType = intent.getIntExtra("challenge_type", 0)
        qrCodeHash = intent.getStringExtra("qr_code_hash")

        // Marcar tempo de início
        alarmStartTimeMs = SystemClock.elapsedRealtime()

        // ── Modo escalada: ajustar UI ──
        if (escalated) {
            binding.textEscalatedBanner.visibility = View.VISIBLE
            // Forçar dificuldade máxima na escalada
            difficulty = 2
        }

        // ── Iniciar som e vibração ──
        startAlarmSound()
        startVibration()

        // ── Gradiente pulsante de fundo ──
        startPulseAnimation()

        // ── Iniciar timer de accountability (SMS se não desligar a tempo) ──
        startAccountabilityTimer()

        // ── QR Code mode ──
        if (challengeType == 2) {
            startQrCodeMode()
            return
        }

        // ── Shake to Wake ou desafio direto ──
        if (challengeType == 1) {
            startShakeToWake()
        } else {
            // Mostrar desafio diretamente
            binding.cardChallenge.visibility = View.VISIBLE
            binding.buttonsRow.visibility = View.VISIBLE
            generateNewChallenge()
        }

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

    // ─── QR Code mode ──────────────────────────────────────────────

    /** Inicia o modo QR Code: esconde desafio, mostra scan. */
    private fun startQrCodeMode() {
        binding.qrSection.visibility = View.VISIBLE
        binding.textQuestion.visibility = View.GONE
        binding.inputAnswerLayout.visibility = View.GONE
        binding.btnCheck.visibility = View.GONE
        binding.btnStop.visibility = View.GONE

        binding.btnScanQr.setOnClickListener {
            val options = ScanOptions().apply {
                setPrompt(getString(R.string.scan_qr_code))
                setBeepEnabled(false)
                setOrientationLocked(true)
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            }
            qrScanLauncher.launch(options)
        }

        binding.btnQrFallback.setOnClickListener {
            startFallbackChallenge()
        }

        // Mostrar fallback após 2 minutos
        fallbackRunnable = Runnable {
            binding.btnQrFallback.visibility = View.VISIBLE
        }
        handler.postDelayed(fallbackRunnable!!, 2 * 60 * 1000L)
    }

    /** QR code correto → mostrar botão de desligar. */
    private fun onQrCodeCorrect() {
        handler.removeCallbacks(fallbackRunnable ?: return)
        binding.textFeedback.text = getString(R.string.qr_correct)
        binding.textFeedback.setTextColor(getColor(R.color.success))
        binding.textFeedback.visibility = View.VISIBLE

        binding.qrSection.visibility = View.GONE
        binding.btnCheck.visibility = View.GONE
        binding.btnSnooze.visibility = View.GONE
        binding.btnStop.visibility = View.VISIBLE
        binding.btnStop.alpha = 1f
        binding.btnStop.isEnabled = true
        binding.btnStop.isClickable = true
    }

    /** Fallback: QR não disponível → desafio matemático difícil. */
    private fun startFallbackChallenge() {
        handler.removeCallbacks(fallbackRunnable ?: return)
        binding.qrSection.visibility = View.GONE
        binding.textQuestion.visibility = View.VISIBLE
        binding.inputAnswerLayout.visibility = View.VISIBLE
        binding.btnCheck.visibility = View.VISIBLE
        difficulty = 2
        generateNewChallenge()
    }

    // ─── Accountability ──────────────────────────────────────────────

    /** Inicia o timer que envia SMS se o alarme não for desligado a tempo. */
    private fun startAccountabilityTimer() {
        val prefs = getSharedPreferences("bjgu_prefs", MODE_PRIVATE)
        val phone = prefs.getString("accountability_phone", null)
        val timeoutMin = prefs.getInt("accountability_timeout_min", 5)

        if (phone.isNullOrBlank()) return  // Nenhum contacto configurado

        accountabilityRunnable = Runnable {
            try {
                @Suppress("DEPRECATION")
                val smsManager = android.telephony.SmsManager.getDefault()
                val message = getString(R.string.accountability_sms)
                smsManager.sendTextMessage(phone, null, message, null, null)
            } catch (_: Exception) {
                // Falha ao enviar SMS — ignorar silenciosamente
            }
        }
        handler.postDelayed(accountabilityRunnable!!, timeoutMin * 60 * 1000L)
    }

    private fun cancelAccountabilityTimer() {
        accountabilityRunnable?.let { handler.removeCallbacks(it) }
    }

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

    // ─── Animação de pulso ────────────────────────────────────────

    private fun startPulseAnimation() {
        pulseAnimator = ValueAnimator.ofFloat(0.15f, 0.28f).apply {
            duration = 3000L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animator ->
                binding.gradientOverlay.alpha = animator.animatedValue as Float
            }
            start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
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
        binding.textQuestion.setTextColor(getColor(com.google.android.material.R.attr.colorOnSurface))

        binding.btnStop.visibility = View.GONE
        binding.btnCheck.visibility = View.VISIBLE
        binding.btnCheck.isEnabled = true
    }

    private fun checkAnswer() {
        val input = binding.inputAnswer.text?.toString()?.trim() ?: ""
        if (input.isEmpty()) return

        val userAnswer = input.toIntOrNull()
        if (userAnswer == null) {
            shakeQuestion()
            return
        }

        val challenge = currentChallenge ?: return
        val isCorrect = challengeGenerator.checkAnswer(challenge, userAnswer)

        if (isCorrect) {
            onCorrectAnswer()
        } else {
            shakeQuestion()
            generateNewChallenge()
        }
    }

    /** Animação de negação (shake horizontal) na pergunta. */
    private fun shakeQuestion() {
        ObjectAnimator.ofFloat(
            binding.textQuestion, "translationX",
            0f, -18f, 18f, -10f, 10f, -5f, 5f, 0f
        ).apply {
            duration = 380
            start()
        }
        binding.textQuestion.setTextColor(getColor(R.color.error))
        binding.textQuestion.postDelayed({
            binding.textQuestion.setTextColor(getColor(com.google.android.material.R.attr.colorOnSurface))
        }, 400)
    }

    /** Chamado quando o utilizador acerta a resposta. */
    private fun onCorrectAnswer() {
        binding.textQuestion.setTextColor(getColor(R.color.success))
        binding.textFeedback.text = getString(R.string.correct_answer)
        binding.textFeedback.setTextColor(getColor(R.color.success))
        binding.textFeedback.visibility = View.VISIBLE

        binding.btnCheck.visibility = View.GONE
        binding.btnSnooze.visibility = View.GONE
        binding.btnStop.visibility = View.VISIBLE
        binding.btnStop.alpha = 1f
        binding.btnStop.isEnabled = true
        binding.btnStop.isClickable = true
        binding.inputAnswer.isEnabled = false

        binding.btnStop.scaleX = 0.85f
        binding.btnStop.scaleY = 0.85f
        binding.btnStop.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setInterpolator(OvershootInterpolator(1.5f))
            .start()
    }

    private fun showFeedback(correct: Boolean) {
        binding.textFeedback.text = getString(R.string.wrong_answer)
        binding.textFeedback.setTextColor(getColor(R.color.error))
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

        cancelAccountabilityTimer()  // Snooze conta como interacção

        // Agendar one-shot para daqui a 5 min
        AlarmScheduler.scheduleOneShotAlarm(
            context = this,
            alarmId = alarmId,
            difficulty = difficulty,
            soundUri = soundUri,
            delayMs = SNOOZE_DELAY_MS,
            escalated = escalated,
            snoozeCount = snoozeCount + 1,
            challengeType = challengeType,
            qrCodeHash = qrCodeHash
        )

        // Fechar esta Activity sem cancelar o alarme original
        stopAlarmSound()
        stopVibration()
        finishAndRemoveTaskSafely()
    }

    // ─── Shake to Wake ───────────────────────────────────────────────

    /** Inicia o modo Shake to Wake: esconde desafio, mostra contagem de shakes. */
    private fun startShakeToWake() {
        shakeTarget = when (difficulty) {
            0 -> 5
            1 -> 8
            2 -> 12
            else -> 5
        }
        shakeCount = 0
        shakeEnabled = true

        binding.shakeSection.visibility = View.VISIBLE
        binding.cardChallenge.visibility = View.GONE
        binding.buttonsRow.visibility = View.GONE
        binding.textShakeProgress.text = "0 / $shakeTarget"

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager?.registerListener(
            shakeListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME  // 20ms — rápido o suficiente
        )
    }

    /** Listener do acelerómetro para detetar shakes. */
    private val shakeListener = object : SensorEventListener {
        private val SHAKE_THRESHOLD = 12f  // Força G mínima para contar como shake
        private val SHAKE_COOLDOWN_MS = 500L  // Tempo mínimo entre shakes

        override fun onSensorChanged(event: SensorEvent?) {
            if (!shakeEnabled || event == null) return

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gForce = Math.sqrt((x * x + y * y + z * z).toDouble()) - SensorManager.GRAVITY_EARTH

            if (gForce > SHAKE_THRESHOLD) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTime > SHAKE_COOLDOWN_MS) {
                    lastShakeTime = now
                    shakeCount++
                    runOnUiThread {
                        binding.textShakeProgress.text = "$shakeCount / $shakeTarget"
                    }

                    if (shakeCount >= shakeTarget) {
                        onShakeComplete()
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Não usado
        }
    }

    /** Chamado quando o utilizador completa o número de shakes necessário. */
    private fun onShakeComplete() {
        shakeEnabled = false
        sensorManager?.unregisterListener(shakeListener)

        runOnUiThread {
            binding.shakeSection.visibility = View.GONE
            binding.cardChallenge.visibility = View.VISIBLE
            binding.buttonsRow.visibility = View.VISIBLE
            generateNewChallenge()
        }
    }

    // ─── Desligar alarme ────────────────────────────────────────────

    /** Para o alarme. Verifica escalada se resolveram demasiado rápido. */
    private fun stopAlarm() {
        val elapsedMs = SystemClock.elapsedRealtime() - alarmStartTimeMs

        stopAlarmSound()
        stopVibration()

        CoroutineScope(Dispatchers.IO).launch {
            // Guardar evento para estatísticas
            val app = com.bjgu.app.BJGUApplication.instance
            app.alarmEventRepository.insert(
                com.bjgu.app.data.alarm.AlarmEventEntity(
                    alarmId = alarmId,
                    timestamp = System.currentTimeMillis(),
                    responseTimeMs = elapsedMs,
                    difficulty = difficulty,
                    wasEscalated = escalated
                )
            )

            // Se NÃO é escalado e resolveu demasiado rápido → disparar escalada
            if (!escalated && elapsedMs < MIN_SOLVE_TIME_MS) {
                val escalatedDifficulty = minOf(difficulty + 1, 2)
                AlarmScheduler.scheduleOneShotAlarm(
                    context = this@AlarmRingingActivity,
                    alarmId = alarmId,
                    difficulty = escalatedDifficulty,
                    soundUri = soundUri,
                    delayMs = ESCALATION_DELAY_MS,
                    escalated = true,
                    snoozeCount = 0,
                    challengeType = challengeType,
                    qrCodeHash = qrCodeHash
                )
            }

            AlarmScheduler.cancelAlarm(this@AlarmRingingActivity, alarmId)

            withContext(Dispatchers.Main) {
                finishAndRemoveTaskSafely()
            }
        }
    }

    private fun finishAndRemoveTaskSafely() {
        cancelAccountabilityTimer()
        handler.removeCallbacks(fallbackRunnable ?: return)
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
        stopPulseAnimation()
        sensorManager?.unregisterListener(shakeListener)
        handler.removeCallbacks(fallbackRunnable ?: return)
        cancelAccountabilityTimer()
    }
}
