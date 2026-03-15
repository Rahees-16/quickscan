package com.rahees.quickscan.ui.generator

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

enum class GeneratorType {
    TEXT, URL, WIFI, CONTACT, EMAIL, PHONE
}

@HiltViewModel
class GeneratorViewModel @Inject constructor() : ViewModel() {

    private val _selectedType = MutableStateFlow(GeneratorType.TEXT)
    val selectedType: StateFlow<GeneratorType> = _selectedType.asStateFlow()

    private val _inputFields = MutableStateFlow<Map<String, String>>(emptyMap())
    val inputFields: StateFlow<Map<String, String>> = _inputFields.asStateFlow()

    private val _generatedBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedBitmap: StateFlow<Bitmap?> = _generatedBitmap.asStateFlow()

    fun selectType(type: GeneratorType) {
        _selectedType.value = type
        _inputFields.value = emptyMap()
        _generatedBitmap.value = null
    }

    fun updateField(key: String, value: String) {
        _inputFields.value = _inputFields.value.toMutableMap().apply {
            put(key, value)
        }
    }

    fun generateQr() {
        val content = buildQrContent() ?: return
        if (content.isBlank()) return

        try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 2
            )
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                512,
                512,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x, y,
                        if (bitMatrix.get(x, y)) android.graphics.Color.BLACK
                        else android.graphics.Color.WHITE
                    )
                }
            }
            _generatedBitmap.value = bitmap
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildQrContent(): String? {
        val fields = _inputFields.value
        return when (_selectedType.value) {
            GeneratorType.TEXT -> fields["text"]
            GeneratorType.URL -> {
                val url = fields["url"] ?: return null
                if (url.startsWith("http://") || url.startsWith("https://")) url
                else "https://$url"
            }
            GeneratorType.WIFI -> {
                val ssid = fields["ssid"] ?: return null
                val password = fields["password"] ?: ""
                val encryption = fields["encryption"] ?: "WPA"
                "WIFI:S:$ssid;T:$encryption;P:$password;;"
            }
            GeneratorType.CONTACT -> {
                val name = fields["name"] ?: return null
                val phone = fields["phone"] ?: ""
                val email = fields["email"] ?: ""
                buildString {
                    appendLine("BEGIN:VCARD")
                    appendLine("VERSION:3.0")
                    appendLine("FN:$name")
                    if (phone.isNotBlank()) appendLine("TEL:$phone")
                    if (email.isNotBlank()) appendLine("EMAIL:$email")
                    appendLine("END:VCARD")
                }
            }
            GeneratorType.EMAIL -> {
                val to = fields["to"] ?: return null
                val subject = fields["subject"] ?: ""
                val body = fields["body"] ?: ""
                "mailto:$to?subject=$subject&body=$body"
            }
            GeneratorType.PHONE -> {
                val phone = fields["phone"] ?: return null
                "tel:$phone"
            }
        }
    }

    fun saveToGallery(context: Context) {
        val bitmap = _generatedBitmap.value ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "QuickScan_QR_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QuickScan")
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                Toast.makeText(context, "Saved to gallery", Toast.LENGTH_SHORT).show()
            }
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "QuickScan"
            )
            dir.mkdirs()
            val file = File(dir, "QuickScan_QR_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Toast.makeText(context, "Saved to gallery", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareBitmap(context: Context) {
        val bitmap = _generatedBitmap.value ?: return

        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "qr_code.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
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
        context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
    }
}
