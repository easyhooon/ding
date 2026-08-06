package io.github.easyhooon.ding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun DingApp(
    store: DingStore,
    onCopy: (String) -> Unit,
    onShare: (String, String) -> Unit,
    onCleared: () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) {
            darkColorScheme(primary = Color(0xFF83D5DE))
        } else {
            lightColorScheme(primary = Color(0xFF006D77))
        },
    ) {
        DingRoute(
            store = store,
            onCopy = onCopy,
            onShare = onShare,
            onCleared = onCleared,
        )
    }
}

@Composable
private fun DingRoute(
    store: DingStore,
    onCopy: (String) -> Unit,
    onShare: (String, String) -> Unit,
    onCleared: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var snapshots by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf(NotificationFilterTag.ALL) }
    var query by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<NotificationSnapshotUiModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val items = remember(snapshots) {
        snapshots.asReversed().map(NotificationSnapshotUiModel::from)
    }
    val filteredItems = remember(items, selectedFilter, query) {
        items.filter { item ->
            (selectedFilter == NotificationFilterTag.ALL || item.tag == selectedFilter) && item.matches(query)
        }
    }

    fun reload() {
        scope.launch {
            isLoading = true
            snapshots = store.readAll()
            isLoading = false
        }
    }

    LaunchedEffect(store) {
        snapshots = store.readAll()
        isLoading = false
    }

    val detailItem = selectedItem
    if (detailItem != null) {
        NotificationDetailScreen(
            item = detailItem,
            onBack = { selectedItem = null },
            onCopy = onCopy,
            onShareMessage = {
                onShare("Ding message", detailItem.rawJson)
            },
            onShareFcmToken = { fcmToken ->
                onShare("FCM registration token at capture", fcmToken)
            },
        )
        return
    }

    NotificationListScreen(
        items = filteredItems,
        totalCount = items.size,
        selectedFilter = selectedFilter,
        query = query,
        isLoading = isLoading,
        onFilterSelected = { selectedFilter = it },
        onQueryChanged = { query = it },
        onItemSelected = { selectedItem = it },
        onCopyFiltered = { onCopy(filteredItems.toJsonExport()) },
        onShareFiltered = {
            onShare("Ding export", filteredItems.toTextExport())
        },
        onReload = ::reload,
        onClear = {
            scope.launch {
                store.clear()
                snapshots = emptyList()
                onCleared()
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationListScreen(
    items: List<NotificationSnapshotUiModel>,
    totalCount: Int,
    selectedFilter: NotificationFilterTag,
    query: String,
    isLoading: Boolean,
    onFilterSelected: (NotificationFilterTag) -> Unit,
    onQueryChanged: (String) -> Unit,
    onItemSelected: (NotificationSnapshotUiModel) -> Unit,
    onCopyFiltered: () -> Unit,
    onShareFiltered: () -> Unit,
    onReload: () -> Unit,
    onClear: () -> Unit,
) {
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Ding", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (isLoading) "Loading…" else "${items.size} of $totalCount messages",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    CopyShareMenu(
                        copyLabel = "Copy filtered JSON",
                        shareLabel = "Share filtered text",
                        contentDescription = "Filtered message actions",
                        enabled = items.isNotEmpty(),
                        onCopy = onCopyFiltered,
                        onShare = onShareFiltered,
                    )
                    TextButton(onClick = onReload) { Text("Reload") }
                    TextButton(
                        onClick = { showClearConfirmation = true },
                        enabled = totalCount > 0,
                    ) {
                        Text("Clear")
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text("Search payloads") },
                placeholder = { Text("Title, source, tag, data key, raw JSON…") },
                singleLine = true,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NotificationFilterTag.entries.forEach { tag ->
                    FilterChip(
                        selected = selectedFilter == tag,
                        onClick = { onFilterSelected(tag) },
                        label = { Text(tag.label) },
                    )
                }
            }

            if (!isLoading && items.isEmpty()) {
                EmptyState(
                    hasFilters = query.isNotBlank() || selectedFilter != NotificationFilterTag.ALL,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(items) { item ->
                        NotificationMessageCard(item = item, onClick = { onItemSelected(item) })
                    }
                }
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear all notifications?") },
            text = {
                Text("All captured notification history will be permanently deleted. This can't be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClear()
                    },
                ) {
                    Text("Clear all", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun EmptyState(hasFilters: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = if (hasFilters) "No messages match the current filters." else "No notification captured yet.",
            modifier = Modifier.padding(24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NotificationMessageCard(
    item: NotificationSnapshotUiModel,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(item.tag, item.categoryLabel)
                Spacer(Modifier.width(8.dp))
                if (item.source.equals(item.categoryLabel, ignoreCase = true)) {
                    Spacer(Modifier.weight(1f))
                } else {
                    Text(
                        text = item.source,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = item.receivedAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.body?.let { body ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CategoryBadge(
    tag: NotificationFilterTag,
    label: String,
) {
    val color = when (tag) {
        NotificationFilterTag.FCM -> Color(0xFF238636)
        NotificationFilterTag.LOCAL -> MaterialTheme.colorScheme.primary
        NotificationFilterTag.ALL -> MaterialTheme.colorScheme.tertiary
    }

    Surface(
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(modifier = Modifier.size(7.dp), color = color, shape = CircleShape) {}
            Spacer(Modifier.width(5.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationDetailScreen(
    item: NotificationSnapshotUiModel,
    onBack: () -> Unit,
    onCopy: (String) -> Unit,
    onShareMessage: () -> Unit,
    onShareFcmToken: (String) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(NotificationDetailTab.OVERVIEW) }
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    CopyShareMenu(
                        copyLabel = "Copy raw JSON",
                        shareLabel = "Share message",
                        contentDescription = "Message actions",
                        onCopy = { onCopy(item.rawJson) },
                        onShare = onShareMessage,
                    )
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                NotificationDetailTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.label, maxLines = 1) },
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    NotificationDetailTab.OVERVIEW -> OverviewTab(
                        item = item,
                        onCopy = onCopy,
                        onShareFcmToken = onShareFcmToken,
                    )
                    NotificationDetailTab.RAW -> JsonTab(item.rawJson)
                }
            }
        }
    }
}

@Composable
private fun CopyShareMenu(
    copyLabel: String,
    shareLabel: String,
    contentDescription: String,
    enabled: Boolean = true,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            enabled = enabled,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_share),
                contentDescription = contentDescription,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(copyLabel) },
                onClick = {
                    expanded = false
                    onCopy()
                },
            )
            DropdownMenuItem(
                text = { Text(shareLabel) },
                onClick = {
                    expanded = false
                    onShare()
                },
            )
        }
    }
}

@Composable
private fun OverviewTab(
    item: NotificationSnapshotUiModel,
    onCopy: (String) -> Unit,
    onShareFcmToken: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        CategoryBadge(item.tag, item.categoryLabel)
        if (item.tag == NotificationFilterTag.FCM) {
            Spacer(Modifier.height(16.dp))
            FcmTokenCard(
                token = item.fcmToken,
                onCopy = onCopy,
                onShare = onShareFcmToken,
            )
        }
        Spacer(Modifier.height(16.dp))
        item.overview.forEachIndexed { index, (label, value) ->
            Column(modifier = Modifier.padding(vertical = 10.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(3.dp))
                Text(text = value, style = MaterialTheme.typography.bodyLarge)
            }
            if (index < item.overview.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun FcmTokenCard(
    token: String?,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "FCM token at capture",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    enabled = token != null,
                    onClick = { token?.let(onCopy) },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_content_copy),
                        contentDescription = "Copy FCM token at capture",
                    )
                }
                IconButton(
                    enabled = token != null,
                    onClick = { token?.let(onShare) },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = "Share FCM token at capture",
                    )
                }
            }

            SelectionContainer {
                Text(
                    text = token ?: "Not captured. Pass the host app's registration token when capturing this message.",
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = if (token == null) FontFamily.Default else FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (token == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

@Composable
private fun JsonTab(text: String) {
    SelectionContainer {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private enum class NotificationDetailTab(
    val label: String,
) {
    OVERVIEW("Overview"),
    RAW("Raw"),
}

private fun List<NotificationSnapshotUiModel>.toJsonExport(): String {
    return joinToString(
        separator = ",\n",
        prefix = "[\n",
        postfix = "\n]",
    ) { item -> item.rawJson.prependIndent("  ") }
}

private fun List<NotificationSnapshotUiModel>.toTextExport(): String {
    return joinToString(separator = "\n\n---\n\n") { item ->
        "${item.receivedAt} · ${item.source} · ${item.categoryLabel}\n${item.rawJson}"
    }
}
