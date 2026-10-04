package id.elclark.lunas.ui.components

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

import id.elclark.lunas.model.PaylaterService
import id.elclark.lunas.util.AiPromptTemplates

@Composable
fun JsonImportDialog(
    onDismiss: () -> Unit,
    onImportJson: suspend (String, Boolean) -> Result<Int>,
    services: List<PaylaterService> = emptyList(),
    aiPrompt: String = remember(services) { AiPromptTemplates.generateExtractionPrompt(services) }
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    var importJsonText by remember { mutableStateOf("") }
    var mergeWithExisting by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Ekstraksi Tagihan AI", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Ekstraksi rincian screenshot via prompt AI dinamis", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // LLM Prompt Helper Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "CARA EKSTRAKSI DENGAN AI",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Kirim screenshot tagihan ke ChatGPT / Gemini / Claude dengan prompt otomatis yang sudah menyertakan daftar layanan Anda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (services.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Layanan Anda: ${services.joinToString(", ") { "${it.name} (${it.id})" }}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val clipEntry = ClipEntry(ClipData.newPlainText("ai_prompt", aiPrompt))
                                    clipboard.setClipEntry(clipEntry)
                                    Toast.makeText(context, "Prompt AI disalin! Tempelkan ke AI beserta gambar tagihan", Toast.LENGTH_LONG).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salin Prompt Dinamis untuk AI", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                }

                // JSON Input Text Area
                Text(
                    text = "TEMPEL JSON DARI AI DI SINI",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = importJsonText,
                    onValueChange = { importJsonText = it },
                    placeholder = {
                        Text(
                            text = "[{\n  \"service\": \"spaylater\",\n  \"title\": \"Cicilan HP\",\n  \"amount\": 250000,\n  \"tenor\": 3,\n  \"month\": \"2026-10\"\n}]",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    minLines = 4,
                    maxLines = 8,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Merge vs Replace Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = if (mergeWithExisting) "Gabungkan (Tambah ke data ada)" else "Ganti Seluruh Data",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (mergeWithExisting) "Tagihan lama tidak akan terhapus" else "PERINGATAN: Semua tagihan lama dihapus",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (mergeWithExisting) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                        )
                    }
                    Switch(
                        checked = mergeWithExisting,
                        onCheckedChange = { mergeWithExisting = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isProcessing) return@Button
                    isProcessing = true
                    coroutineScope.launch {
                        val result = onImportJson(importJsonText, mergeWithExisting)
                        result.onSuccess { count ->
                            Toast.makeText(context, "Berhasil menyuntikkan $count tagihan!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }.onFailure { err ->
                            Toast.makeText(context, "Gagal mengimpor: ${err.message}", Toast.LENGTH_LONG).show()
                        }
                        isProcessing = false
                    }
                },
                shape = RoundedCornerShape(10.dp),
                enabled = importJsonText.isNotBlank() && !isProcessing
            ) {
                Text(if (isProcessing) "Memproses..." else "Suntik Tagihan AI")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
