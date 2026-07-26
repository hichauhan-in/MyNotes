package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.NeuIconButton
import com.example.ui.components.NeuSurface
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.brandGradientHorizontal

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val insets = WindowInsets.systemBars.asPaddingValues()
    var showAppInfo by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = insets.calculateTopPadding() + 8.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeuIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                size = 44.dp,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = insets.calculateBottomPadding() + 32.dp),
        ) {
            // ---- Appearance ----
            SettingsSection(title = "Appearance", icon = Icons.Rounded.ColorLens) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                ThemeModeSelector(
                    selected = settings.themeMode,
                    onSelect = viewModel::setThemeMode,
                )
                Spacer(Modifier.height(8.dp))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsDivider()
                    ToggleRow(
                        icon = Icons.Rounded.Brightness6,
                        title = "Dynamic colour",
                        subtitle = "Match your wallpaper (Material You)",
                        checked = settings.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---- Security ----
            SettingsSection(title = "Security & Privacy", icon = Icons.Rounded.Shield) {
                InfoBanner(
                    icon = Icons.Rounded.Lock,
                    text = "Every note is encrypted with AES-256 using a key stored in the Android Keystore. Plaintext never touches storage.",
                )
                Spacer(Modifier.height(14.dp))
                ToggleRow(
                    icon = Icons.Rounded.Fingerprint,
                    title = "App lock",
                    subtitle = "Require fingerprint or screen lock to open",
                    checked = settings.appLockEnabled,
                    onCheckedChange = viewModel::setAppLockEnabled,
                )
                SettingsDivider()
                ToggleRow(
                    icon = Icons.Rounded.VisibilityOff,
                    title = "Hide in recent apps",
                    subtitle = "Blur the preview in the app switcher",
                    checked = settings.hideFromRecents,
                    onCheckedChange = viewModel::setHideFromRecents,
                )
            }

            Spacer(Modifier.height(20.dp))

            // ---- Cloud ----
            SettingsSection(title = "Backup & Sync", icon = Icons.Rounded.CloudUpload) {
                ToggleRow(
                    icon = Icons.Rounded.CloudUpload,
                    title = "Encrypted cloud sync",
                    subtitle = "Back up to Google Drive - encrypted only",
                    checked = settings.cloudSyncEnabled,
                    onCheckedChange = viewModel::setCloudSyncEnabled,
                )
                AnimatedVisibility(visible = settings.cloudSyncEnabled) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        InfoBanner(
                            icon = Icons.Rounded.Shield,
                            text = "Only encrypted blobs are uploaded. Your encryption keys never leave this device, so no one - not even us - can read your notes.",
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---- Export ----
            SettingsSection(title = "Export", icon = Icons.Rounded.FolderOpen) {
                val exportContext = LocalContext.current
                val exportFolder = settings.defaultExportFolder
                val exportPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocumentTree(),
                ) { uri ->
                    if (uri != null) {
                        runCatching {
                            exportContext.contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                            )
                        }
                        viewModel.setDefaultExportFolder(uri.toString())
                    }
                }
                SettingsLinkRow(
                    icon = Icons.Rounded.FolderOpen,
                    title = "Default export folder",
                    subtitle = exportFolder?.let { exportFolderLabel(it) } ?: "Ask each time when exporting",
                    trailingIcon = Icons.Rounded.Edit,
                    onClick = { runCatching { exportPicker.launch(null) } },
                )
                if (exportFolder != null) {
                    SettingsDivider()
                    SettingsLinkRow(
                        icon = Icons.Rounded.Close,
                        title = "Clear default folder",
                        subtitle = "Go back to choosing a location each time",
                        onClick = { viewModel.setDefaultExportFolder(null) },
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Notes export as text, Markdown, HTML or PDF; a book exports as a ZIP that " +
                        "keeps its folder structure and attachments. Files save to this folder, or you'll " +
                        "be asked where to save each time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            // ---- About ----
            SettingsSection(title = "About", icon = Icons.Rounded.Info) {
                val context = LocalContext.current
                SettingsLinkRow(
                    icon = Icons.Rounded.Person,
                    title = "About the developer",
                    subtitle = "https://www.hichauhan.in/",
                    trailingIcon = Icons.Rounded.OpenInNew,
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.hichauhan.in/")),
                            )
                        }
                    },
                )
                SettingsDivider()
                SettingsLinkRow(
                    icon = Icons.Rounded.Info,
                    title = "Application Information",
                    subtitle = "Understanding MyNotes+",
                    trailingIcon = Icons.Rounded.ChevronRight,
                    onClick = { showAppInfo = true },
                )
            }
        }
    }

    if (showAppInfo) {
        AppInfoDialog(onDismiss = { showAppInfo = false })
    }
}

private fun exportFolderLabel(uriStr: String): String = runCatching {
    val id = DocumentsContract.getTreeDocumentId(Uri.parse(uriStr))
    id.substringAfter(':').ifBlank { id }
}.getOrDefault("Selected folder")

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    NeuSurface(
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val options = listOf(
        Triple(ThemeMode.SYSTEM, "System", Icons.Rounded.SettingsBrightness),
        Triple(ThemeMode.LIGHT, "Light", Icons.Rounded.LightMode),
        Triple(ThemeMode.DARK, "Dark", Icons.Rounded.DarkMode),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (mode, label, icon) ->
            val isSelected = mode == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(13.dp))
                    .then(
                        if (isSelected) Modifier.background(brandGradientHorizontal())
                        else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(mode) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun InfoBanner(
    icon: ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.10f))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SettingsLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingIcon: ImageVector? = null,
    trailingLabel: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            trailingLabel != null -> Text(
                text = trailingLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )

            trailingIcon != null -> Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    )
}

/**
 * A full-screen "About the app" popup with a little breathing room around the edges. It explains
 * what MyNotes+ is for, why it's useful, what's inside, and how it keeps notes private.
 */
@Composable
private fun AppInfoDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val insets = WindowInsets.systemBars.asPaddingValues()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = insets.calculateTopPadding() + 20.dp,
                    bottom = insets.calculateBottomPadding() + 20.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                // Branded header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brandGradientHorizontal())
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "MyNotes+",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = "Private notes, truly yours",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Scrollable body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 22.dp),
                ) {
                    AppInfoSection(icon = Icons.Rounded.AutoAwesome, title = "Our mission") {
                        AppInfoParagraph(
                            "MyNotes+ is built on one idea: your notes belong to you and no one else. " +
                                "It's a calm, private home for everything on your mind - with no accounts, no ads, " +
                                "and no data mining. Just a fast, beautiful place to write, plan and remember.",
                        )
                    }
                    AppInfoSection(icon = Icons.Rounded.Bolt, title = "Why you'll love it") {
                        AppInfoBullet("Truly private - every note is encrypted on your device.")
                        AppInfoBullet("Works fully offline, so it's instant wherever you are.")
                        AppInfoBullet("No account, no tracking, no ads. Ever.")
                        AppInfoBullet("Organise your way with books, tags, pins and colours.")
                    }
                    AppInfoSection(icon = Icons.Rounded.Widgets, title = "What's inside") {
                        AppInfoBullet("Notes, checklists, tables and callouts.")
                        AppInfoBullet("Whiteboards, expense trackers and spreadsheets.")
                        AppInfoBullet("Photos, voice notes and reusable templates.")
                        AppInfoBullet("A recoverable Trash so nothing is lost by accident.")
                    }
                    AppInfoSection(icon = Icons.Rounded.Shield, title = "Privacy & safety") {
                        AppInfoParagraph(
                            "Every note is locked with AES-256-GCM using a key held in your device's " +
                                "hardware-backed keystore. Plaintext is never written to disk and never leaves your " +
                                "device. Optional cloud backup uploads only encrypted data - the keys always stay " +
                                "with you, so no one, not even us, can read your notes.",
                        )
                    }
                    AppInfoSection(icon = Icons.Rounded.Lock, title = "In your control") {
                        AppInfoParagraph(
                            "Add an optional fingerprint or screen-lock to open the app, blur the preview in the " +
                                "recent-apps switcher, and delete anything whenever you like. It's your space, on your terms.",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppInfoSection(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    Spacer(Modifier.height(10.dp))
    Column(Modifier.fillMaxWidth()) { content() }
    Spacer(Modifier.height(22.dp))
}

@Composable
private fun AppInfoParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AppInfoBullet(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}
