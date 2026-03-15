package com.rahees.quickscan.ui.generator

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    viewModel: GeneratorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedFormat by viewModel.selectedFormat.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val inputFields by viewModel.inputFields.collectAsStateWithLifecycle()
    val generatedBitmap by viewModel.generatedBitmap.collectAsStateWithLifecycle()
    val qrColor by viewModel.qrColor.collectAsStateWithLifecycle()
    val qrBackgroundColor by viewModel.qrBackgroundColor.collectAsStateWithLifecycle()

    val types = GeneratorType.entries.toList()
    val formats = BarcodeFormat.entries.toList()
    val isQrCode = selectedFormat == BarcodeFormat.QR_CODE

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isExpanded = screenWidthDp > 840

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (isQrCode) "Generate QR Code" else "Generate Barcode") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Barcode format selector
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(formats) { format ->
                    FilterChip(
                        selected = selectedFormat == format,
                        onClick = { viewModel.setFormat(format) },
                        label = { Text(format.displayName) }
                    )
                }
            }

            // Type selector tabs (only for QR Code)
            if (isQrCode) {
                ScrollableTabRow(
                    selectedTabIndex = types.indexOf(selectedType),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    types.forEach { type ->
                        Tab(
                            selected = selectedType == type,
                            onClick = { viewModel.selectType(type) },
                            text = {
                                Text(
                                    when (type) {
                                        GeneratorType.TEXT -> "Text"
                                        GeneratorType.URL -> "URL"
                                        GeneratorType.WIFI -> "WiFi"
                                        GeneratorType.CONTACT -> "Contact"
                                        GeneratorType.EMAIL -> "Email"
                                        GeneratorType.PHONE -> "Phone"
                                    }
                                )
                            }
                        )
                    }
                }
            }

            if (isExpanded) {
                // Tablet: form and preview side by side
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Form side
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isQrCode) {
                            GeneratorFormFields(selectedType, inputFields, viewModel)
                        } else {
                            BarcodeTextInput(selectedFormat, inputFields, viewModel)
                        }

                        Button(
                            onClick = { viewModel.generateQr() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Generate")
                        }
                    }

                    // Preview side
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        generatedBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Generated Code",
                                modifier = Modifier.size(300.dp)
                            )

                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.saveToGallery(context) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Download, "Save")
                                    Spacer(Modifier.width(8.dp))
                                    Text("Save")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.shareBitmap(context) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, "Share")
                                    Spacer(Modifier.width(8.dp))
                                    Text("Share")
                                }
                            }

                            if (isQrCode) {
                                Spacer(Modifier.height(16.dp))

                                QrColorPickerSection(
                                    selectedQrColor = qrColor,
                                    selectedBackgroundColor = qrBackgroundColor,
                                    onQrColorSelected = { viewModel.setQrColor(it) },
                                    onBackgroundColorSelected = { viewModel.setQrBackgroundColor(it) }
                                )
                            }
                        } ?: Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Code preview will appear here",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Phone: vertical scroll layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isQrCode) {
                            GeneratorFormFields(selectedType, inputFields, viewModel)
                        } else {
                            BarcodeTextInput(selectedFormat, inputFields, viewModel)
                        }

                        Button(
                            onClick = { viewModel.generateQr() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Generate")
                        }

                        generatedBitmap?.let { bitmap ->
                            Spacer(Modifier.height(8.dp))

                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Generated Code",
                                modifier = Modifier
                                    .size(256.dp)
                                    .align(Alignment.CenterHorizontally)
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.saveToGallery(context) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Download, "Save")
                                    Spacer(Modifier.width(8.dp))
                                    Text("Save")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.shareBitmap(context) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, "Share")
                                    Spacer(Modifier.width(8.dp))
                                    Text("Share")
                                }
                            }

                            if (isQrCode) {
                                Spacer(Modifier.height(16.dp))

                                QrColorPickerSection(
                                    selectedQrColor = qrColor,
                                    selectedBackgroundColor = qrBackgroundColor,
                                    onQrColorSelected = { viewModel.setQrColor(it) },
                                    onBackgroundColorSelected = { viewModel.setQrBackgroundColor(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneratorFormFields(
    selectedType: GeneratorType,
    inputFields: Map<String, String>,
    viewModel: GeneratorViewModel
) {
    when (selectedType) {
        GeneratorType.TEXT -> {
            OutlinedTextField(
                value = inputFields["text"] ?: "",
                onValueChange = { viewModel.updateField("text", it) },
                label = { Text("Enter text") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        GeneratorType.URL -> {
            OutlinedTextField(
                value = inputFields["url"] ?: "",
                onValueChange = { viewModel.updateField("url", it) },
                label = { Text("Enter URL") },
                placeholder = { Text("https://example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        GeneratorType.WIFI -> {
            OutlinedTextField(
                value = inputFields["ssid"] ?: "",
                onValueChange = { viewModel.updateField("ssid", it) },
                label = { Text("SSID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = inputFields["password"] ?: "",
                onValueChange = { viewModel.updateField("password", it) },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            var expanded by remember { mutableStateOf(false) }
            val encryptionOptions = listOf("WPA", "WEP", "None")
            val currentEncryption = inputFields["encryption"] ?: "WPA"

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = currentEncryption,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Encryption") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    encryptionOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.updateField("encryption", option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        GeneratorType.CONTACT -> {
            OutlinedTextField(
                value = inputFields["name"] ?: "",
                onValueChange = { viewModel.updateField("name", it) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = inputFields["phone"] ?: "",
                onValueChange = { viewModel.updateField("phone", it) },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = inputFields["email"] ?: "",
                onValueChange = { viewModel.updateField("email", it) },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        GeneratorType.EMAIL -> {
            OutlinedTextField(
                value = inputFields["to"] ?: "",
                onValueChange = { viewModel.updateField("to", it) },
                label = { Text("To") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = inputFields["subject"] ?: "",
                onValueChange = { viewModel.updateField("subject", it) },
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = inputFields["body"] ?: "",
                onValueChange = { viewModel.updateField("body", it) },
                label = { Text("Body") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        GeneratorType.PHONE -> {
            OutlinedTextField(
                value = inputFields["phone"] ?: "",
                onValueChange = { viewModel.updateField("phone", it) },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun BarcodeTextInput(
    format: BarcodeFormat,
    inputFields: Map<String, String>,
    viewModel: GeneratorViewModel
) {
    val text = inputFields["text"] ?: ""
    val isDigitsOnly = format in listOf(
        BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.UPC_A
    )

    val (label, hint, errorMessage) = when (format) {
        BarcodeFormat.EAN_13 -> Triple("EAN-13 Data", "Enter 12 or 13 digits",
            if (text.isNotEmpty() && !text.matches(Regex("^\\d{12,13}$"))) "EAN-13 requires 12 or 13 digits" else null)
        BarcodeFormat.EAN_8 -> Triple("EAN-8 Data", "Enter 7 or 8 digits",
            if (text.isNotEmpty() && !text.matches(Regex("^\\d{7,8}$"))) "EAN-8 requires 7 or 8 digits" else null)
        BarcodeFormat.UPC_A -> Triple("UPC-A Data", "Enter 11 or 12 digits",
            if (text.isNotEmpty() && !text.matches(Regex("^\\d{11,12}$"))) "UPC-A requires 11 or 12 digits" else null)
        else -> Triple("Barcode Data", "Enter text or numbers", null)
    }

    OutlinedTextField(
        value = text,
        onValueChange = { viewModel.updateField("text", it) },
        label = { Text(label) },
        placeholder = { Text(hint) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = errorMessage != null,
        supportingText = errorMessage?.let { msg -> { Text(msg) } },
        keyboardOptions = if (isDigitsOnly) KeyboardOptions(keyboardType = KeyboardType.Number)
        else KeyboardOptions.Default
    )
}

@Composable
private fun QrColorPickerSection(
    selectedQrColor: Int,
    selectedBackgroundColor: Int,
    onQrColorSelected: (Int) -> Unit,
    onBackgroundColorSelected: (Int) -> Unit
) {
    val qrColors = listOf(
        Color.Black to "Black",
        Color(0xFF1565C0) to "Blue",
        Color(0xFFC62828) to "Red",
        Color(0xFF2E7D32) to "Green",
        Color(0xFF6A1B9A) to "Purple",
        Color(0xFFE65100) to "Orange",
        Color(0xFF00897B) to "Teal",
        Color(0xFFAD1457) to "Pink"
    )

    val backgroundColors = listOf(
        Color.White to "White",
        Color(0xFFBBDEFB) to "Light Blue",
        Color(0xFFFFF9C4) to "Light Yellow",
        Color(0xFFC8E6C9) to "Light Green",
        Color(0xFFF8BBD0) to "Light Pink",
        Color(0xFFE0E0E0) to "Light Gray"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "QR Color",
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            qrColors.forEach { (color, label) ->
                val argb = color.toArgb()
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (argb == selectedQrColor)
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else
                                Modifier.border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                        )
                        .clickable { onQrColorSelected(argb) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Background",
            style = MaterialTheme.typography.labelLarge
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            backgroundColors.forEach { (color, label) ->
                val argb = color.toArgb()
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (argb == selectedBackgroundColor)
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else
                                Modifier.border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                        )
                        .clickable { onBackgroundColorSelected(argb) }
                )
            }
        }
    }
}
