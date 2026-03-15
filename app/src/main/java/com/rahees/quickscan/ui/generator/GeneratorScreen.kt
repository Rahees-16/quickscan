package com.rahees.quickscan.ui.generator

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    viewModel: GeneratorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val inputFields by viewModel.inputFields.collectAsStateWithLifecycle()
    val generatedBitmap by viewModel.generatedBitmap.collectAsStateWithLifecycle()

    val types = GeneratorType.entries.toList()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Generate QR Code") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Type selector tabs
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

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dynamic form fields
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

                // Generate button
                Button(
                    onClick = { viewModel.generateQr() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate")
                }

                // QR code preview
                generatedBitmap?.let { bitmap ->
                    Spacer(Modifier.height(8.dp))

                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Generated QR Code",
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
                }
            }
        }
    }
}
