package id.elclark.lunas.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun RestoreBackupDialog(
    onDismiss: () -> Unit,
    onRestoreBackup: suspend (String, Boolean) -> Result<Int>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var backupJsonText by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var replaceAllData by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val content = stream?.bufferedReader()?.use { it.readText() }
                    if (!content.isNullOrBlank()) {
                        backupJsonText = content
                        selectedFileName = uri.lastPathSegment?.substringAfterLast("/") ?: "File Cadangan.json"
                        Toast.makeText(context, "File cadangan berhasil dibaca!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membuka file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

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
                            Icons.Default.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Pulihkan Cadangan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Kembalikan layanan, tagihan & riwayat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                // File Picker Action
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "FILE CADANGAN (.JSON)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedFileName != null) "File terpilih: $selectedFileName" else "Pilih file .json cadangan dari memori perangkat Anda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                openDocLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (selectedFileName != null) "Ganti File Cadangan" else "Pilih File Cadangan (.json)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                // Or manual text paste
                Text(
                    text = "ATAU TEMPEL TEKS JSON CADANGAN",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = backupJsonText,
                    onValueChange = { backupJsonText = it },
                    placeholder = { Text("{\"version\": 1, \"services\": [...], \"items\": [...]}") },
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 6,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Mode switch: Replace vs Merge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = if (replaceAllData) "Ganti Seluruh Data (Rekomendasi)" else "Gabungkan Data",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (replaceAllData) "Data layanan & tagihan dipulihkan persis seperti saat dicadangkan" else "Data baru akan digabung ke data yang saat ini ada",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = replaceAllData,
                        onCheckedChange = { replaceAllData = it }
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
                        val result = onRestoreBackup(backupJsonText, !replaceAllData)
                        result.onSuccess { count ->
                            Toast.makeText(context, "Berhasil memulihkan cadangan ($count tagihan & layanan)!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }.onFailure { err ->
                            Toast.makeText(context, "Gagal memulihkan: ${err.message}", Toast.LENGTH_LONG).show()
                        }
                        isProcessing = false
                    }
                },
                shape = RoundedCornerShape(10.dp),
                enabled = backupJsonText.isNotBlank() && !isProcessing
            ) {
                Text(if (isProcessing) "Memulihkan..." else "Pulihkan Data")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
