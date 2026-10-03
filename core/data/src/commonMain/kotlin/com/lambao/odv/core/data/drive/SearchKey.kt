package com.lambao.odv.core.data.drive

private val vietnameseBase: Map<Char, Char> = buildMap {
    val groups = listOf(
        'a' to "àáạảãâầấậẩẫăằắặẳẵ",
        'e' to "èéẹẻẽêềếệểễ",
        'i' to "ìíịỉĩ",
        'o' to "òóọỏõôồốộổỗơờớợởỡ",
        'u' to "ùúụủũưừứựửữ",
        'y' to "ỳýỵỷỹ",
        'd' to "đ",
    )
    for ((base, letters) in groups) letters.forEach { put(it, base) }
}

/**
 * Khóa so khớp tên (DS-03, TM-05): hạ chữ thường rồi bỏ dấu tiếng Việt, nên "Phim Việt" khớp "viet" và "VIỆT". Chạy ở
 * `commonMain` nên không dùng `java.text.Normalizer`; xử lý cả chữ dựng sẵn lẫn dấu rời (U+0300 đến U+036F).
 * Cùng một hàm cho tên lưu vào Room và từ khóa nhập vào, để hai bên luôn khớp.
 */
internal fun searchKey(text: String): String {
    val lower = text.lowercase()
    return buildString(lower.length) {
        for (c in lower) {
            if (c in '̀'..'ͯ') continue
            append(vietnameseBase[c] ?: c)
        }
    }
}
