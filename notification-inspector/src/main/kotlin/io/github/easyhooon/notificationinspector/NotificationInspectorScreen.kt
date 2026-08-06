package io.github.easyhooon.notificationinspector

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
internal fun NotificationInspectorApp(
    store: NotificationInspectorStore,
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
        NotificationInspectorRoute(
            store = store,
            onCopy = onCopy,
            onShare = onShare,
            onCleared = onCleared,
        )
    }
}

@Composable
private fun NotificationInspectorRoute(
    store: NotificationInspectorStore,
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
                onShare("Notification Inspector message", detailItem.rawJson)
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
            onShare("Notification Inspector export", filteredItems.toTextExport())
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Notification Inspector", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (isLoading) "Loading…" else "${items.size} of $totalCount messages",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onReload) { Text("Reload") }
                    TextButton(onClick = onClear, enabled = totalCount > 0) { Text("Clear") }
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCopyFiltered, enabled = items.isNotEmpty()) {
                    Text("Copy filtered JSON")
                }
                TextButton(onClick = onShareFiltered, enabled = items.isNotEmpty()) {
                    Text("Share filtered text")
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
                Text(
                    text = item.source,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
                    TextButton(onClick = onBack) { Text("Back") }
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

            DetailActions(item = item, onCopy = onCopy, onShare = onShareMessage)

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
private fun DetailActions(
    item: NotificationSnapshotUiModel,
    onCopy: (String) -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
    ) {
        TextButton(onClick = { onCopy(item.rawJson) }) { Text("Copy raw JSON") }
        TextButton(onClick = { onCopy(item.dataJson) }) { Text("Copy data") }
        TextButton(onClick = { onCopy(item.notificationJson) }) { Text("Copy notification") }
        TextButton(onClick = onShare) { Text("Share message") }
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
