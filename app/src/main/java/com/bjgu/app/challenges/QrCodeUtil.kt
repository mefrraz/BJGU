package com.bjgu.app.challenges

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Utilitário para geração de QR Codes.
 *
 * Gera um QR code com um hash único por alarme,
 * permitindo ao utilizador imprimi-lo e colocá-lo noutra divisão.
 * O alarme só desliga quando o QR code correto for escaneado.
 */
object QrCodeUtil {

    private const val QR_SIZE = 400  // pixels
    private const val QR_PREFIX = "BJGU"

    /**
     * Gera um hash único para o QR code de um alarme.
     */
    fun generateHash(): String {
        return UUID.randomUUID().toString().take(8).uppercase()
    }

    /**
     * Constrói o conteúdo do QR code.
     * Formato: BJGU:alarmId:hash
     */
    fun buildQrContent(alarmId: Long, hash: String): String {
        return "$QR_PREFIX:$alarmId:$hash"
    }

    /**
     * Gera uma imagem Bitmap do QR code.
     *
     * @param content Texto a codificar no QR.
     * @return Bitmap do QR code, ou null se falhar.
     */
    fun generateQrBitmap(content: String): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val matrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE)
            val bitmap = Bitmap.createBitmap(QR_SIZE, QR_SIZE, Bitmap.Config.RGB_565)

            for (x in 0 until QR_SIZE) {
                for (y in 0 until QR_SIZE) {
                    val color = if (matrix[x, y]) android.graphics.Color.BLACK
                        else android.graphics.Color.WHITE
                    bitmap.setPixel(x, y, color)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Guarda o QR code como ficheiro PNG no cache e devolve o URI para partilhar.
     */
    fun saveAndShareQr(context: Context, alarmId: Long, hash: String) {
        val content = buildQrContent(alarmId, hash)
        val bitmap = generateQrBitmap(content) ?: return

        val cacheDir = File(context.cacheDir, "qrcodes")
        cacheDir.mkdirs()
        val file = File(cacheDir, "bjgu_qr_${alarmId}.png")
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Partilhar QR Code"))
    }
}
