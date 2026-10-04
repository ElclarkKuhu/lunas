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
import androidx.compose.ui.res.stringResource
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
    val backupSuccessText = stringResource(R.string.settings_backup_success)
    val backupFailureFormat = stringResource(R.string.settings_backup_fail)
    val biometricPromptTitle = stringResource(R.string.settings_biometric_prompt_title)
    val biometricPromptSubtitle = stringResource(R.string.settings_biometric_prompt_subtitle)
    val verificationFailureFormat = stringResource(R.string.settings_verification_fail)
    val biometricUnavailableText = stringResource(R.string.settings_biometric_unavailable)
    val noLinkAppText = stringResource(R.string.settings_no_link_app)
    val backupCopiedText = stringResource(R.string.settings_copy_backup_success)
    val appVersion = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
    }

    val settings by repository.settings.collectAsStateWithLifecycle()
    val services by repository.services.collectAsStateWithLifecycle()

    var showAiImportDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var showOpenSourceNotice by remember { mutableStateOf(false) }
    var showPrivacyNotice by remember { mutableStateOf(false) }
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
                    Toast.makeText(context, backupSuccessText, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        String.format(Locale.getDefault(), backupFailureFormat, e.message ?: ""),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
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
                            contentDescription = stringResource(R.string.common_back),
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
            PreferenceSection(title = stringResource(R.string.settings_security_privacy)) {
                PreferenceCard {
                    PreferenceSwitchRow(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.settings_biometric_title),
                        subtitle = stringResource(R.string.settings_biometric_subtitle),
                        checked = settings.biometricEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (BiometricPromptHelper.canAuthenticate(context)) {
                                    BiometricPromptHelper.showPrompt(
                                        activity = activity,
                                        title = biometricPromptTitle,
                                        subtitle = biometricPromptSubtitle,
                                        onSuccess = {
                                            coroutineScope.launch {
                                                repository.updateSettings(settings.copy(biometricEnabled = true))
                                            }
                                        },
                                        onError = { err ->
                                            Toast.makeText(
                                                context,
                                                String.format(Locale.getDefault(), verificationFailureFormat, err),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, biometricUnavailableText, Toast.LENGTH_LONG).show()
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
            PreferenceSection(title = stringResource(R.string.settings_notifications)) {
                PreferenceCard {
                    PreferenceSwitchRow(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.settings_notification_title),
                        subtitle = stringResource(R.string.settings_notification_subtitle),
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
            PreferenceSection(title = stringResource(R.string.settings_services)) {
                PreferenceCard {
                    PreferenceClickableRow(
                        icon = Icons.Default.CreditCard,
                        title = stringResource(R.string.settings_manage_services),
                        subtitle = stringResource(R.string.settings_manage_services_subtitle, services.size),
                        onClick = onNavigateToManageServices
                    )
                }
            }

            // Section 4: Data & Cadangan
            PreferenceSection(title = stringResource(R.string.settings_data_backup)) {
                PreferenceCard {
                    PreferenceClickableRow(
                        icon = Icons.Default.AutoAwesome,
                        title = stringResource(R.string.settings_ai_import_title),
                        subtitle = stringResource(R.string.settings_ai_import_subtitle),
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = { showAiImportDialog = true }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    PreferenceClickableRow(
                        icon = Icons.Default.UploadFile,
                        title = stringResource(R.string.settings_export_json_title),
                        subtitle = stringResource(R.string.settings_export_json_subtitle),
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
                        title = stringResource(R.string.settings_restore_json_title),
                        subtitle = stringResource(R.string.settings_restore_json_subtitle),
                        onClick = { showRestoreDialog = true }
                    )
                }
            }

            // Section 5: Tentang Aplikasi
            PreferenceSection(title = stringResource(R.string.settings_about)) {
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
                                text = stringResource(R.string.common_lunas),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.settings_version_format, appVersion),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.settings_offline),
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
                        title = stringResource(R.string.settings_source_code),
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
                                Toast.makeText(context, noLinkAppText, Toast.LENGTH_LONG).show()
                            }
                        }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    PreferenceClickableRow(
                        icon = Icons.Default.Description,
                        title = stringResource(R.string.settings_open_source),
                        subtitle = stringResource(R.string.settings_open_source_desc),
                        onClick = { showOpenSourceNotice = true }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    PreferenceClickableRow(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.settings_privacy_policy),
                        subtitle = stringResource(R.string.settings_privacy_policy_desc),
                        onClick = { showPrivacyNotice = true }
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
                Text(stringResource(R.string.settings_export_dialog_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    stringResource(R.string.settings_export_dialog_desc),
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
                    Text(stringResource(R.string.settings_save_json_file))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExportOptionsDialog = false
                        coroutineScope.launch {
                            val clipEntry = ClipEntry(ClipData.newPlainText("lunas_backup", exportJsonContent))
                            clipboard.setClipEntry(clipEntry)
                            Toast.makeText(context, backupCopiedText, Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.common_copy_to_clipboard))
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
            title = { Text(stringResource(R.string.settings_mit_license), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(R.string.settings_mit_description),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { showOpenSourceNotice = false }) {
                    Text(stringResource(R.string.common_close))
                }
            }
        )
    }

    if (showPrivacyNotice) {
        AlertDialog(
            onDismissRequest = { showPrivacyNotice = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text(stringResource(R.string.settings_privacy_policy), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(R.string.settings_privacy_policy_text),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyNotice = false }) {
                    Text(stringResource(R.string.common_close))
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
