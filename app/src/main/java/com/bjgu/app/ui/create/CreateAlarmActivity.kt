package com.bjgu.app.ui.create

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bjgu.app.R
import com.bjgu.app.alarm.AlarmScheduler
import com.bjgu.app.challenges.QrCodeUtil
import com.bjgu.app.data.alarm.AlarmEntity
import com.bjgu.app.databinding.ActivityCreateAlarmBinding
import com.bjgu.app.ui.EdgeToEdgeUtil
import com.google.android.material.chip.Chip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Ecrã de criação de um novo alarme.
 *
 * Permite ao utilizador definir:
 *  - Hora (TimePicker spinner)
 *  - Dias da semana (Chips multi-seleção)
 *  - Som (RingtoneManager do sistema)
 *  - Dificuldade do desafio (RadioGroup: fácil/médio/difícil)
 *
 * Ao guardar, insere na base de dados e agenda no AlarmManager.
 */
class CreateAlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateAlarmBinding

    /** URI do som selecionado (null = som padrão do sistema). */
    private var selectedSoundUri: String? = null

    /** Hash do QR code (gerado quando o modo QR é ativado). */
    private var qrCodeHash: String? = null

    /** ID do alarme em edição (null = criação nova). */
    private var editingAlarmId: Long? = null

    /** Tipo de desafio selecionado (0=Mat, 1=Shake, 2=QR). */
    private var selectedChallengeType = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        EdgeToEdgeUtil.setup(this, binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Verificar modo edição
        editingAlarmId = intent.getLongExtra("alarm_id", -1).takeIf { it != -1L }
        if (editingAlarmId != null) {
            title = getString(R.string.edit_alarm_title)
            loadAlarmForEditing(editingAlarmId!!)
        }

        // Inicializar NumberPickers
        binding.pickerHour.minValue = 0
        binding.pickerHour.maxValue = 23
        binding.pickerHour.value = 7
        binding.pickerMinute.minValue = 0
        binding.pickerMinute.maxValue = 59
        binding.pickerMinute.value = 0

        binding.pickerHour.setOnValueChangedListener { _, _, newVal -> updateClockDisplay() }
        binding.pickerMinute.setOnValueChangedListener { _, _, newVal -> updateClockDisplay() }
        updateClockDisplay()

        // Criar chips dos dias da semana
        createDayChips()

        // Cards de tipo de desafio
        setupChallengeCards()

        // Botão para escolher som
        binding.btnSelectSound.setOnClickListener {
            openRingtonePicker()
        }

        binding.btnShareQr.setOnClickListener {
            qrCodeHash?.let {
                QrCodeUtil.saveAndShareQr(this, editingAlarmId ?: 0L, it)
            }
        }

        // QR preview pode aparecer inline nas options_qr

        // Botão Guardar
        binding.btnSave.setOnClickListener {
            saveAlarm()
        }
    }

    // ─── Métodos privados ────────────────────────────────────────────

    /** Cria os 7 chips para os dias da semana (Seg a Dom). */
    private fun createDayChips() {
        val dayNames = listOf(
            R.string.mon_short,  // Seg
            R.string.tue_short,  // Ter
            R.string.wed_short,  // Qua
            R.string.thu_short,  // Qui
            R.string.fri_short,  // Sex
            R.string.sat_short,  // Sáb
            R.string.sun_short   // Dom
        )

        for ((index, nameRes) in dayNames.withIndex()) {
            val chip = Chip(this).apply {
                text = getString(nameRes)
                isCheckable = true
                isChecked = (index in 1..5)  // Seg–Sex pré-selecionados
                tag = index  // Guardamos o índice do bit (0=Dom…6=Sáb)
            }
            binding.chipGroupDays.addView(chip)
        }
    }

    /** Abre o seletor de toques do sistema para escolher o som do alarme. */
    private fun openRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.select_sound))
            // Se já tiver um som selecionado, pré-seleciona-o
            if (selectedSoundUri != null) {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(selectedSoundUri))
            }
        }
        startActivityForResult(intent, RINGTONE_REQUEST_CODE)
    }

    @Deprecated("Usar registerForActivityResult seria preferível, mas mantemos por simplicidade.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RINGTONE_REQUEST_CODE && resultCode == RESULT_OK) {
            val uri: Uri? = data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            selectedSoundUri = uri?.toString()

            // Mostrar nome do som selecionado
            val ringtone = if (uri != null) {
                RingtoneManager.getRingtone(this, uri)
            } else null

            binding.textSoundName.text = ringtone?.getTitle(this)
                ?: getString(R.string.system_default)
        }
    }

    /** Valida, cria e persiste o alarme. */
    private fun saveAlarm() {
        val hour = binding.pickerHour.value
        val minute = binding.pickerMinute.value

        val daysOfWeek = buildDaysBitmask()
        if (daysOfWeek == 0) {
            Toast.makeText(this, R.string.select_days_error, Toast.LENGTH_SHORT).show()
            return
        }

        val difficulty = when (binding.toggleDifficulty.checkedButtonId) {
            R.id.btn_easy -> 0
            R.id.btn_medium -> 1
            R.id.btn_hard -> 2
            else -> 0
        }

        // 4. Criar entidade
        val alarm = AlarmEntity(
            id = 0,
            hour = hour,
            minute = minute,
            daysOfWeek = daysOfWeek,
            difficulty = difficulty,
            enabled = true,
            alarmSoundUri = selectedSoundUri,
            challengeType = selectedChallengeType,
            qrCodeHash = if (selectedChallengeType == 2) qrCodeHash else null
        )

        // 5. Guardar na BD e agendar (em background)
        val app = com.bjgu.app.BJGUApplication.instance
        CoroutineScope(Dispatchers.IO).launch {
            val savedAlarm = if (editingAlarmId != null) {
                val updated = alarm.copy(id = editingAlarmId!!)
                app.alarmRepository.update(updated)
                updated
            } else {
                val newId = app.alarmRepository.insert(alarm)
                alarm.copy(id = newId)
            }

            withContext(Dispatchers.Main) {
                AlarmScheduler.scheduleAlarm(this@CreateAlarmActivity, savedAlarm)
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    /**
     * Constrói o bitmask de dias da semana a partir dos chips selecionados.
     * Bit 0 = Domingo … Bit 6 = Sábado.
     */
    private fun buildDaysBitmask(): Int {
        var mask = 0
        for (i in 0 until binding.chipGroupDays.childCount) {
            val chip = binding.chipGroupDays.getChildAt(i) as Chip
            if (chip.isChecked) {
                val bitIndex = chip.tag as Int  // 0=Dom, 1=Seg, …, 6=Sáb
                mask = mask or (1 shl bitIndex)
            }
        }
        return mask
    }

    /** Mostra preview do QR code num ImageView. */
    private fun showQrPreview(alarmId: Long) {
        val hash = qrCodeHash ?: return
        val content = QrCodeUtil.buildQrContent(alarmId, hash)
        val bitmap = QrCodeUtil.generateQrBitmap(content)
        if (bitmap != null) {
            binding.imgQrPreview.setImageBitmap(bitmap)
            binding.imgQrPreview.visibility = View.VISIBLE
            binding.btnShareQr.visibility = View.VISIBLE
        }
    }

    /** Configura os 3 cards de tipo de desafio. */
    private fun setupChallengeCards() {
        fun selectCard(type: Int) {
            selectedChallengeType = type
            val accent = getColor(R.color.accent)
            val surfaceHigh = getColor(R.color.surface_high)
            binding.cardMath.strokeColor = if (type == 0) accent else surfaceHigh
            binding.cardMath.strokeWidth = if (type == 0) 2 else 1
            binding.cardShake.strokeColor = if (type == 1) accent else surfaceHigh
            binding.cardShake.strokeWidth = if (type == 1) 2 else 1
            binding.cardQr.strokeColor = if (type == 2) accent else surfaceHigh
            binding.cardQr.strokeWidth = if (type == 2) 2 else 1
            binding.optionsMath.visibility = if (type == 0) View.VISIBLE else View.GONE
            binding.optionsQr.visibility = if (type == 2) View.VISIBLE else View.GONE
            if (type == 2 && qrCodeHash == null) {
                qrCodeHash = QrCodeUtil.generateHash()
                showQrPreview(editingAlarmId ?: 0L)
            }
        }
        binding.cardMath.setOnClickListener { selectCard(0) }
        binding.cardShake.setOnClickListener { selectCard(1) }
        binding.cardQr.setOnClickListener { selectCard(2) }
    }

    /** Atualiza o display do relógio digital no topo. */
    private fun updateClockDisplay() {
        val h = binding.pickerHour.value
        val m = binding.pickerMinute.value
        binding.textClockDisplay.text = String.format("%02d:%02d", h, m)
    }

    /** Carrega os dados de um alarme existente para edição. */
    private fun loadAlarmForEditing(alarmId: Long) {
        val app = com.bjgu.app.BJGUApplication.instance
        CoroutineScope(Dispatchers.IO).launch {
            val alarm = app.alarmRepository.getAlarmById(alarmId) ?: return@launch
            withContext(Dispatchers.Main) {
                binding.pickerHour.value = alarm.hour
                binding.pickerMinute.value = alarm.minute
                updateClockDisplay()

                // Selecionar dias
                for (i in 0 until binding.chipGroupDays.childCount) {
                    val chip = binding.chipGroupDays.getChildAt(i) as com.google.android.material.chip.Chip
                    val bitIndex = chip.tag as Int
                    chip.isChecked = (alarm.daysOfWeek and (1 shl bitIndex)) != 0
                }

                // Dificuldade
                binding.toggleDifficulty.check(
                    when (alarm.difficulty) {
                        0 -> R.id.btn_easy
                        1 -> R.id.btn_medium
                        2 -> R.id.btn_hard
                        else -> R.id.btn_easy
                    }
                )

                // Som
                selectedSoundUri = alarm.alarmSoundUri
                if (!alarm.alarmSoundUri.isNullOrEmpty()) {
                    val ringtone = RingtoneManager.getRingtone(this@CreateAlarmActivity, Uri.parse(alarm.alarmSoundUri))
                    binding.textSoundName.text = ringtone?.getTitle(this@CreateAlarmActivity) ?: getString(R.string.system_default)
                }

                // Tipo de desafio
                selectedChallengeType = alarm.challengeType
                when (alarm.challengeType) {
                    0 -> binding.cardMath.performClick()
                    1 -> binding.cardShake.performClick()
                    2 -> {
                        binding.cardQr.performClick()
                        if (!alarm.qrCodeHash.isNullOrEmpty()) {
                            qrCodeHash = alarm.qrCodeHash
                            showQrPreview(alarmId)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val RINGTONE_REQUEST_CODE = 200
    }
}
