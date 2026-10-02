package com.lambao.odv.tools.debug

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val prettyJson = Json { prettyPrint = true }

/** Parse [text] thành JSON, null nếu không phải JSON hợp lệ. */
internal fun parseJsonOrNull(text: String?): JsonElement? {
    val trimmed = text?.trim().orEmpty()
    if (trimmed.isEmpty() || (trimmed.first() != '{' && trimmed.first() != '[')) return null
    return try {
        Json.parseToJsonElement(trimmed)
    } catch (e: Exception) {
        null
    }
}

/** JSON in đẹp (thụt 4 khoảng trắng) để sao chép. */
internal fun prettyJsonText(element: JsonElement): String = prettyJson.encodeToString(JsonElement.serializer(), element)

/** Số lần [query] xuất hiện trong [text], không phân biệt hoa thường, không chồng lấn. Query rỗng trả 0. */
internal fun countTextMatches(text: String, query: String): Int {
    if (query.isEmpty()) return 0
    var count = 0
    var from = 0
    while (true) {
        val index = text.indexOf(query, from, ignoreCase = true)
        if (index < 0) return count
        count++
        from = index + query.length
    }
}

/** Số lần khớp trong khóa và giá trị nguyên thủy của cây JSON (đúng phần được hiển thị). */
internal fun countJsonMatches(element: JsonElement, query: String): Int {
    if (query.isEmpty()) return 0
    return when (element) {
        is JsonObject -> element.entries.sumOf { (key, value) -> countTextMatches(key, query) + countJsonMatches(value, query) }
        is JsonArray -> element.sumOf { countJsonMatches(it, query) }
        is JsonPrimitive -> countTextMatches(primitiveText(element), query)
    }
}

/** Văn bản hiển thị của một giá trị nguyên thủy: chuỗi có nháy kép, số/bool/null để nguyên. */
internal fun primitiveText(primitive: JsonPrimitive): String = when {
    primitive is JsonNull -> "null"
    primitive.isString -> "\"" + primitive.content + "\""
    else -> primitive.content
}

/** Đường dẫn của mọi object/array có khớp [query] ở đâu đó trong cây con (kể cả tổ tiên), để tự mở khi tìm kiếm. */
internal fun containersWithMatch(root: JsonElement, query: String): Set<String> {
    if (query.isEmpty()) return emptySet()
    val result = mutableSetOf<String>()
    fun visit(element: JsonElement, path: String): Boolean {
        val found = when (element) {
            is JsonObject -> element.entries.map { (key, value) ->
                val inKey = countTextMatches(key, query) > 0
                visit(value, "$path.$key") || inKey
            }.any { it }
            is JsonArray -> element.mapIndexed { i, child -> visit(child, "$path[$i]") }.any { it }
            is JsonPrimitive -> countTextMatches(primitiveText(element), query) > 0
        }
        if (found && element !is JsonPrimitive) result += path
        return found
    }
    visit(root, ROOT_PATH)
    return result
}

internal const val ROOT_PATH = "$"
private const val DEFAULT_PAGE = 50

/** Một dòng đã trải phẳng của cây JSON. */
internal class JsonRow(
    /** Khóa ổn định của dòng (đường dẫn + loại) để LazyColumn không dùng nhầm state cuộn khi gập/mở. */
    val id: String,
    val depth: Int,
    /** Khóa của object cha, null với phần tử mảng hoặc gốc. */
    val key: String?,
    /** `{` `[` mở container, `}` `]` đóng, hoặc giá trị nguyên thủy / bản tóm tắt khi thu gọn. */
    val text: String,
    val kind: Kind,
    val comma: Boolean,
    /** Có nút +/- ở dòng này (container có phần tử). */
    val foldPath: String?,
    val expanded: Boolean,
    /** Dòng "Hiện thêm" của mảng/object dài: path cần tăng số phần tử hiển thị. */
    val morePath: String? = null,
) {
    enum class Kind { Open, Close, Collapsed, Empty, Value, More }
}

/**
 * Trải phẳng [root] thành các dòng hiển thị.
 *
 * Quy tắc mở: container đang có kết quả tìm kiếm luôn mở; còn lại theo [overrides] (người dùng bấm +/-), nếu chưa có thì
 * mở khi độ sâu < [defaultDepth]. Container dài chỉ hiện [DEFAULT_PAGE] phần tử đầu (tăng bằng "Hiện thêm"), trừ khi đang
 * tìm kiếm thì hiện hết để mọi kết quả đều nhìn thấy.
 */
internal fun flattenJson(
    root: JsonElement,
    defaultDepth: Int,
    overrides: Map<String, Boolean>,
    pageLimits: Map<String, Int>,
    query: String,
    matched: Set<String>,
): List<JsonRow> {
    val rows = mutableListOf<JsonRow>()

    fun isExpanded(path: String, depth: Int): Boolean = when {
        path in matched -> true
        else -> overrides[path] ?: (depth < defaultDepth)
    }

    fun emit(element: JsonElement, key: String?, path: String, depth: Int, comma: Boolean) {
        when (element) {
            is JsonPrimitive -> rows += JsonRow(path, depth, key, primitiveText(element), JsonRow.Kind.Value, comma, null, false)
            is JsonObject -> {
                if (element.isEmpty()) {
                    rows += JsonRow(path, depth, key, "{}", JsonRow.Kind.Empty, comma, null, false)
                    return
                }
                val expanded = isExpanded(path, depth)
                if (!expanded) {
                    rows += JsonRow(path, depth, key, "{ … } ${element.size} khóa", JsonRow.Kind.Collapsed, comma, path, false)
                    return
                }
                rows += JsonRow("$path#open", depth, key, "{", JsonRow.Kind.Open, false, path, true)
                val limit = if (query.isNotEmpty()) Int.MAX_VALUE else pageLimits[path] ?: DEFAULT_PAGE
                val entries = element.entries.toList()
                entries.take(limit).forEachIndexed { i, (childKey, child) ->
                    emit(child, childKey, "$path.$childKey", depth + 1, comma = i < entries.lastIndex)
                }
                if (entries.size > limit) {
                    rows += JsonRow("$path#more", depth + 1, null, "… còn ${entries.size - limit} khóa", JsonRow.Kind.More, false, null, false, morePath = path)
                }
                rows += JsonRow("$path#close", depth, null, "}", JsonRow.Kind.Close, comma, null, false)
            }
            is JsonArray -> {
                if (element.isEmpty()) {
                    rows += JsonRow(path, depth, key, "[]", JsonRow.Kind.Empty, comma, null, false)
                    return
                }
                val expanded = isExpanded(path, depth)
                if (!expanded) {
                    rows += JsonRow(path, depth, key, "[ … ] ${element.size} mục", JsonRow.Kind.Collapsed, comma, path, false)
                    return
                }
                rows += JsonRow("$path#open", depth, key, "[", JsonRow.Kind.Open, false, path, true)
                val limit = if (query.isNotEmpty()) Int.MAX_VALUE else pageLimits[path] ?: DEFAULT_PAGE
                element.take(limit).forEachIndexed { i, child ->
                    emit(child, null, "$path[$i]", depth + 1, comma = i < element.lastIndex)
                }
                if (element.size > limit) {
                    rows += JsonRow("$path#more", depth + 1, null, "… còn ${element.size - limit} mục", JsonRow.Kind.More, false, null, false, morePath = path)
                }
                rows += JsonRow("$path#close", depth, null, "]", JsonRow.Kind.Close, comma, null, false)
            }
        }
    }

    emit(root, null, ROOT_PATH, 0, comma = false)
    return rows
}

/** Số phần tử hiển thị thêm mỗi lần bấm "Hiện thêm". */
internal const val PAGE_STEP = DEFAULT_PAGE
internal const val FIRST_PAGE = DEFAULT_PAGE
