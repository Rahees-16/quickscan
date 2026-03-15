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

enum class BarcodeFormat(val displayName: String, val zxingFormat: com.google.zxing.BarcodeFormat) {
    QR_CODE("QR Code", com.google.zxing.BarcodeFormat.QR_CODE),
    CODE_128("Code 128", com.google.zxing.BarcodeFormat.CODE_128),
    CODE_39("Code 39", com.google.zxing.BarcodeFormat.CODE_39),
    EAN_13("EAN-13", com.google.zxing.BarcodeFormat.EAN_13),
    EAN_8("EAN-8", com.google.zxing.BarcodeFormat.EAN_8),
    UPC_A("UPC-A", com.google.zxing.BarcodeFormat.UPC_A),
    PDF_417("PDF 417", com.google.zxing.BarcodeFormat.PDF_417),
    AZTEC("Aztec", com.google.zxing.BarcodeFormat.AZTEC),
    DATA_MATRIX("Data Matrix", com.google.zxing.BarcodeFormat.DATA_MATRIX),
}

private val LINEAR_FORMATS = setOf(
    BarcodeFormat.CODE_128, BarcodeFormat.CODE_39,
    BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.UPC_A
)

@HiltViewModel
class GeneratorViewModel @Inject constructor() : ViewModel() {

    private val _selectedFormat = MutableStateFlow(BarcodeFormat.QR_CODE)
    val selectedFormat: StateFlow<BarcodeFormat> = _selectedFormat.asStateFlow()

    private val _selectedType = MutableStateFlow(GeneratorType.TEXT)
    val selectedType: StateFlow<GeneratorType> = _selectedType.asStateFlow()

    private val _inputFields = MutableStateFlow<Map<String, String>>(emptyMap())
    val inputFields: StateFlow<Map<String, String>> = _inputFields.asStateFlow()

    private val _generatedBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedBitmap: StateFlow<Bitmap?> = _generatedBitmap.asStateFlow()

    private val _qrColor = MutableStateFlow(android.graphics.Color.BLACK)
    val qrColor: StateFlow<Int> = _qrColor.asStateFlow()

    private val _qrBackgroundColor = MutableStateFlow(android.graphics.Color.WHITE)
    val qrBackgroundColor: StateFlow<Int> = _qrBackgroundColor.asStateFlow()

    fun setFormat(format: BarcodeFormat) {
        _selectedFormat.value = format
        _inputFields.value = emptyMap()
        _generatedBitmap.value = null
    }

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
        val format = _selectedFormat.value
        val content = if (format == BarcodeFormat.QR_CODE) {
            buildQrContent()
        } else {
            _inputFields.value["text"]
        } ?: return
        if (content.isBlank()) return

        try {
            val isLinear = format in LINEAR_FORMATS
            val encodeWidth = if (isLinear) 600 else 512
            val encodeHeight = if (isLinear) 200 else 512

            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 2
            )
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                format.zxingFormat,
                encodeWidth,
                encodeHeight,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x, y,
                        if (bitMatrix.get(x, y)) _qrColor.value
                        else _qrBackgroundColor.value
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

    fun setQrColor(color: Int) {
        _qrColor.value = color
        // Re-generate if there's already a QR code displayed
        if (_generatedBitmap.value != null) {
            generateQr()
        }
    }

    fun setQrBackgroundColor(color: Int) {
        _qrBackgroundColor.value = color
        // Re-generate if there's already a QR code displayed
        if (_generatedBitmap.value != null) {
            generateQr()
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
