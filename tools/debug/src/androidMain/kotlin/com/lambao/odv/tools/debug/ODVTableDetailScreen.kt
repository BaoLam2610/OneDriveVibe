package com.lambao.odv.tools.debug

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVAppBar
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonSize
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVDialog
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.component.ODVSearchBar
import com.lambao.odv.core.designsystem.component.ODVTextField
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.tools.debug.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

// Ô quá dài (vd. JSON, URL có tempauth) chỉ hiện đầu; sao chép vẫn lấy đủ.
private const val MAX_CELL_CHARS = 1000
private const val SEARCH_DEBOUNCE_MS = 250L

private sealed interface PageState {
    class Loaded(val page: TablePage) : PageState
    class Failed(val reason: String) : PageState
}

/**
 * Màn chi tiết một bảng DB (ADR-0012), cùng kiểu với màn chi tiết log API: tìm kiếm có debounce và tô khớp, chọn chữ để sao chép,
 * nút sao chép từng dòng và cả trang. Phân trang [TABLE_PAGE_SIZE] dòng/trang vì bảng có thể hàng chục nghìn dòng (drive_item).
 * Dữ liệu hiện đầy đủ trừ khi bật che ([maskTraffic], cùng công tắc với log API).
 *
 * [reload] tăng lên để đọc lại sau khi ở ngoài xóa bảng.
 */
@Composable
internal fun ODVTableDetailScreen(
    dbName: String,
    table: String,
    maskTraffic: Boolean,
    reload: Int,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val browser = remember(dbName) { DatabaseBrowser(sqlSourceFor(context, dbName)) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var rawQuery by rememberSaveable { mutableStateOf("") }
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }
    var jumpOpen by rememberSaveable { mutableStateOf(false) }
    // Back đóng thanh tìm kiếm trước, rồi mới thoát màn chi tiết.
    BackHandler(enabled = searching) {
        searching = false
        rawQuery = ""
        pageIndex = 0
    }
    // Debounce 250 ms: mỗi lần gõ là một truy vấn LIKE trên mọi cột.
    val query by produceState("", rawQuery, searching) {
        val typed = if (searching) rawQuery else ""
        if (typed.isNotEmpty()) delay(SEARCH_DEBOUNCE_MS)
        value = typed
    }
    val state by produceState<PageState?>(null, browser, table, query, pageIndex, maskTraffic, reload, refresh) {
        value = try {
            PageState.Loaded(browser.page(table, query, pageIndex, maskTraffic))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PageState.Failed(e::class.simpleName ?: "lỗi")
        }
    }
    val loaded = (state as? PageState.Loaded)?.page
    // Sau khi xóa bảng hoặc từ khóa mới, trang đang đứng có thể vượt quá số trang còn lại.
    LaunchedEffect(loaded?.pageCount) {
        if (loaded != null && pageIndex > loaded.pageCount - 1) pageIndex = loaded.pageCount - 1
    }

    ODVScaffold(
        topBar = {
            if (searching) {
                ODVSearchBar(
                    query = rawQuery,
                    onQueryChange = {
                        rawQuery = it
                        pageIndex = 0
                    },
                    placeholder = "Tìm trong mọi cột của $table",
                    backContentDescription = "Đóng tìm kiếm",
                    onBack = {
                        searching = false
                        rawQuery = ""
                        pageIndex = 0
                    },
                    clearContentDescription = "Xóa từ khóa",
                )
            } else {
                ODVAppBar(
                    title = table,
                    navigation = { ODVIconButton(ODVIcon.ArrowLeft, "Quay lại", onBack) },
                    actions = {
                        DebugIconButton(
                            R.drawable.ic_debug_copy,
                            "Sao chép cả trang",
                            { loaded?.let { copyToClipboard(context, "trang ${pageIndex + 1} của $table", pageText(it, pageIndex)) } },
                            tint = ODVTheme.colors.ink,
                        )
                        ODVIconButton(ODVIcon.Sync, "Làm mới", { refresh++ })
                        ODVIconButton(ODVIcon.Search, "Tìm kiếm", { searching = true })
                        DebugIconButton(R.drawable.ic_debug_delete, "Xóa toàn bộ bảng", onDelete, tint = ODVTheme.colors.danger)
                    },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                null -> Note("Đang đọc…")
                is PageState.Failed -> Note("Không đọc được bảng: ${current.reason}")
                is PageState.Loaded -> {
                    val page = current.page
                    Summary(dbName, page, query)
                    if (page.rows.isEmpty()) {
                        Note(if (query.isEmpty()) "Bảng trống." else "Không có dòng nào khớp.")
                    }
                    val listState = rememberLazyListState()
                    // Sang trang khác thì về đầu danh sách.
                    LaunchedEffect(pageIndex, query) { listState.scrollToItem(0) }
                    // SelectionContainer: nhấn giữ để chọn/sao chép chữ; nút sao chép từng dòng nằm ngoài vùng chọn.
                    SelectionContainer(Modifier.weight(1f)) {
                        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(page.rows, key = { index, _ -> "$pageIndex:$index" }) { index, row ->
                                RowCard(
                                    number = pageIndex.toLong() * TABLE_PAGE_SIZE + index + 1,
                                    columns = page.columns,
                                    row = row,
                                    query = query,
                                    onCopy = { copyToClipboard(context, "dòng ${index + 1} của $table", rowText(page.columns, row)) },
                                )
                            }
                        }
                    }
                    PageBar(
                        index = pageIndex,
                        count = page.pageCount,
                        onPrevious = { pageIndex-- },
                        onNext = { pageIndex++ },
                        onJump = { jumpOpen = true },
                    )
                    if (jumpOpen) {
                        JumpDialog(count = page.pageCount, onDismiss = { jumpOpen = false }, onJump = {
                            pageIndex = it
                            jumpOpen = false
                        })
                    }
                }
            }
        }
    }
}

private fun rowText(columns: List<String>, row: List<String>): String =
    columns.indices.joinToString("\n") { "${columns[it]}: ${row.getOrElse(it) { "" }}" }

private fun pageText(page: TablePage, index: Int): String =
    page.rows.mapIndexed { i, row -> "# ${index.toLong() * TABLE_PAGE_SIZE + i + 1}\n${rowText(page.columns, row)}" }.joinToString("\n\n")

@Composable
private fun Note(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        style = ODVTheme.typography.code,
        color = ODVTheme.colors.inkMuted,
    )
}

@Composable
private fun Summary(dbName: String, page: TablePage, query: String) {
    val colors = ODVTheme.colors
    if (query.isEmpty()) {
        Note("$dbName · ${page.total} dòng")
    } else {
        Text(
            "${page.total} dòng khớp",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = ODVTheme.typography.captionStrong,
            color = if (page.total > 0) colors.voltText else colors.danger,
        )
    }
}

@Composable
private fun RowCard(number: Long, columns: List<String>, row: List<String>, query: String, onCopy: () -> Unit) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    val highlight = SpanStyle(background = colors.warningSoft, color = colors.ink)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(colors.surface, ODVTheme.shapes.sm)
            .padding(start = 12.dp, end = 4.dp, bottom = 8.dp),
    ) {
        DisableSelection {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("#$number", style = type.captionStrong, color = colors.voltText, modifier = Modifier.weight(1f))
                DebugIconButton(R.drawable.ic_debug_copy, "Sao chép dòng $number", onCopy)
            }
        }
        columns.forEachIndexed { index, column ->
            val raw = row.getOrElse(index) { "" }
            val shown = if (raw.length > MAX_CELL_CHARS) raw.take(MAX_CELL_CHARS) + "… (+${raw.length - MAX_CELL_CHARS} ký tự)" else raw
            val text = buildAnnotatedString {
                appendMatches("$column: ", "", SpanStyle(color = colors.inkMuted), highlight)
                appendMatches(shown, query, SpanStyle(color = colors.ink), highlight)
            }
            // Xuống dòng thay vì cuộn ngang, để vuốt dọc luôn cuộn cả màn.
            Text(text, style = type.code, modifier = Modifier.fillMaxWidth().padding(end = 8.dp, top = 2.dp))
        }
    }
}

@Composable
private fun PageBar(index: Int, count: Int, onPrevious: () -> Unit, onNext: () -> Unit, onJump: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ODVButton("Trước", onPrevious, style = ODVButtonStyle.Secondary, size = ODVButtonSize.Sm, enabled = index > 0)
        // Chạm vào số trang để nhảy tới trang bất kỳ.
        Text(
            "trang ${index + 1} / $count",
            modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onJump).padding(horizontal = 12.dp, vertical = 14.dp),
            style = ODVTheme.typography.bodySmStrong,
            color = ODVTheme.colors.ink,
        )
        ODVButton("Sau", onNext, style = ODVButtonStyle.Secondary, size = ODVButtonSize.Sm, enabled = index < count - 1)
    }
}

@Composable
private fun JumpDialog(count: Int, onDismiss: () -> Unit, onJump: (Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val target = text.toIntOrNull()?.takeIf { it in 1..count }
    ODVDialog(
        onDismissRequest = onDismiss,
        title = "Nhảy tới trang",
        extra = {
            ODVTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(7) },
                label = "Trang (1 đến $count)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
    ) {
        ODVButton("Hủy", onDismiss, style = ODVButtonStyle.Ghost)
        ODVButton("Đi", { target?.let { onJump(it - 1) } }, enabled = target != null)
    }
}
