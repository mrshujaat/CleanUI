package com.example.mediabrowser.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mediabrowser.domain.model.DarkModeOption
import com.example.mediabrowser.domain.model.ImageQuality
import com.example.mediabrowser.domain.model.LayoutStyle
import com.example.mediabrowser.ui.components.appBackgroundGradient
import com.example.mediabrowser.ui.theme.parseHexColor

// Hardcoded donation link. Change this to your own URL.
private const val DONATE_URL = "https://ko-fi.com/shujaatbabar"

// Card background for the modal sheets (the grey box in the design).
private val ModalSurface = Color(0xFF2A2B2E)
private val SubtleText = Color(0xFF8A8D91)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsState()
    val accentColor = parseHexColor(settings.accentColorHex, Color(0xFF2DD4BF))

    var showAppearance by remember { mutableStateOf(false) }
    var showOthers by remember { mutableStateOf(false) }
    var showSitePicker by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    val context = androidx.compose.ui.platform.LocalContext.current
    val backupStatus by viewModel.backupStatus.collectAsState()

    // Holds the JSON to write once the user has picked a destination file.
    var pendingBackupJson by remember { mutableStateOf<String?>(null) }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val data = pendingBackupJson
        if (uri != null && data != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(data.toByteArray())
                }
            } catch (_: Exception) { }
        }
        pendingBackupJson = null
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)
                    ?.use { it.readBytes().decodeToString() } ?: ""
                if (text.isNotBlank()) viewModel.importBackup(text)
            } catch (_: Exception) { }
        }
    }

    // Surface backup/restore results as a toast.
    LaunchedEffect(backupStatus) {
        backupStatus?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearBackupStatus()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.appBackgroundGradient(accentColor, MaterialTheme.colorScheme.background)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
            )

            // Appearance → modal
            NavRow(
                title = "Appearance",
                subtitle = "Change how content appears on the application",
                onClick = { showAppearance = true }
            )

            SectionSpacer()

            // Image Quality (inline)
            Text(
                "Image Quality",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
            )
            Row(
                modifier = Modifier.padding(start = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ImageQuality.entries.forEach { quality ->
                    QualityPill(
                        label = quality.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = settings.imageQuality == quality,
                        accent = accentColor,
                        onClick = { viewModel.setImageQuality(quality) }
                    )
                }
            }

            SectionSpacer()

            // Home Feed type (inline) — Default trending vs personalized Poison Feed
            Text(
                "Home Feed",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(start = 20.dp, bottom = 4.dp)
            )
            Text(
                "Poison learns from what you view, search, favourite & download",
                color = Color(0xFF8A8D91),
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            )
            Row(
                modifier = Modifier.padding(start = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.example.mediabrowser.domain.model.FeedType.entries.forEach { type ->
                    QualityPill(
                        label = if (type == com.example.mediabrowser.domain.model.FeedType.POISON) "Poison" else "Default",
                        selected = settings.homeFeedType == type,
                        accent = accentColor,
                        onClick = { viewModel.setHomeFeedType(type) }
                    )
                }
            }

            SectionSpacer()

            // Sites — Rule34 (default) or any other supported booru. Switching
            // retargets the whole app (home, search, details) to that site.
            val currentSite = com.example.mediabrowser.domain.model.BooruSite.fromSettings(settings)
            NavRow(
                title = "Sites",
                subtitle = if (currentSite.isDefault) "Rule34 (default)" else "Others — ${currentSite.displayName}",
                onClick = { showSitePicker = true }
            )

            SectionSpacer()

            // Others → modal
            NavRow(
                title = "Others",
                subtitle = "Notification, Content, Autoplay, etc",
                onClick = { showOthers = true }
            )

            SectionSpacer()

            // API information (inline)
            Text(
                "API information",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
            )
            ApiField(
                label = "API KEY",
                value = settings.apiCredentialOne,
                onValueChange = { viewModel.setApiCredentialOne(it) }
            )
            Spacer(Modifier.height(16.dp))
            ApiField(
                label = "User ID",
                value = settings.apiCredentialTwo,
                onValueChange = { viewModel.setApiCredentialTwo(it) }
            )

            SectionSpacer()

            // Backup & Restore
            Text(
                "Backup & Restore",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(start = 20.dp, bottom = 4.dp)
            )
            Text(
                "Save your favourites, batches, tags, preferences & learned feed to a file you can restore after reinstalling",
                color = SubtleText,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            )
            Row(
                modifier = Modifier.padding(start = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QualityPill(
                    label = "Export",
                    selected = false,
                    accent = accentColor,
                    onClick = {
                        viewModel.exportBackup { jsonText ->
                            pendingBackupJson = jsonText
                            exportLauncher.launch("mediabrowser_backup.json")
                        }
                    }
                )
                QualityPill(
                    label = "Import",
                    selected = false,
                    accent = accentColor,
                    onClick = {
                        importLauncher.launch(arrayOf("application/json"))
                    }
                )
            }

            SectionSpacer()

            // Clear Cache
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showClearCacheDialog = true }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text("Clear Cache", color = Color.White, fontSize = 26.sp)
                Text(formatBytes(cacheSizeBytes) + " used", color = SubtleText, fontSize = 14.sp)
            }

            SectionSpacer()

            // Donate
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri(DONATE_URL) }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text("Donate", color = Color.White, fontSize = 26.sp)
                Text("Please support to keep the App running.", color = SubtleText, fontSize = 14.sp)
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showSitePicker) {
        SitePickerModal(
            current = com.example.mediabrowser.domain.model.BooruSite.fromSettings(settings),
            accent = accentColor,
            onSelect = { site ->
                viewModel.setSite(site)
                showSitePicker = false
                android.widget.Toast.makeText(
                    context,
                    if (site.isDefault) "Switched to Rule34" else "Switched to ${site.displayName}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { showSitePicker = false }
        )
    }

    if (showAppearance) {
        AppearanceModal(
            settings = settings,
            accent = accentColor,
            onDismiss = { showAppearance = false },
            onGridColumns = { viewModel.setGridColumns(it) },
            onCardCorners = { viewModel.setCardCornerRadius(it) },
            onHomeLayout = { viewModel.setHomeLayoutStyle(it) },
            onFavLayout = { viewModel.setFavoritesLayoutStyle(it) }
        )
    }

    if (showOthers) {
        OthersModal(
            settings = settings,
            accent = accentColor,
            onDismiss = { showOthers = false },
            onSafeSearch = { viewModel.setSafeSearch(it) },
            onWifiOnly = { viewModel.setWifiOnlyDownloads(it) },
            onNotifications = { viewModel.setNotificationsEnabled(it) },
            onAutoplay = { viewModel.setAutoPlayVideos(it) }
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear cache?") },
            text = { Text("This frees up ${formatBytes(cacheSizeBytes)} of cached images.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCache()
                    showClearCacheDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/** A tappable landing row: big title, subtitle, chevron. */
@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 26.sp)
            Text(subtitle, color = SubtleText, fontSize = 14.sp)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun SectionSpacer() {
    Spacer(Modifier.height(28.dp))
}

/** Low/Medium/High pill. */
@Composable
private fun QualityPill(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) accent else Color.White, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 10.dp)
    ) {
        Text(label, color = Color(0xFF111111), fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ApiField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = SubtleText,
                unfocusedBorderColor = Color(0xFF3A3D42),
                cursorColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Appearance modal — the grey sheet from the design. */
@Composable
private fun AppearanceModal(
    settings: com.example.mediabrowser.domain.model.AppSettings,
    accent: Color,
    onDismiss: () -> Unit,
    onGridColumns: (Int) -> Unit,
    onCardCorners: (Int) -> Unit,
    onHomeLayout: (LayoutStyle) -> Unit,
    onFavLayout: (LayoutStyle) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ModalSurface, RoundedCornerShape(20.dp))
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("Appearance", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp))

            // Dark / Light (disabled)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioDot(selected = true, accent = accent)
                Text("Dark", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(start = 10.dp, end = 28.dp))
                RadioDot(selected = false, accent = accent, enabled = false)
                Text("Light (Disabled)", color = SubtleText, fontSize = 16.sp, modifier = Modifier.padding(start = 10.dp))
            }

            Spacer(Modifier.height(20.dp))
            Text("Grid Columns = ${settings.gridColumns}", color = Color.White, fontSize = 16.sp)
            Slider(
                value = settings.gridColumns.toFloat(),
                onValueChange = { onGridColumns(it.toInt()) },
                valueRange = 1f..4f,
                steps = 2,
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Color(0xFF5A5D62))
            )

            Spacer(Modifier.height(8.dp))
            Text("Card Corners = ${settings.cardCornerRadiusDp}", color = Color.White, fontSize = 16.sp)
            Slider(
                value = settings.cardCornerRadiusDp.toFloat(),
                onValueChange = { onCardCorners(it.toInt()) },
                valueRange = 0f..32f,
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = Color(0xFF5A5D62))
            )

            Spacer(Modifier.height(20.dp))
            Text("Layout", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
            LayoutChoiceRow("Home", settings.homeLayoutStyle, accent, onHomeLayout)
            Spacer(Modifier.height(10.dp))
            LayoutChoiceRow("Favourites", settings.favoritesLayoutStyle, accent, onFavLayout)

            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Done", color = accent) }
            }
        }
    }
}

@Composable
private fun LayoutChoiceRow(label: String, current: LayoutStyle, accent: Color, onSelect: (LayoutStyle) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onSelect(LayoutStyle.GRID) }) {
            RadioDot(selected = current == LayoutStyle.GRID, accent = accent)
            Text("Grid", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp, end = 20.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onSelect(LayoutStyle.MASONRY) }) {
            RadioDot(selected = current == LayoutStyle.MASONRY, accent = accent)
            Text("Masonry", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** Others modal — ON/OFF toggles. */
@Composable
private fun OthersModal(
    settings: com.example.mediabrowser.domain.model.AppSettings,
    accent: Color,
    onDismiss: () -> Unit,
    onSafeSearch: (Boolean) -> Unit,
    onWifiOnly: (Boolean) -> Unit,
    onNotifications: (Boolean) -> Unit,
    onAutoplay: (Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ModalSurface, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text("Others", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp))

            OnOffRow("Safe Search", settings.safeSearchEnabled, accent, onSafeSearch)
            OnOffRow("Download over WiFi", settings.downloadOverWifiOnly, accent, onWifiOnly)
            OnOffRow("Notifications", settings.notificationsEnabled, accent, onNotifications)
            OnOffRow("Autoplay Videos", settings.autoPlayVideos, accent, onAutoplay)

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Done", color = accent) }
            }
        }
    }
}

@Composable
private fun OnOffRow(label: String, enabled: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(
            "ON",
            color = if (enabled) accent else SubtleText,
            fontSize = 16.sp,
            fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.clickable { onChange(true) }.padding(horizontal = 10.dp)
        )
        Text(
            "OFF",
            color = if (!enabled) accent else SubtleText,
            fontSize = 16.sp,
            fontWeight = if (!enabled) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.clickable { onChange(false) }.padding(horizontal = 10.dp)
        )
    }
}

/** Simple filled/hollow radio dot. */
@Composable
private fun RadioDot(selected: Boolean, accent: Color, enabled: Boolean = true) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(
                if (selected) accent else if (enabled) Color.White else Color(0xFF5A5D62),
                androidx.compose.foundation.shape.CircleShape
            )
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb < 1024) "%.1f MB".format(mb) else "%.2f GB".format(mb / 1024)
}

/** Sites picker — Rule34 (default) on top, then every other supported booru. */
@Composable
private fun SitePickerModal(
    current: com.example.mediabrowser.domain.model.BooruSite,
    accent: Color,
    onSelect: (com.example.mediabrowser.domain.model.BooruSite) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        val viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
        val settings by viewModel.settings.collectAsState()

        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .background(Color(0xFF1A1D1F), RoundedCornerShape(20.dp))
                .padding(vertical = 18.dp)
        ) {
            item {
                Text(
                    "Sites",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
            item {
                SiteRow(
                    label = "Rule34",
                    sub = "Default — full experience",
                    selected = current.isDefault,
                    accent = accent,
                    onClick = { onSelect(com.example.mediabrowser.domain.model.BooruSite.RULE34) }
                )
            }
            item {
                Text(
                    "Others",
                    color = SubtleText,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)
                )
            }
            items(com.example.mediabrowser.domain.model.BooruSite.OTHERS) { site ->
                SiteRow(
                    label = site.displayName,
                    sub = site.apiBaseUrl.removePrefix("https://").trimEnd('/'),
                    selected = current == site,
                    accent = accent,
                    onClick = { onSelect(site) }
                )
            }
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Site API keys",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
                Text(
                    "Optional for most. Gelbooru requires a key/user pair since 2023.",
                    color = SubtleText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
            items(com.example.mediabrowser.domain.model.BooruSite.entries.toList()) { site ->
                SiteCredentialCard(
                    site = site,
                    stored = com.example.mediabrowser.data.remote.SiteCredentialStore.get(settings, site),
                    accent = accent,
                    onSave = { key, user -> viewModel.setSiteCredential(site, key, user) }
                )
            }
        }
    }
}

@Composable
private fun SiteCredentialCard(
    site: com.example.mediabrowser.domain.model.BooruSite,
    stored: com.example.mediabrowser.data.remote.SiteApiCredential,
    accent: Color,
    onSave: (key: String, user: String) -> Unit
) {
    var key by remember(site.name, stored.key) { mutableStateOf(stored.key) }
    var user by remember(site.name, stored.user) { mutableStateOf(stored.user) }
    val dirty = key != stored.key || user != stored.user

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(site.displayName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            singleLine = true,
            label = { Text("API key", fontSize = 12.sp) },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = SubtleText, unfocusedBorderColor = Color(0xFF3A3D42),
                focusedLabelColor = SubtleText, unfocusedLabelColor = SubtleText,
                cursorColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            singleLine = true,
            label = { Text("User ID", fontSize = 12.sp) },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = SubtleText, unfocusedBorderColor = Color(0xFF3A3D42),
                focusedLabelColor = SubtleText, unfocusedLabelColor = SubtleText,
                cursorColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (dirty) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .background(accent, RoundedCornerShape(8.dp))
                        .clickable { onSave(key.trim(), user.trim()) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Save", color = Color(0xFF111111), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun SiteRow(label: String, sub: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = if (selected) accent else Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(sub, color = SubtleText, fontSize = 13.sp)
        }
        androidx.compose.material3.RadioButton(
            selected = selected,
            onClick = onClick,
            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                selectedColor = accent,
                unselectedColor = SubtleText
            )
        )
    }
}
