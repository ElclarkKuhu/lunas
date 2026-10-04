package id.elclark.lunas.ui.settings

import android.content.ClipData
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.elclark.lunas.R
import id.elclark.lunas.data.LunasRepository
import id.elclark.lunas.security.BiometricPromptHelper
import id.elclark.lunas.theme.LunasTheme
import id.elclark.lunas.ui.components.JsonImportDialog
import id.elclark.lunas.ui.components.RestoreBackupDialog
import id.elclark.lunas.util.AiPromptTemplates
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class SettingsActivity : FragmentActivity() {

    private lateinit var repository: LunasRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = LunasRepository.getInstance(applicationContext)

        enableEdgeToEdge()
        setContent {
            LunasTheme {
                SettingsScreen(
                    repository = repository,
                    activity = this,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: LunasRepository,
    activity: FragmentActivity,
    onNavigateBack: () -> Unit,
    onNavigateToManageServices: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    val settings by repository.settings.collectAsStateWithLifecycle()
    val services by repository.services.collectAsStateWithLifecycle()

    var showAiImportDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var showOpenSourceNotice by remember { mutableStateOf(false) }
    var exportJsonContent by remember { mutableStateOf("") }

    val exportDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(exportJsonContent.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "File cadangan berhasil disimpan!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal menyimpan file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pengaturan",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Keamanan
            PreferenceSection(title = "KEAMANAN & PRIVASI") {
                PreferenceCard {
                    PreferenceSwitchRow(
                        icon = Icons.Default.Lock,
                        title = "Kunci Biometrik / PIN",
                        subtitle = "Wajib sidik jari atau PIN saat membuka aplikasi",
                        checked = settings.biometricEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (BiometricPromptHelper.canAuthenticate(context)) {
                                    BiometricPromptHelper.showPrompt(
                                        activity = activity,
                                        title = "Verifikasi Sidik Jari",
                                        subtitle = "Konfirmasi untuk mengaktifkan kunci aplikasi",
                                        onSuccess = {
                                            coroutineScope.launch {
                                                repository.updateSettings(settings.copy(biometricEnabled = true))
                                            }
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, "Gagal verifikasi: $err", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "Perangkat belum mengaktifkan sidik jari atau PIN", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                coroutineScope.launch {
                                    repository.updateSettings(settings.copy(biometricEnabled = false))
                                }
                            }
                        }
                    )
                }
            }

            // Section 2: Notifikasi
            PreferenceSection(title = "PENGINGAT & NOTIFIKASI") {
                PreferenceCard {
                    PreferenceSwitchRow(
                        icon = Icons.Default.Notifications,
                        title = "Notifikasi Pengingat",
                        subtitle = "Pengingat otomatis saat tagihan dicetak dan H-2 sebelum jatuh tempo",
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { enable ->
                            coroutineScope.launch {
                                repository.updateSettings(settings.copy(notificationsEnabled = enable))
                            }
                        }
                    )
                }
            }

            // Section 3: Layanan Paylater
            PreferenceSection(title = "LAYANAN PAYLATER") {
                PreferenceCard {
                    PreferenceClickableRow(
                        icon = Icons.Default.CreditCard,
                        title = "Kelola Daftar Layanan",
                        subtitle = "${services.size} layanan aktif (atur tanggal cetak & jatuh tempo)",
                        onClick = onNavigateToManageServices
                    )
                }
            }

            // Section 4: Data & Cadangan
            PreferenceSection(title = "DATA & CADANGAN") {
                PreferenceCard {
                    PreferenceClickableRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "Ekstraksi Screenshot AI",
                        subtitle = "Ekstraksi gambar/screenshot via prompt AI dinamis dengan daftar layanan Anda",
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = { showAiImportDialog = true }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    PreferenceClickableRow(
                        icon = Icons.Default.UploadFile,
                        title = "Ekspor Cadangan (File JSON)",
                        subtitle = "Simpan file .json lengkap (layanan, tagihan, pengaturan)",
                        onClick = {
                            coroutineScope.launch {
                                exportJsonContent = repository.exportBackupJson()
                                showExportOptionsDialog = true
                            }
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    PreferenceClickableRow(
                        icon = Icons.Default.Restore,
                        title = "Pulihkan Cadangan (File JSON)",
                        subtitle = "Buka file .json cadangan atau tempel teks JSON penuh",
                        onClick = { showRestoreDialog = true }
                    )
                }
            }

            // Section 5: Tentang Aplikasi
            PreferenceSection(title = "TENTANG APLIKASI") {
                PreferenceCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_lunas_logo),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Lunas",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Versi 1.1 • Paylater & Cicilan Tracker",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "100% offline & data tersimpan aman di perangkat lokal Anda.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    PreferenceClickableRow(
                        icon = Icons.Default.Code,
                        title = "Kode Sumber",
                        subtitle = "github.com/ElclarkKuhu/lunas",
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/ElclarkKuhu/lunas")
                                    )
                                )
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(context, "Tidak ada aplikasi untuk membuka tautan.", Toast.LENGTH_LONG).show()
                            }
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    PreferenceClickableRow(
                        icon = Icons.Default.Description,
                        title = "Lisensi Open Source",
                        subtitle = "Lunas menggunakan lisensi MIT",
                        onClick = { showOpenSourceNotice = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showExportOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showExportOptionsDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    Icons.Default.UploadFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Ekspor Cadangan Data", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Cadangan mencakup seluruh daftar layanan paylater, riwayat tagihan, dan pengaturan aplikasi Anda.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportOptionsDialog = false
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        exportDocLauncher.launch("Lunas_Backup_$timestamp.json")
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan File .json")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExportOptionsDialog = false
                        coroutineScope.launch {
                            val clipEntry = ClipEntry(ClipData.newPlainText("lunas_backup", exportJsonContent))
                            clipboard.setClipEntry(clipEntry)
                            Toast.makeText(context, "Data cadangan JSON disalin ke clipboard!", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salin ke Clipboard")
                }
            }
        )
    }

    if (showAiImportDialog) {
        JsonImportDialog(
            onDismiss = { showAiImportDialog = false },
            onImportJson = { text, merge ->
                repository.importJsonData(text, merge)
            },
            services = services
        )
    }

    if (showRestoreDialog) {
        RestoreBackupDialog(
            onDismiss = { showRestoreDialog = false },
            onRestoreBackup = { text, merge ->
                repository.importJsonData(text, merge)
            }
        )
    }

    if (showOpenSourceNotice) {
        AlertDialog(
            onDismissRequest = { showOpenSourceNotice = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Lisensi MIT", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Lunas is open-source software licensed under the MIT License.\n\n" +
                        "Copyright (c) 2026 Elclark\n\n" +
                        "You may use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the software, provided this copyright notice and permission notice are included.\n\n" +
                        "The software is provided “as is”, without warranty of any kind. See the repository for the complete license text.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { showOpenSourceNotice = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
private fun PreferenceSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun PreferenceCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun PreferenceSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun PreferenceClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}
