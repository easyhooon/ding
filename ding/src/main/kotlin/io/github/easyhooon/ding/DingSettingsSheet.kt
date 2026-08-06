package io.github.easyhooon.ding

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val GITHUB_URL = "https://github.com/easyhooon/ding"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DingSettingsSheet(
    darkMode: Boolean?,
    canClear: Boolean,
    onDarkModeChange: (Boolean?) -> Unit,
    onReload: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(12.dp))

            SectionHeader("General")
            DarkModeRow(darkMode = darkMode, onDarkModeChange = onDarkModeChange)

            SectionDivider()

            SectionHeader("Data")
            ActionRow(
                icon = R.drawable.ic_autorenew,
                title = "Reload messages",
                description = "Read captured notifications from storage",
                accent = MaterialTheme.colorScheme.primary,
                onClick = onReload,
            )
            ActionRow(
                icon = R.drawable.ic_delete_outline,
                title = "Clear all notifications",
                description = "Permanently remove every captured notification",
                accent = MaterialTheme.colorScheme.error,
                enabled = canClear,
                onClick = onClear,
            )

            SectionDivider()

            SectionHeader("About")
            InfoRow(title = "Version", value = BuildConfig.DING_VERSION)
            ClickableInfoRow(
                title = "GitHub",
                value = "easyhooon/ding",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))
                    runCatching { context.startActivity(intent) }
                        .onFailure { if (it !is ActivityNotFoundException) throw it }
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

private enum class DarkModeOption(val label: String, val value: Boolean?) {
    SYSTEM(label = "System", value = null),
    LIGHT(label = "Light", value = false),
    DARK(label = "Dark", value = true),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DarkModeRow(
    darkMode: Boolean?,
    onDarkModeChange: (Boolean?) -> Unit,
) {
    val selected = DarkModeOption.entries.first { it.value == darkMode }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIcon(
            icon = R.drawable.ic_brightness_6,
            accent = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.size(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DarkModeOption.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == selected,
                        onClick = { onDarkModeChange(option.value) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = DarkModeOption.entries.size,
                        ),
                    ) {
                        Text(option.label)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: Int,
    title: String,
    description: String,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val contentColor = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIcon(icon = icon, accent = contentColor)
        Spacer(Modifier.size(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = contentColor,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsIcon(icon: Int, accent: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ClickableInfoRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                painter = painterResource(R.drawable.ic_open_in_new),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
