package com.rahees.quickscan.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rahees.quickscan.util.ContentType
import com.rahees.quickscan.util.parseVCard
import com.rahees.quickscan.util.parseWifiConfig

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartActionButtons(
    content: String,
    contentType: ContentType,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (contentType) {
            ContentType.URL -> {
                ActionButton(
                    icon = Icons.Default.Language,
                    label = "Open Browser",
                    onClick = {
                        val url = if (content.startsWith("http")) content else "https://$content"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                )
                ActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy Link",
                    onClick = { copyToClipboard(context, content) }
                )
            }

            ContentType.WIFI -> {
                val wifi = parseWifiConfig(content)
                ActionButton(
                    icon = Icons.Default.Wifi,
                    label = "Connect to WiFi",
                    onClick = {
                        val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                        context.startActivity(intent)
                    }
                )
                ActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy Password",
                    onClick = { copyToClipboard(context, wifi?.password ?: "") }
                )
            }

            ContentType.CONTACT -> {
                val contact = parseVCard(content)
                ActionButton(
                    icon = Icons.Default.Person,
                    label = "Add Contact",
                    onClick = {
                        val intent = Intent(Intent.ACTION_INSERT).apply {
                            type = ContactsContract.Contacts.CONTENT_TYPE
                            putExtra(ContactsContract.Intents.Insert.NAME, contact?.name ?: "")
                            putExtra(ContactsContract.Intents.Insert.PHONE, contact?.phone ?: "")
                            putExtra(ContactsContract.Intents.Insert.EMAIL, contact?.email ?: "")
                        }
                        context.startActivity(intent)
                    }
                )
                if (!contact?.phone.isNullOrBlank()) {
                    ActionButton(
                        icon = Icons.Default.Phone,
                        label = "Call",
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact?.phone}"))
                            context.startActivity(intent)
                        }
                    )
                }
                if (!contact?.email.isNullOrBlank()) {
                    ActionButton(
                        icon = Icons.Default.Email,
                        label = "Email",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact?.email}"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            ContentType.PHONE -> {
                val phoneNumber = content.removePrefix("tel:")
                ActionButton(
                    icon = Icons.Default.Phone,
                    label = "Call",
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                        context.startActivity(intent)
                    }
                )
                ActionButton(
                    icon = Icons.Default.Message,
                    label = "Send SMS",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber"))
                        context.startActivity(intent)
                    }
                )
                ActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy",
                    onClick = { copyToClipboard(context, phoneNumber) }
                )
            }

            ContentType.EMAIL -> {
                val email = content.removePrefix("mailto:")
                ActionButton(
                    icon = Icons.Default.Email,
                    label = "Send Email",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                        context.startActivity(intent)
                    }
                )
                ActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy",
                    onClick = { copyToClipboard(context, email) }
                )
            }

            ContentType.SMS -> {
                val smsContent = content.removePrefix("smsto:").removePrefix("sms:")
                val parts = smsContent.split(":", limit = 2)
                val number = parts.getOrNull(0) ?: ""
                ActionButton(
                    icon = Icons.Default.Message,
                    label = "Send SMS",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
                        if (parts.size > 1) {
                            intent.putExtra("sms_body", parts[1])
                        }
                        context.startActivity(intent)
                    }
                )
                ActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy",
                    onClick = { copyToClipboard(context, content) }
                )
            }

            ContentType.GEO -> {
                ActionButton(
                    icon = Icons.Default.Map,
                    label = "Open Maps",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(content))
                        context.startActivity(intent)
                    }
                )
                ActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy",
                    onClick = { copyToClipboard(context, content) }
                )
            }

            ContentType.TEXT -> {
                ActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy",
                    onClick = { copyToClipboard(context, content) }
                )
                ActionButton(
                    icon = Icons.Default.Search,
                    label = "Search Web",
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/search?q=${Uri.encode(content)}")
                        )
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    FilledTonalButton(onClick = onClick) {
        Icon(icon, contentDescription = label)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("QuickScan", text))
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}
