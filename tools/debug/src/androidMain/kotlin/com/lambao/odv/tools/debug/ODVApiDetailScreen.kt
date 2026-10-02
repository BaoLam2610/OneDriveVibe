package com.lambao.odv.tools.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVAppBar
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonSize
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.component.ODVSearchBar
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.network.HttpTrafficEntry
import com.lambao.odv.core.network.masked
import kotlinx.serialization.json.JsonElement
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TEXT_PAGE = 200

/** Một phần của màn chi tiết: văn bản thường hoặc JSON. */
internal class Section(val id: String, val title: String, val text: String?, val json: JsonElement?) {
    val copyText: String get() = json?.let(::prettyJsonText) ?: text.orEmpty()
    fun matchCount(query: String): Int =
        if (json != null) countJsonMatches(json, query) else countTextMatches(text.orEmpty(), query)
}

/** Trạng thái +/- của một phần, không lưu qua xoay màn hình (công cụ debug). */
private class SectionState {
    var expanded by mutableStateOf(true)
    var defaultDepth by mutableIntStateOf(2)
    val overrides = mutableStateMapOf<String, Boolean>()
    val pageLimits = mutableStateMapOf<String, Int>()
    var textLimit by mutableIntStateOf(TEXT_PAGE)
}

private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

private fun headersText(headers: List<Pair<String, String>>): String =
    headers.joinToString("\n") { "${it.first}: ${it.second}" }

/** Body dạng form-urlencoded (endpoint token) đọc dễ hơn khi tách thành từng dòng `khóa: giá trị` đã giải mã. */
private fun formBodyText(body: String): String = body.split('&').joinToString("\n") { pair ->
    val index = pair.indexOf('=')
    if (index < 0) pair else URLDecoder.decode(pair.substring(0, index), "UTF-8") + ": " + URLDecoder.decode(pair.substring(index + 1), "UTF-8")
}

private fun bodySection(id: String, title: String, body: String?): Section {
    if (body.isNullOrEmpty()) return Section(id, title, "(không có)", null)
    val json = parseJsonOrNull(body)
    if (json != null) return Section(id, title, null, json)
    val looksLikeForm = body.contains('=') && !body.contains('\n') && !body.contains(' ')
    return Section(id, title, if (looksLikeForm) formBodyText(body) else body, null)
}

private fun buildSections(request: DebugRequest, entry: HttpTrafficEntry): List<Section> {
    val overview = buildString {
        appendLine("Method: ${entry.method}")
        appendLine("URL: ${entry.url}")
        appendLine("Status: ${entry.status ?: "(không có phản hồi)"}")
        appendLine("Thời gian: ${entry.durationMs} ms")
        append("Lúc: ${timeFormat.format(Date(request.timeMs))}")
        entry.error?.let { append("\nLỗi: $it") }
    }
    return listOf(
        Section("overview", "Tổng quan", overview, null),
        Section("reqHeaders", "Request header", headersText(entry.requestHeaders).ifEmpty { "(không có)" }, null),
        bodySection("reqBody", "Request body", entry.requestBody),
        Section("resHeaders", "Response header", headersText(entry.responseHeaders).ifEmpty { "(không có)" }, null),
        bodySection("resBody", "Response body", entry.responseBody),
    )
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    // Có thể chứa token/secret: yêu cầu hệ thống không hiện bản xem trước nội dung vừa sao chép (Android 13+).
    clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Đã sao chép: $label", Toast.LENGTH_SHORT).show()
}

/**
 * Màn chi tiết một request (ADR-0013): Tổng quan, Request header, Request body, Response header, Response body.
 * JSON hiển thị dạng cây có nút +/- ở từng nút và nút mở/thu hết; mảng dài chỉ hiện 50 phần tử đầu kèm "Hiện thêm".
 * Tìm kiếm khớp trong mọi phần, hiện tổng số kết quả và số kết quả của từng phần. Dữ liệu hiện đầy đủ, trừ khi bật che.
 */
@Composable
internal fun ODVApiDetailScreen(request: DebugRequest, maskTraffic: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val entry = remember(request, maskTraffic) { if (maskTraffic) request.entry.masked() else request.entry }
    val sections = remember(request.id, maskTraffic) { SectionCache.get(request, maskTraffic) }
    val states = remember(request.id) { sections.associate { it.id to SectionState() } }
    var searching by rememberSaveable { mutableStateOf(false) }
    var rawQuery by rememberSaveable { mutableStateOf("") }
    // Back đóng thanh tìm kiếm trước, rồi mới thoát màn chi tiết.
    BackHandler(enabled = searching) {
        searching = false
        rawQuery = ""
    }
    val query = if (searching) rawQuery else ""
    val counts = remember(sections, query) { sections.associate { it.id to it.matchCount(query) } }
    val total = counts.values.sum()

    ODVScaffold(
        topBar = {
            if (searching) {
                ODVSearchBar(
                    query = rawQuery,
                    onQueryChange = { rawQuery = it },
                    placeholder = "Tìm trong header, request, response",
                    backContentDescription = "Đóng tìm kiếm",
                    onBack = { searching = false; rawQuery = "" },
                    clearContentDescription = "Xóa từ khóa",
                )
            } else {
                ODVAppBar(
                    title = "${request.entry.method} ${request.entry.status ?: "—"}",
                    navigation = { ODVIconButton(ODVIcon.ArrowLeft, "Quay lại", onBack) },
                    actions = {
                        ODVIconButton(ODVIcon.Search, "Tìm kiếm", { searching = true })
                        ODVIconButton(ODVIcon.Close, "Đóng", onBack)
                    },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize()) {
            if (query.isNotEmpty()) {
                Text(
                    "$total kết quả",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = ODVTheme.typography.captionStrong,
                    color = if (total > 0) ODVTheme.colors.voltText else ODVTheme.colors.danger,
                )
            }
            LazyColumn(contentPadding = padding) {
                sections.forEach { section ->
                    sectionItems(
                        section = section,
                        state = states.getValue(section.id),
                        query = query,
                        matches = counts.getValue(section.id),
                        onCopy = { copyToClipboard(context, section.title, section.copyText) },
                    )
                }
            }
        }
    }
}

private fun LazyListScope.sectionItems(
    section: Section,
    state: SectionState,
    query: String,
    matches: Int,
    onCopy: () -> Unit,
) {
    // Đang tìm kiếm mà phần này có kết quả thì luôn mở để thấy kết quả.
    val expanded = state.expanded || (query.isNotEmpty() && matches > 0)
    item(key = "header:${section.id}") {
        SectionHeader(section, expanded, matches, query, state, onCopy)
    }
    if (!expanded) return
    val json = section.json
    if (json != null) {
        val matched = containersWithMatch(json, query)
        val rows = flattenJson(json, state.defaultDepth, state.overrides, state.pageLimits, query, matched)
        items(rows.size, key = { "${section.id}:${rows[it].id}" }) { index ->
            JsonRowView(
                row = rows[index],
                query = query,
                onToggle = { path -> state.overrides[path] = !(rows[index].expanded) },
                onMore = { path -> state.pageLimits[path] = (state.pageLimits[path] ?: FIRST_PAGE) + PAGE_STEP },
            )
        }
    } else {
        val lines = section.text.orEmpty().lines()
        val limit = if (query.isNotEmpty()) Int.MAX_VALUE else state.textLimit
        items(lines.take(limit).size, key = { "${section.id}:$it" }) { index ->
            TextLine(lines[index], query)
        }
        if (lines.size > limit) {
            item(key = "more:${section.id}") {
                Text(
                    "… còn ${lines.size - limit} dòng. Hiện thêm",
                    modifier = Modifier.fillMaxWidth().clickable { state.textLimit += TEXT_PAGE }.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = ODVTheme.typography.captionStrong,
                    color = ODVTheme.colors.voltText,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(section: Section, expanded: Boolean, matches: Int, query: String, state: SectionState, onCopy: () -> Unit) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { state.expanded = !state.expanded }.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(if (expanded) "−" else "+", style = type.heading, color = colors.voltText, modifier = Modifier.width(20.dp))
            Text(
                section.title + if (query.isNotEmpty()) "  ($matches)" else "",
                style = type.bodyStrong,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
        }
        if (expanded) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ODVButton("Sao chép", onCopy, style = ODVButtonStyle.Secondary, size = ODVButtonSize.Sm)
                if (section.json != null) {
                    ODVButton(
                        "+ Mở hết",
                        { state.defaultDepth = Int.MAX_VALUE; state.overrides.clear() },
                        style = ODVButtonStyle.Ghost,
                        size = ODVButtonSize.Sm,
                    )
                    ODVButton(
                        "− Thu gọn",
                        { state.defaultDepth = 1; state.overrides.clear() },
                        style = ODVButtonStyle.Ghost,
                        size = ODVButtonSize.Sm,
                    )
                }
            }
        }
    }
}

@Composable
private fun TextLine(text: String, query: String) {
    val colors = ODVTheme.colors
    val hl = SpanStyle(background = colors.warningSoft, color = colors.ink)
    val annotated = buildAnnotatedString {
        appendMatches(text, query, SpanStyle(color = colors.ink), hl)
    }
    Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 1.dp)) {
        Text(annotated, style = ODVTheme.typography.code)
    }
}

@Composable
private fun JsonRowView(row: JsonRow, query: String, onToggle: (String) -> Unit, onMore: (String) -> Unit) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    val hl = SpanStyle(background = colors.warningSoft, color = colors.ink)
    val annotated: AnnotatedString = buildAnnotatedString {
        row.key?.let {
            appendMatches("\"$it\"", query, SpanStyle(color = colors.voltText), hl)
            withStyle(SpanStyle(color = colors.inkMuted)) { append(": ") }
        }
        val valueColor: Color = when {
            row.kind == JsonRow.Kind.Collapsed || row.kind == JsonRow.Kind.More -> colors.inkMuted
            row.kind != JsonRow.Kind.Value -> colors.ink
            row.text.startsWith("\"") -> colors.success
            row.text == "true" || row.text == "false" || row.text == "null" -> colors.kindPhoto
            else -> colors.kindVideo
        }
        if (row.kind == JsonRow.Kind.Value) {
            appendMatches(row.text, query, SpanStyle(color = valueColor), hl)
        } else {
            withStyle(SpanStyle(color = valueColor)) { append(row.text) }
        }
        if (row.comma) withStyle(SpanStyle(color = colors.inkMuted)) { append(",") }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = (4 + row.depth * 14).dp, end = 8.dp)
            .then(if (row.morePath != null) Modifier.clickable { onMore(row.morePath) } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val path = row.foldPath
        if (path != null) {
            Box(
                Modifier.size(28.dp).clickable { onToggle(path) },
                contentAlignment = Alignment.Center,
            ) {
                Text(if (row.expanded) "−" else "+", style = type.heading, color = colors.voltText)
            }
        } else {
            Spacer(Modifier.width(28.dp))
        }
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            Text(annotated, style = type.code)
        }
    }
}

/** Thêm [text] vào builder, tô nền các đoạn khớp [query] (không phân biệt hoa thường). */
private fun AnnotatedString.Builder.appendMatches(text: String, query: String, base: SpanStyle, highlight: SpanStyle) {
    if (query.isEmpty()) {
        withStyle(base) { append(text) }
        return
    }
    var from = 0
    while (true) {
        val index = text.indexOf(query, from, ignoreCase = true)
        if (index < 0) break
        if (index > from) withStyle(base) { append(text.substring(from, index)) }
        withStyle(base.merge(highlight)) { append(text.substring(index, index + query.length)) }
        from = index + query.length
    }
    if (from < text.length) withStyle(base) { append(text.substring(from)) }
}

/**
 * Bộ nhớ đệm các [Section] đã dựng (kèm JSON đã parse) theo request và chế độ che, dùng chung cho màn chi tiết và cho
 * việc đếm kết quả ở danh sách, để hai nơi đếm giống hệt nhau và không parse lại body lớn mỗi lần gõ phím.
 */
internal object SectionCache {
    private val cache = HashMap<Pair<Long, Boolean>, List<Section>>()

    @Synchronized
    fun get(request: DebugRequest, masked: Boolean): List<Section> = cache.getOrPut(request.id to masked) {
        buildSections(request, if (masked) request.entry.masked() else request.entry)
    }

    /** Bỏ các mục của request đã bị xóa khỏi [ApiTrafficStore]. */
    @Synchronized
    fun retain(ids: Set<Long>) {
        cache.keys.retainAll { it.first in ids }
    }
}

/** Tổng số kết quả của [query] trong mọi phần của một request: cùng cách đếm với màn chi tiết. */
internal fun requestMatchCount(request: DebugRequest, masked: Boolean, query: String): Int =
    SectionCache.get(request, masked).sumOf { it.matchCount(query) }
