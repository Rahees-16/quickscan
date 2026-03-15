package com.rahees.quickscan.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun RateAppDialog(
    onRateNow: () -> Unit,
    onLater: () -> Unit,
    onNever: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        icon = {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700)
            )
        },
        title = { Text("Enjoying QuickScan?") },
        text = { Text("If you enjoy using QuickScan, would you mind taking a moment to rate it? It really helps!") },
        confirmButton = {
            TextButton(onClick = onRateNow) {
                Text("Rate Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text("Later")
            }
            TextButton(onClick = onNever) {
                Text("Never")
            }
        }
    )
}
