package com.lambao.odv.tools.debug

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Severity
import com.lambao.odv.core.designsystem.component.ODVAppBar
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonSize
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVChip
import com.lambao.odv.core.designsystem.component.ODVEmptyState
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVRadioRow
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.component.ODVSearchBar
import com.lambao.odv.core.designsystem.component.ODVSecureMode
import com.lambao.odv.core.designsystem.component.ODVSwitchRow
import com.lambao.odv.core.designsystem.component.ODVTab
import com.lambao.odv.core.designsystem.component.ODVTabs
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

private fun formatTime(ms: Long): String = timeFormat.format(Date(ms))

private const val TAB_API = 0
private const val TAB_LOG = 1
private const val TAB_STORAGE = 2

/**
 * Màn Debug: Tabs rồi nội dung từng tab; bấm một request ở tab API mở màn chi tiết. Chuỗi để trực tiếp vì chỉ dùng khi
 * phát triển (ADR-0012). Tìm kiếm có ở tab API và Log; Lưu trữ và Khác để sau.
 */
@Composable
internal fun ODVDebugScreen(onBack: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(TAB_API) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var storageRefresh by remember { mutableIntStateOf(0) }
    val maskTraffic by DebugSettings.maskTraffic.collectAsState()
    val requests by ApiTrafficStore.requests.collectAsState()

    val selected = selectedId?.let { id -> requests.firstOrNull { it.id == id } }
    if (selected != null) {
        BackHandler { selectedId = null }
        ODVApiDetailScreen(request = selected, maskTraffic = maskTraffic, onBack = { selectedId = null })
        return
    }

    val searchable = tab == TAB_API || tab == TAB_LOG
    BackHandler(enabled = searching) {
        searching = false
        query = ""
    }

    ODVScaffold(
        topBar = {
            if (searching && searchable) {
                ODVSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = if (tab == TAB_API) "Tìm trong mọi request" else "Tìm trong log",
                    backContentDescription = "Đóng tìm kiếm",
                    onBack = {
                        searching = false
                        query = ""
                    },
                    clearContentDescription = "Xóa từ khóa",
                )
            } else {
                ODVAppBar(
                    title = "Debug",
                    navigation = { ODVIconButton(ODVIcon.ArrowLeft, "Quay lại", onBack) },
                    // X ở góc phải đóng màn Debug (finish Activity). Xóa/làm mới nằm ở thanh công cụ của từng tab.
                    actions = {
                        if (searchable) ODVIconButton(ODVIcon.Search, "Tìm kiếm", { searching = true })
                        ODVIconButton(ODVIcon.Close, "Đóng", onBack)
                    },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize()) {
            ODVTabs(
                tabs = listOf(
                    ODVTab("API", ODVIcon.Sync),
                    ODVTab("Log", ODVIcon.List),
                    ODVTab("Lưu trữ", ODVIcon.Folder),
                    ODVTab("Khác", ODVIcon.Tune),
                ),
                selectedIndex = tab,
                // Đổi tab thì đóng tìm kiếm: từ khóa của tab này không có nghĩa ở tab kia. Làm ở đây thay vì LaunchedEffect(tab) để
                // quay lại từ màn chi tiết không xóa mất từ khóa.
                onSelect = {
                    tab = it
                    searching = false
                    query = ""
                },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            )
            val activeQuery = if (searching) query else ""
            when (tab) {
                TAB_API -> ToolRow("Xóa log API") { ApiTrafficStore.clear() }
                TAB_LOG -> ToolRow("Xóa log local") { DebugLogStore.clear() }
                TAB_STORAGE -> ToolRow("Làm mới") { storageRefresh++ }
            }
            when (tab) {
                TAB_API -> ApiTab(padding, requests, activeQuery, maskTraffic) { selectedId = it }
                TAB_LOG -> LogTab(padding, activeQuery)
                TAB_STORAGE -> StorageTab(padding, storageRefresh)
                else -> OthersTab(padding)
            }
        }
    }
}

@Composable
private fun ToolRow(label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        ODVButton(label, onClick, style = ODVButtonStyle.Secondary, size = ODVButtonSize.Sm)
    }
}

// ---------- Tab API ----------

private class ApiSearchResult(val items: List<Pair<DebugRequest, Int>>, val totalMatches: Int)

@Composable
private fun ApiTab(
    padding: PaddingValues,
    requests: List<DebugRequest>,
    query: String,
    maskTraffic: Boolean,
    onOpen: (Long) -> Unit,
) {
    if (requests.isEmpty()) {
        EmptyTab("Chưa có request nào", "Dùng app để gọi Graph, request sẽ hiện ở đây.")
        return
    }
    // Tìm trong body lớn có thể nặng: chạy ngoài luồng chính để gõ phím không bị giật.
    val result by produceState(ApiSearchResult(requests.asReversed().map { it to 0 }, 0), requests, query, maskTraffic) {
        value = withContext(Dispatchers.Default) {
            val ordered = requests.asReversed()
            if (query.isEmpty()) {
                ApiSearchResult(ordered.map { it to 0 }, 0)
            } else {
                // Cùng cách đếm với màn chi tiết (SectionCache) nên số ở danh sách và trong chi tiết luôn khớp nhau.
                SectionCache.retain(requests.mapTo(HashSet()) { it.id })
                val scored = ordered.map { request ->
                    ensureActive()
                    request to requestMatchCount(request, maskTraffic, query)
                }.filter { it.second > 0 }
                ApiSearchResult(scored, scored.sumOf { it.second })
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        if (query.isNotEmpty()) {
            SearchSummary("${result.items.size}/${requests.size} request · ${result.totalMatches} kết quả", result.totalMatches > 0)
        }
        if (result.items.isEmpty()) {
            EmptyTab("Không có kết quả", "Không request nào chứa “$query”.")
        } else {
            LazyColumn(contentPadding = padding) {
                items(result.items, key = { it.first.id }) { (request, count) ->
                    RequestRow(request, count, query) { onOpen(request.id) }
                }
            }
        }
    }
}

@Composable
private fun SearchSummary(text: String, found: Boolean) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        style = ODVTheme.typography.captionStrong,
        color = if (found) ODVTheme.colors.voltText else ODVTheme.colors.danger,
    )
}

@Composable
private fun RequestRow(request: DebugRequest, matches: Int, query: String, onClick: () -> Unit) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    val entry = request.entry
    val status = entry.status
    val statusColor = when {
        status == null -> colors.danger
        status < 300 -> colors.success
        status < 500 -> colors.warning
        else -> colors.danger
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(entry.method, style = type.timecode, color = colors.ink)
            Text(status?.toString() ?: (entry.error ?: "-"), style = type.timecode, color = statusColor)
            Text("${entry.durationMs} ms", style = type.timecode, color = colors.inkMuted)
            Text(formatTime(request.timeMs), style = type.timecode, color = colors.inkFaint)
            if (query.isNotEmpty()) Text("$matches khớp", style = type.timecode, color = colors.voltText)
        }
        Text(entry.url, style = type.code, color = colors.inkMuted, maxLines = 2)
    }
}

// ---------- Tab Log ----------

@Composable
private fun LogTab(padding: PaddingValues, query: String) {
    val lines by DebugLogStore.lines.collectAsState()
    var minSeverity by rememberSaveable { mutableStateOf(Severity.Verbose.name) }
    val min = Severity.valueOf(minSeverity)
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(Severity.Verbose to "V+", Severity.Debug to "D+", Severity.Info to "I+", Severity.Warn to "W+", Severity.Error to "E").forEach { (severity, label) ->
                ODVChip(label, selected = min == severity, onClick = { minSeverity = severity.name })
            }
        }
        val bySeverity = lines.filter { it.severity >= min }
        val scored = if (query.isEmpty()) {
            bySeverity.map { it to 0 }
        } else {
            bySeverity.map { it to countTextMatches("${it.tag} ${it.message} ${it.throwable.orEmpty()}", query) }.filter { it.second > 0 }
        }
        if (query.isNotEmpty()) {
            SearchSummary("${scored.size}/${bySeverity.size} dòng · ${scored.sumOf { it.second }} kết quả", scored.isNotEmpty())
        }
        if (scored.isEmpty()) {
            EmptyTab(if (query.isEmpty()) "Chưa có log" else "Không có kết quả", "Log của app (Kermit) từ mức đã chọn sẽ hiện ở đây.")
        } else {
            LazyColumn(contentPadding = padding) {
                items(scored.asReversed(), key = { it.first.id }) { (line, _) -> LogRow(line) }
            }
        }
    }
}

@Composable
private fun LogRow(line: DebugLogLine) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    val color: Color = when (line.severity) {
        Severity.Error, Severity.Assert -> colors.danger
        Severity.Warn -> colors.warning
        Severity.Info -> colors.ink
        else -> colors.inkMuted
    }
    SelectionContainer {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text(
                "${formatTime(line.timeMs)} ${line.severity.name.first()}/${line.tag}: ${line.message}",
                style = type.code,
                color = color,
            )
            line.throwable?.let { Text(it, style = type.code, color = colors.inkMuted) }
        }
    }
}

// ---------- Tab Lưu trữ ----------

@Composable
private fun StorageTab(padding: PaddingValues, refresh: Int) {
    val context = LocalContext.current
    val snapshot by produceState<StorageSnapshot?>(null, refresh) {
        value = withContext(Dispatchers.IO) { StorageReader.read(context) }
    }
    val data = snapshot
    if (data == null) {
        EmptyTab("Đang đọc…", null)
        return
    }
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { SectionTitle("SharedPreferences") }
        if (data.preferences.isEmpty()) item { Note("Không có tệp SharedPreferences.") }
        items(data.preferences, key = { "prefs:${it.name}" }) { prefs ->
            Collapsible(title = "${prefs.name}  (${prefs.entries.size})") {
                SelectionContainer {
                    Text(
                        prefs.entries.joinToString("\n") { "${it.first} = ${it.second}" }.ifEmpty { "(trống)" },
                        style = type.code,
                        color = colors.ink,
                    )
                }
            }
        }
        item { SectionTitle("DataStore") }
        item { Note(if (data.dataStoreFiles.isEmpty()) "Chưa có tệp DataStore." else data.dataStoreFiles.joinToString("\n")) }
        item { SectionTitle("Cơ sở dữ liệu (Room / SQLite, chỉ đọc, 50 dòng đầu)") }
        if (data.databases.isEmpty()) item { Note("Chưa có cơ sở dữ liệu (Room vào ở Lát 3).") }
        items(data.databases, key = { "db:${it.name}" }) { db ->
            Collapsible(title = "${db.name}  (${db.sizeBytes / 1024} KB)") {
                if (db.error != null) {
                    Note("Không mở được: ${db.error}")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        db.tables.forEach { table ->
                            Collapsible(title = "${table.name}  (${table.rowCount} dòng)") {
                                SelectionContainer {
                                    Box(Modifier.horizontalScroll(rememberScrollState())) {
                                        Text(
                                            (listOf(table.columns.joinToString(" | ")) + table.rows.map { it.joinToString(" | ") })
                                                .joinToString("\n"),
                                            style = type.code,
                                            color = colors.ink,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { SectionTitle("Kho bí mật (chỉ tên tệp, không giải mã)") }
        item { Note(if (data.secretFiles.isEmpty()) "Chưa có." else data.secretFiles.joinToString("\n")) }
    }
}

// ---------- Tab Khác ----------

@Composable
private fun OthersTab(padding: PaddingValues) {
    val context = LocalContext.current
    val secureMode by DebugSettings.secureMode.collectAsState()
    val maskTraffic by DebugSettings.maskTraffic.collectAsState()
    val actions = DebugActions.items
    LazyColumn(contentPadding = padding) {
        item { SectionTitle("FLAG_SECURE toàn app") }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                ODVRadioRow(
                    label = "Theo thiết kế",
                    selected = secureMode == ODVSecureMode.ByDesign,
                    onClick = { DebugSettings.setSecureMode(ODVSecureMode.ByDesign) },
                    description = "Chỉ màn Kết nối, Khóa, nhập PIN, Cài đặt chặn chụp màn hình",
                )
                ODVRadioRow(
                    label = "Luôn bật",
                    selected = secureMode == ODVSecureMode.AlwaysOn,
                    onClick = { DebugSettings.setSecureMode(ODVSecureMode.AlwaysOn) },
                    description = "Mọi màn của app chặn chụp màn hình",
                )
                ODVRadioRow(
                    label = "Luôn tắt",
                    selected = secureMode == ODVSecureMode.AlwaysOff,
                    onClick = { DebugSettings.setSecureMode(ODVSecureMode.AlwaysOff) },
                    description = "Không màn nào chặn, kể cả màn nhạy cảm (để chụp/quay màn hình)",
                )
            }
        }
        item { SectionTitle("Log API") }
        item {
            ODVSwitchRow(
                title = "Che dữ liệu nhạy cảm",
                checked = maskTraffic,
                onCheckedChange = { DebugSettings.setMaskTraffic(it) },
                modifier = Modifier.padding(horizontal = 16.dp),
                description = "Che Authorization, token, client_secret, downloadUrl khi hiển thị. Mặc định tắt: hiện đầy đủ.",
            )
        }
        item { SectionTitle("Công cụ") }
        if (actions.isEmpty()) item { Note("Chưa có công cụ nào đăng ký qua DebugActions.register.") }
        items(actions, key = { it.title }) { action ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { action.onClick(context) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(action.title, style = ODVTheme.typography.bodyStrong, color = ODVTheme.colors.ink)
                Text(action.description, style = ODVTheme.typography.caption, color = ODVTheme.colors.inkMuted)
            }
        }
    }
}

// ---------- Thành phần chung ----------

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        style = ODVTheme.typography.captionStrong,
        color = ODVTheme.colors.voltText,
    )
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        style = ODVTheme.typography.code,
        color = ODVTheme.colors.inkMuted,
    )
}

@Composable
private fun Collapsible(title: String, content: @Composable () -> Unit) {
    var open by rememberSaveable(title) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            (if (open) "- " else "+ ") + title,
            modifier = Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 10.dp),
            style = ODVTheme.typography.bodySmStrong,
            color = ODVTheme.colors.ink,
        )
        if (open) Box(Modifier.padding(bottom = 8.dp)) { content() }
    }
}

@Composable
private fun EmptyTab(title: String, body: String?) {
    Box(Modifier.fillMaxSize()) {
        ODVEmptyState(
            icon = ODVIcon.Info,
            title = title,
            modifier = Modifier.padding(top = 48.dp),
            body = body,
        )
    }
}
