package com.rahees.quickscan.ui.scanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.ToneGenerator
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.rahees.quickscan.util.settingsDataStore
import kotlinx.coroutines.flow.map
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.rahees.quickscan.ui.components.ScanOverlay
import com.rahees.quickscan.util.BarcodeAnalyzer
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    onScanResult: (content: String, format: String, type: String) -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isFlashOn by viewModel.isFlashOn.collectAsStateWithLifecycle()
    val isMultiScanMode by viewModel.isMultiScanMode.collectAsStateWithLifecycle()
    val batchScans by viewModel.batchScans.collectAsStateWithLifecycle()
    val lastScanResult by viewModel.lastScanResult.collectAsStateWithLifecycle()

    var hasCameraPermission by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        permissionDenied = !isGranted
    }

    // Gallery image picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val image = InputImage.fromFilePath(context, it)
                val scanner = BarcodeScanning.getClient()
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        barcodes.firstOrNull()?.let { barcode ->
                            val content = barcode.rawValue ?: return@addOnSuccessListener
                            val displayValue = barcode.displayValue ?: content
                            val format = barcodeFormatToString(barcode.format)
                            viewModel.onBarcodeScanned(content, displayValue, format)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ScannerScreen", "Gallery barcode scan failed", e)
                    }
            } catch (e: Exception) {
                Log.e("ScannerScreen", "Failed to load image from gallery", e)
            }
        }
    }

    LaunchedEffect(Unit) {
        val permission = Manifest.permission.CAMERA
        val granted = ContextCompat.checkSelfPermission(context, permission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(permission)
        }
    }

    // Read scan behavior settings
    val vibrateOnScan by context.settingsDataStore.data.map { prefs ->
        prefs[booleanPreferencesKey("vibrate_on_scan")] ?: true
    }.collectAsStateWithLifecycle(initialValue = true)

    val autoCopy by context.settingsDataStore.data.map { prefs ->
        prefs[booleanPreferencesKey("auto_copy_clipboard")] ?: false
    }.collectAsStateWithLifecycle(initialValue = false)

    val playSound by context.settingsDataStore.data.map { prefs ->
        prefs[booleanPreferencesKey("play_sound_on_scan")] ?: false
    }.collectAsStateWithLifecycle(initialValue = false)

    LaunchedEffect(lastScanResult) {
        lastScanResult?.let { result ->
            if (vibrateOnScan) vibrate(context)
            if (playSound) playBeep(context)
            if (autoCopy) copyToClipboard(context, result.content)
            onScanResult(result.content, result.format, result.type.name)
            viewModel.clearLastResult()
        }
    }

    if (!hasCameraPermission) {
        PermissionDeniedContent(
            onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(
                                Executors.newSingleThreadExecutor(),
                                BarcodeAnalyzer { barcode ->
                                    val content = barcode.rawValue ?: return@BarcodeAnalyzer
                                    val displayValue = barcode.displayValue ?: content
                                    val format = barcodeFormatToString(barcode.format)
                                    viewModel.onBarcodeScanned(content, displayValue, format)
                                }
                            )
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("ScannerScreen", "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        ScanOverlay(modifier = Modifier.fillMaxSize())

        // Top bar with flash control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "QuickScan",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Row {
                IconButton(onClick = {
                    viewModel.toggleFlash()
                    camera?.cameraControl?.enableTorch(!isFlashOn)
                }) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isMultiScanMode && batchScans.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Scanned: ${batchScans.size} codes",
                                style = MaterialTheme.typography.titleSmall
                            )
                            IconButton(onClick = { viewModel.clearBatch() }) {
                                Icon(Icons.Default.Clear, "Clear scans")
                            }
                        }
                        LazyColumn {
                            items(batchScans) { scan ->
                                Text(
                                    text = scan.displayValue.ifBlank { scan.content },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "Point your camera at a QR code or barcode",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Scan mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !isMultiScanMode,
                    onClick = { if (isMultiScanMode) viewModel.toggleScanMode() },
                    label = { Text("Single") },
                    leadingIcon = if (!isMultiScanMode) {
                        { Icon(Icons.Default.CropFree, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                FilterChip(
                    selected = isMultiScanMode,
                    onClick = { if (!isMultiScanMode) viewModel.toggleScanMode() },
                    label = { Text("Multi") },
                    leadingIcon = if (isMultiScanMode) {
                        { Icon(Icons.Default.DynamicFeed, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }

            Spacer(Modifier.height(12.dp))

            // Gallery pick button
            FloatingActionButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = "Pick from gallery"
                )
            }
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Camera permission is needed to scan QR codes and barcodes. Please grant the permission to continue.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        androidx.compose.material3.Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

private fun vibrate(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

private fun playBeep(context: Context) {
    try {
        val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        toneGenerator.release()
    } catch (_: Exception) {}
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Scanned content", text)
    clipboard.setPrimaryClip(clip)
}

private fun barcodeFormatToString(format: Int): String {
    return when (format) {
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_CODE_93 -> "CODE_93"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_PDF417 -> "PDF_417"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        else -> "UNKNOWN"
    }
}
