package com.example.mediabrowser.ui.settings

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mediabrowser.domain.model.DarkModeOption
import com.example.mediabrowser.domain.model.LayoutStyle
import com.example.mediabrowser.ui.components.appBackgroundGradient
import com.example.mediabrowser.ui.theme.parseHexColor
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsState()
    val accentColor = parseHexColor(settings.accentColorHex, Color(0xFF2DD4BF))
    
    // Parse user custom surface color or fallback to a crisp semi-transparent dark shade
    val surfaceColor = parseHexColor(settings.surfaceColorHex, Color(0x22FFFFFF))

    var showDarkModeDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var activeColorPicker by remember { mutableStateOf<ColorTarget?>(null) }

    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.appBackgroundGradient(accentColor)
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
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )

            SectionHeader("Appearance")
            SettingsRow(
                title = "Dark mode",
                value = settings.darkMode.name.lowercase().replaceFirstChar { it.uppercase() },
                surfaceColor = surfaceColor,
                onClick = { showDarkModeDialog = true }
            )
            SettingsSliderRow(
                title = "Grid columns",
                value = settings.gridColumns,
                range = 1f..4f,
                onValueChange = { viewModel.setGridColumns(it) }
            )
            SettingsSliderRow(
                title = "Card corner radius",
                value = settings.cardCornerRadiusDp,
                range = 0f..32f,
                onValueChange = { viewModel.setCardCornerRadius(it) }
            )
            LayoutStyleRow(
                title = "Home layout",
                current = settings.homeLayoutStyle,
                onSelect = viewModel::setHomeLayoutStyle
            )
            LayoutStyleRow(
                title = "Favorites layout",
                current = settings.favoritesLayoutStyle,
                onSelect = viewModel::setFavoritesLayoutStyle
            )

            Divider(color = Color(0xFF2A2D2F))
            SectionHeader("Theme Colors")
            ColorRow("Accent color", settings.accentColorHex, surfaceColor) { activeColorPicker = ColorTarget.ACCENT }
            ColorRow("Background color", settings.backgroundColorHex, surfaceColor) { activeColorPicker = ColorTarget.BACKGROUND }
            ColorRow("Surface color", settings.surfaceColorHex, surfaceColor) { activeColorPicker = ColorTarget.SURFACE }
            ColorRow("Artist tag color", settings.tagArtistColorHex, surfaceColor) { activeColorPicker = ColorTarget.TAG_ARTIST }
            ColorRow("Character tag color", settings.tagCharacterColorHex, surfaceColor) { activeColorPicker = ColorTarget.TAG_CHARACTER }
            ColorRow("Copyright tag color", settings.tagCopyrightColorHex, surfaceColor) { activeColorPicker = ColorTarget.TAG_COPYRIGHT }
            ColorRow("Meta tag color", settings.tagMetaColorHex, surfaceColor) { activeColorPicker = ColorTarget.TAG_META }
            ColorRow("General tag color", settings.tagGeneralColorHex, surfaceColor) { activeColorPicker = ColorTarget.TAG_GENERAL }

            Divider(color = Color(0xFF2A2D2F))
            SectionHeader("Content")
            SwitchRow("Safe search", settings.safeSearchEnabled, surfaceColor, viewModel::setSafeSearch)
            SwitchRow("Data saver", settings.dataSaverEnabled, surfaceColor, viewModel::setDataSaver)
            SwitchRow("Autoplay videos", settings.autoPlayVideos, surfaceColor, viewModel::setAutoPlayVideos)

            Divider(color = Color(0xFF2A2D2F))
            SectionHeader("Downloads")
            SwitchRow("Download over Wi-Fi only", settings.downloadOverWifiOnly, surfaceColor, viewModel::setWifiOnlyDownloads)
            if (notificationPermissionState != null && !notificationPermissionState.status.isGranted) {
                ListItem(
                    headlineContent = { Text("Enable download notifications", color = Color.White) },
                    supportingContent = {
                        Text("Allow notifications to see download progress.", color = Color(0xFF8A8D91))
                    },
                    colors = ListItemDefaults.colors(containerColor = surfaceColor),
                    modifier = Modifier.clickable { notificationPermissionState.launchPermissionRequest() }
                )
            }

            Divider(color = Color(0xFF2A2D2F))
            SectionHeader("Advanced API Configuration")
            Text(
                text = "Optional: point the app at a different provider. Leave blank to use the built-in default.",
                color = Color(0xFF8A8D91),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            ApiConfigField("Provider name", settings.apiProviderName, viewModel::setApiProviderName)
            ApiConfigField("Base URL", settings.apiBaseUrl, viewModel::setApiBaseUrl)
            ApiConfigField("API Key / Credential 1", settings.apiCredentialOne, viewModel::setApiCredentialOne, isSecret = true)
            ApiConfigField("Client Secret / Credential 2 (optional)", settings.apiCredentialTwo, viewModel::setApiCredentialTwo, isSecret = true)

            Divider(color = Color(0xFF2A2D2F))
            SectionHeader("Storage")
            SettingsRow(
                title = "Clear image cache",
                value = formatBytes(cacheSizeBytes),
                surfaceColor = surfaceColor,
                onClick = { showClearCacheDialog = true }
            )

            Divider(color = Color(0xFF2A2D2F))
            SectionHeader("About")
            ListItem(
                headlineContent = { Text("Media Browser", color = Color.White) },
                supportingContent = { Text("v1.0", color = Color(0xFF8A8D91)) },
                colors = ListItemDefaults.colors(containerColor = surfaceColor)
            )
        }
    }

    if (showDarkModeDialog) {
        DarkModeDialog(
            current = settings.darkMode,
            onSelect = { viewModel.setDarkMode(it); showDarkModeDialog = false },
            onDismiss = { showDarkModeDialog = false }
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear image cache?") },
            text = { Text("This removes cached thumbnails and previews. Downloaded files are not affected.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearCache(); showClearCacheDialog = false }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") }
            }
        )
    }

    activeColorPicker?.let { target ->
        val (title, currentHex, onApply) = colorTargetConfig(target, settings, viewModel)
        ColorPickerDialog(
            title = title,
            currentHex = currentHex,
            onColorSelected = onApply,
            onDismiss = { activeColorPicker = null }
        )
    }
}

private enum class ColorTarget {
    ACCENT, BACKGROUND, SURFACE, TAG_ARTIST, TAG_CHARACTER, TAG_COPYRIGHT, TAG_META, TAG_GENERAL
}

private fun colorTargetConfig(
    target: ColorTarget,
    settings: com.example.mediabrowser.domain.model.AppSettings,
    viewModel: SettingsViewModel
): Triple<String, String, (String) -> Unit> = when (target) {
    ColorTarget.ACCENT -> Triple("Accent color", settings.accentColorHex, viewModel::setAccentColor)
    ColorTarget.BACKGROUND -> Triple("Background color", settings.backgroundColorHex, viewModel::setBackgroundColor)
    ColorTarget.SURFACE -> Triple("Surface color", settings.surfaceColorHex, viewModel::setSurfaceColor)
    ColorTarget.TAG_ARTIST -> Triple("Artist tag color", settings.tagArtistColorHex, viewModel::setTagArtistColor)
    ColorTarget.TAG_CHARACTER -> Triple("Character tag color", settings.tagCharacterColorHex, viewModel::setTagCharacterColor)
    ColorTarget.TAG_COPYRIGHT -> Triple("Copyright tag color", settings.tagCopyrightColorHex, viewModel::setTagCopyrightColor)
    ColorTarget.TAG_META -> Triple("Meta tag color", settings.tagMetaColorHex, viewModel::setTagMetaColor)
    ColorTarget.TAG_GENERAL -> Triple("General tag color", settings.tagGeneralColorHex, viewModel::setTagGeneralColor)
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF2DD4BF), // Fixed accent teal fallback color for clean structural hierarchy
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsRow(title: String, value: String, surfaceColor: Color, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, color = Color.White) },
        supportingContent = { Text(value, color = Color(0xFF8A8D91)) },
        colors = ListItemDefaults.colors(containerColor = surfaceColor),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, surfaceColor: Color, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title, color = Color.White) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        colors = ListItemDefaults.colors(containerColor = surfaceColor)
    )
}

@Composable
private fun SettingsSliderRow(title: String, value: Int, range: ClosedFloatingPointRange<Float>, onValueChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text("$title: $value", color = Color.White, fontSize = 14.sp)
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range
        )
    }
}

@Composable
private fun ColorRow(title: String, hex: String, surfaceColor: Color, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, color = Color.White) },
        trailingContent = {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(parseHexColor(hex, Color.Gray), CircleShape)
            )
        },
        colors = ListItemDefaults.colors(containerColor = surfaceColor),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun LayoutStyleRow(title: String, current: LayoutStyle, onSelect: (LayoutStyle) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(title, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
        Row {
            LayoutStyle.entries.forEach { style ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .clickable { onSelect(style) }
                ) {
                    RadioButton(selected = current == style, onClick = { onSelect(style) })
                    Text(style.name.lowercase().replaceFirstChar { it.uppercase() }, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ApiConfigField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isSecret: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isSecret) {
            androidx.compose.ui.text.input.PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun DarkModeDialog(current: DarkModeOption, onSelect: (DarkModeOption) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dark mode") },
        text = {
            Column {
                DarkModeOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option == current, onClick = { onSelect(option) })
                        Text(option.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) "%.1f MB used".format(mb) else "0 MB used"
}