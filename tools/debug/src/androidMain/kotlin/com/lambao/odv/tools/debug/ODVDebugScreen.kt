package com.lambao.odv.tools.debug

import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Severity
import com.lambao.odv.core.designsystem.component.ODVAppBar
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonSize
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVDialog
import com.lambao.odv.core.designsystem.component.ODVDialogTone
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
import com.lambao.odv.core.network.traffic.maskedUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
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
 * phát triển (ADR-0012). Tìm kiếm có ở tab API và Log; bảng DB ở tab Lưu trữ có màn chi tiết riêng (ODVTableDetailScreen).
 */
@Composable
internal fun ODVDebugScreen(onBack: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(TAB_API) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var storageRefresh by remember { mutableIntStateOf(0) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    // Bảng đang xem chi tiết và bảng đang chờ xác nhận xóa, tách thành hai chuỗi vì rememberSaveable không lưu được Pair.
    var openDb by rememberSaveable { mutableStateOf<String?>(null) }
    var openTable by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteDb by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteTable by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val maskTraffic by DebugSettings.maskTraffic.collectAsState()
    val requests by ApiTrafficStore.requests.collectAsState()

    val selected = selectedId?.let { id -> requests.firstOrNull { it.id == id } }
    if (selected != null) {
        BackHandler { selectedId = null }
        ODVApiDetailScreen(request = selected, maskTraffic = maskTraffic, onBack = { selectedId = null })
        return
    }

    val detailDb = openDb
    val detailTable = openTable
    val askDeleteDb = deleteDb
    val askDeleteTable = deleteTable
    if (askDeleteDb != null && askDeleteTable != null) {
        DeleteTableDialog(askDeleteDb, askDeleteTable, onDismiss = { deleteDb = null; deleteTable = null }) {
            deleteDb = null
            deleteTable = null
            storageRefresh++
        }
    }
    if (detailDb != null && detailTable != null) {
        BackHandler { openDb = null; openTable = null }
        ODVTableDetailScreen(
            dbName = detailDb,
            table = detailTable,
            maskTraffic = maskTraffic,
            reload = storageRefresh,
            onDelete = { deleteDb = detailDb; deleteTable = detailTable },
            onBack = { openDb = null; openTable = null },
        )
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
                TAB_STORAGE -> Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // "Làm mới" chỉ đọc lại danh sách (không đổi dữ liệu); "Xóa dữ liệu local" mới là thao tác xóa.
                    ODVButton("Làm mới", { storageRefresh++ }, style = ODVButtonStyle.Secondary, size = ODVButtonSize.Sm)
                    if (DebugHooks.clearLocalData != null) {
                        ODVButton(
                            "Xóa dữ liệu local",
                            { confirmClear = true },
                            style = ODVButtonStyle.Danger,
                            size = ODVButtonSize.Sm,
                        )
                    }
                }
            }
            when (tab) {
                TAB_API -> ApiTab(padding, requests, activeQuery, maskTraffic) { selectedId = it }
                TAB_LOG -> LogTab(padding, activeQuery)
                TAB_STORAGE -> StorageTab(
                    padding,
                    storageRefresh,
                    onOpenTable = { db, table -> openDb = db; openTable = table },
                    onDeleteTable = { db, table -> deleteDb = db; deleteTable = table },
                )
                else -> OthersTab(padding)
            }
        }
    }

    if (confirmClear) {
        // Không hoàn tác được, nên luôn hỏi lại. Chạy trên scope của màn Debug: DisconnectUseCase tự không bị hủy giữa chừng
        // (NonCancellable); hook khởi động lại app ở bước cuối nên màn này đóng sau khi xóa xong.
        ODVDialog(
            onDismissRequest = { confirmClear = false },
            title = "Xóa dữ liệu local?",
            icon = ODVIcon.Alert,
            tone = ODVDialogTone.Danger,
            body = "Như Ngắt kết nối: xóa cấu hình kết nối, mã PIN, sinh trắc học, khóa mã hóa và token trên máy, rồi quay về màn " +
                "Kết nối. Cài đặt debug được giữ. Không hoàn tác được.",
            alert = true,
        ) {
            ODVButton("Hủy", { confirmClear = false }, style = ODVButtonStyle.Ghost)
            ODVButton(
                "Xóa dữ liệu",
                {
                    confirmClear = false
                    scope.launch { DebugHooks.clearLocalData?.invoke() }
                },
                style = ODVButtonStyle.DangerSolid,
            )
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
                    RequestRow(request, count, query, maskTraffic) { onOpen(request.id) }
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
private fun RequestRow(request: DebugRequest, matches: Int, query: String, maskTraffic: Boolean, onClick: () -> Unit) {
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 6.dp, end = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.method, style = type.timecode, color = colors.ink)
                Text(status?.toString() ?: (entry.error ?: "-"), style = type.timecode, color = statusColor)
                Text("${entry.durationMs} ms", style = type.timecode, color = colors.inkMuted)
                Text(formatTime(request.timeMs), style = type.timecode, color = colors.inkFaint)
                if (query.isNotEmpty()) Text("$matches khớp", style = type.timecode, color = colors.voltText)
            }
            // URL có thể mang `sig`/`tempauth`: cũng phải che khi công tắc che đang bật (ADR-0013).
            val url = remember(entry, maskTraffic) { if (maskTraffic) entry.maskedUrl() else entry.url }
            Text(url, style = type.code, color = colors.inkMuted, maxLines = 2)
        }
        // Sao chép nhanh cURL không cần mở chi tiết.
        DebugIconButton(
            R.drawable.ic_debug_curl,
            "Sao chép cURL",
            { scope.launch { copyCurl(context, request, maskTraffic) } },
        )
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
private fun StorageTab(
    padding: PaddingValues,
    refresh: Int,
    onOpenTable: (db: String, table: String) -> Unit,
    onDeleteTable: (db: String, table: String) -> Unit,
) {
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
        if (data.dataStore.isEmpty()) item { Note("Chưa có kho DataStore.") }
        items(data.dataStore, key = { "ds:${it.name}" }) { store ->
            Collapsible(title = "${store.name}  (${store.entries.size})") {
                SelectionContainer {
                    Text(
                        store.entries.joinToString("\n") { "${it.first} = ${it.second}" }.ifEmpty { "(trống)" },
                        style = type.code,
                        color = colors.ink,
                    )
                }
            }
        }
        item { SectionTitle("Cơ sở dữ liệu (chạm một bảng để xem chi tiết)") }
        if (data.databases.isEmpty()) item { Note("Chưa có cơ sở dữ liệu.") }
        items(data.databases, key = { "db:${it.name}" }) { db ->
            Collapsible(title = "${db.name}  (${db.sizeBytes / 1024} KB)") {
                if (db.error != null) {
                    Note("Không mở được: ${db.error}")
                } else {
                    Column {
                        db.tables.forEach { table ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${table.name}  (${table.rowCount} dòng)",
                                    modifier = Modifier.weight(1f).clickable { onOpenTable(db.name, table.name) }.padding(vertical = 10.dp),
                                    style = type.bodySmStrong,
                                    color = colors.ink,
                                )
                                DebugIconButton(
                                    R.drawable.ic_debug_delete,
                                    "Xóa toàn bộ bảng ${table.name}",
                                    { onDeleteTable(db.name, table.name) },
                                    tint = colors.danger,
                                )
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

/**
 * Xác nhận xóa toàn bộ dòng của một bảng (chỉ xóa dữ liệu, giữ cấu trúc). Lời cảnh báo theo loại DB: `odv.db` là bản sao của OneDrive
 * nên xóa được nhưng cần biết `sync_state`; DB của Media3 là chỉ mục cache video, xóa lệch với tệp trên đĩa.
 */
@Composable
private fun DeleteTableDialog(db: String, table: String, onDismiss: () -> Unit, onDeleted: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val consequence = when {
        db == ROOM_DATABASE_NAME && table == "drive_item" ->
            "Danh sách tệp biến mất khỏi app. Bảng sync_state vẫn còn nên app coi như đã đồng bộ xong và không tự tải lại; " +
                "muốn đồng bộ lại hãy xóa cả bảng sync_state, hoặc dùng \"Xóa dữ liệu local\"."
        db == ROOM_DATABASE_NAME && table == "sync_state" ->
            "App sẽ quên mốc đồng bộ và đồng bộ lại từ đầu ở lần mở sau; danh sách tệp (drive_item) được giữ."
        db == ROOM_DATABASE_NAME -> "Dữ liệu của bảng mất ngay trong app."
        else ->
            "Đây là DB nội bộ của thư viện (Media3). Xóa có thể làm chỉ mục cache video lệch với tệp đã lưu trên đĩa, gây lỗi " +
                "phát hoặc cache rác; nếu gặp lỗi hãy dùng \"Xóa dữ liệu local\"."
    }
    ODVDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = "Xóa toàn bộ bảng $table?",
        icon = ODVIcon.Alert,
        tone = ODVDialogTone.Danger,
        body = "Xóa mọi dòng của bảng $table trong $db. Không hoàn tác được. $consequence",
        alert = true,
    ) {
        ODVButton("Hủy", onDismiss, style = ODVButtonStyle.Ghost, enabled = !busy)
        ODVButton(
            "Xóa bảng",
            {
                busy = true
                scope.launch {
                    val message = try {
                        DatabaseBrowser(sqlSourceFor(context, db)).clear(table)
                        "Đã xóa dữ liệu bảng $table"
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        "Không xóa được: ${e::class.simpleName}"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    onDeleted()
                }
            },
            style = ODVButtonStyle.DangerSolid,
            loading = busy,
        )
    }
}

// ---------- Tab Khác ----------

@Composable
private fun OthersTab(padding: PaddingValues) {
    val context = LocalContext.current
    val secureMode by DebugSettings.secureMode.collectAsState()
    val maskTraffic by DebugSettings.maskTraffic.collectAsState()
    val thumbnailScale by DebugSettings.thumbnailScalePercent.collectAsState()
    val scope = rememberCoroutineScope()
    var thumbnailCleared by remember { mutableStateOf(false) }
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
        item { SectionTitle("Thumbnail") }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                // Tỉ lệ so với cỡ mặc định của app (100%). Thấp thì nhẹ và nhanh nhưng mềm; cao thì nét nhưng nặng.
                DebugSettings.thumbnailScales.forEach { percent ->
                    ODVRadioRow(
                        label = if (percent == 100) "100% (mặc định)" else "$percent%",
                        selected = thumbnailScale == percent,
                        onClick = { DebugSettings.setThumbnailScale(percent) },
                        description = when {
                            percent < 100 -> "Nhẹ và nhanh hơn, ảnh mềm hơn"
                            percent > 100 -> "Nét hơn, tốn dung lượng và mạng hơn"
                            else -> "Cỡ app đang dùng"
                        },
                    )
                }
            }
        }
        item {
            Note(
                "Áp dụng cho thumbnail tải từ lần kế tiếp (khóa cache có cỡ thực nên không dùng nhầm ảnh cỡ cũ). " +
                    "Ảnh đang hiện giữ nguyên tới khi mở lại màn; xóa cache để ép tải lại toàn bộ.",
            )
        }
        if (DebugHooks.clearThumbnailCache != null) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    ODVButton(
                        "Xóa cache thumbnail",
                        {
                            thumbnailCleared = false
                            scope.launch {
                                DebugHooks.clearThumbnailCache?.invoke()
                                thumbnailCleared = true
                            }
                        },
                        style = ODVButtonStyle.Secondary,
                        size = ODVButtonSize.Sm,
                    )
                }
            }
            if (thumbnailCleared) item { Note("Đã xóa cache thumbnail.") }
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
