package com.lambao.odv.tools.debug

import android.content.Context
import com.lambao.odv.core.network.HttpTrafficEntry
import com.lambao.odv.core.network.masked
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Header curl tự tính hoặc đặt theo kết nối, nên chép lại sẽ sai hoặc thừa khi phát lại.
private val SKIPPED_CURL_HEADERS = setOf("content-length", "host", "connection", "accept-encoding", "user-agent")

/** Bọc [text] trong dấu nháy đơn của shell; dấu nháy đơn bên trong được thoát thành `'\''`. */
private fun shellQuote(text: String): String = "'" + text.replace("'", "'\\''") + "'"

/**
 * Lệnh `curl` phát lại request này (tab API của màn Debug). Dữ liệu đúng như [HttpTrafficEntry] đang giữ: nếu muốn bản đã
 * che thì truyền `entry.masked()` (ADR-0013). Bản đầy đủ có `Authorization` và `client_secret`, nên người gọi phải coi kết quả
 * là nhạy cảm (clipboard đánh dấu `IS_SENSITIVE`, không ghi log).
 */
internal fun HttpTrafficEntry.toCurl(): String = buildString {
    append("curl")
    // GET là mặc định của curl; còn lại ghi rõ method để phát lại đúng.
    if (!method.equals("GET", ignoreCase = true)) append(" -X ").append(method)
    append(' ').append(shellQuote(url))
    for ((name, value) in requestHeaders) {
        if (name.lowercase() in SKIPPED_CURL_HEADERS) continue
        append(" \\\n  -H ").append(shellQuote("$name: $value"))
    }
    // --data-raw: không để curl diễn giải ký tự `@` đầu chuỗi như tên tệp.
    requestBody?.takeIf { it.isNotEmpty() }?.let { append(" \\\n  --data-raw ").append(shellQuote(it)) }
}

/**
 * Sao chép cURL của [request] vào clipboard. Dựng chuỗi (có thể che 1M ký tự bằng regex) ngoài luồng chính rồi mới chạm
 * clipboard. [maskTraffic] theo công tắc "Che dữ liệu nhạy cảm" ở tab Khác.
 */
internal suspend fun copyCurl(context: Context, request: DebugRequest, maskTraffic: Boolean) {
    val curl = withContext(Dispatchers.Default) {
        (if (maskTraffic) request.entry.masked() else request.entry).toCurl()
    }
    copyToClipboard(context, "cURL", curl)
}
