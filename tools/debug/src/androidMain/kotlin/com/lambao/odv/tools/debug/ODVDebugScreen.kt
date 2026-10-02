package com.lambao.odv.tools.debug

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Severity
import com.lambao.odv.core.designsystem.component.ODVAppBar
import com.lambao.odv.core.designsystem.component.ODVChip
import com.lambao.odv.core.designsystem.component.ODVEmptyState
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.component.ODVTab
import com.lambao.odv.core.designsystem.component.ODVTabs
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

private fun formatTime(ms: Long): String = timeFormat.format(Date(ms))

/** Màn Debug: Tabs rồi nội dung từng tab. Chuỗi để trực tiếp vì chỉ dùng khi phát triển (ADR-0012). */
@Composable
internal fun ODVDebugScreen(onBack: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var storageRefresh by remember { mutableIntStateOf(0) }
    ODVScaffold(
        topBar = {
            ODVAppBar(
                title = "Debug",
                navigation = { ODVIconButton(ODVIcon.ArrowLeft, "Quay lại", onBack) },
                actions = {
                    when (tab) {
                        0 -> ODVIconButton(ODVIcon.Close, "Xóa log API", { ApiTrafficStore.clear() })
                        1 -> ODVIconButton(ODVIcon.Close, "Xóa log local", { DebugLogStore.clear() })
                        else -> ODVIconButton(ODVIcon.Sync, "Làm mới", { storageRefresh++ })
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize()) {
            ODVTabs(
                tabs = listOf(ODVTab("API", ODVIcon.Sync), ODVTab("Log", ODVIcon.List), ODVTab("Lưu trữ", ODVIcon.Folder)),
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            )
            when (tab) {
                0 -> ApiTab(padding)
                1 -> LogTab(padding)
                else -> StorageTab(padding, storageRefresh)
            }
        }
    }
}

// ---------- Tab API ----------

@Composable
private fun ApiTab(padding: PaddingValues) {
    val requests by ApiTrafficStore.requests.collectAsState()
    if (requests.isEmpty()) {
        EmptyTab("Chưa có request nào", "Dùng app để gọi Graph, request sẽ hiện ở đây (đã che Authorization, token và downloadUrl).")
        return
    }
    var expanded by remember { mutableStateOf(emptySet<Long>()) }
    LazyColumn(contentPadding = padding) {
        items(requests.asReversed(), key = { it.id }) { request ->
            val open = request.id in expanded
            RequestRow(request, open) { expanded = if (open) expanded - request.id else expanded + request.id }
        }
    }
}

@Composable
private fun RequestRow(request: DebugRequest, open: Boolean, onToggle: () -> Unit) {
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
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(entry.method, style = type.timecode, color = colors.ink)
            Text(status?.toString() ?: (entry.error ?: "-"), style = type.timecode, color = statusColor)
            Text("${entry.durationMs} ms", style = type.timecode, color = colors.inkMuted)
            Text(formatTime(request.timeMs), style = type.timecode, color = colors.inkFaint)
        }
        Text(entry.url, style = type.code, color = colors.inkMuted, maxLines = if (open) Int.MAX_VALUE else 2)
        if (open) {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (entry.requestHeaders.isNotEmpty()) {
                        Block("Request header", entry.requestHeaders.joinToString("\n") { "${it.first}: ${it.second}" })
                    }
                    if (entry.responseHeaders.isNotEmpty()) {
                        Block("Response header", entry.responseHeaders.joinToString("\n") { "${it.first}: ${it.second}" })
                    }
                    entry.responseBody?.let { Block("Response body", it) }
                        ?: Block("Response body", "(không ghi: endpoint token hoặc lỗi trước khi nhận phản hồi)")
                }
            }
        }
    }
}

@Composable
private fun Block(title: String, text: String) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Column {
        Text(title, style = type.captionStrong, color = colors.voltText)
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            Text(text, style = type.code, color = colors.ink)
        }
    }
}

// ---------- Tab Log ----------

@Composable
private fun LogTab(padding: PaddingValues) {
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
        val visible = lines.filter { it.severity >= min }
        if (visible.isEmpty()) {
            EmptyTab("Chưa có log", "Log của app (Kermit) từ mức đã chọn sẽ hiện ở đây.")
        } else {
            LazyColumn(contentPadding = padding) {
                items(visible.asReversed(), key = { it.id }) { line -> LogRow(line) }
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
