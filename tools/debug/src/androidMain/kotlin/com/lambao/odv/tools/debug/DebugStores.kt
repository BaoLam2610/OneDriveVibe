package com.lambao.odv.tools.debug

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import com.lambao.odv.core.network.HttpTrafficEntry
import com.lambao.odv.core.network.HttpTrafficRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.dsl.module

private const val MAX_LOGS = 500
private const val MAX_REQUESTS = 200

/** Một dòng log local (Kermit) đang giữ trong bộ đệm. */
class DebugLogLine(
    val id: Long,
    val timeMs: Long,
    val severity: Severity,
    val tag: String,
    val message: String,
    val throwable: String?,
)

/** Bộ đệm vòng chứa log local, mới nhất ở cuối. Chỉ nằm trong bộ nhớ, mất khi tiến trình chết. */
object DebugLogStore {
    private var nextId = 0L
    private val _lines = MutableStateFlow<List<DebugLogLine>>(emptyList())
    val lines: StateFlow<List<DebugLogLine>> = _lines.asStateFlow()

    fun add(severity: Severity, tag: String, message: String, throwable: Throwable?) {
        val line = DebugLogLine(
            id = synchronized(this) { nextId++ },
            timeMs = System.currentTimeMillis(),
            severity = severity,
            tag = tag,
            message = message,
            throwable = throwable?.stackTraceToString()?.take(4000),
        )
        _lines.update { (it + line).takeLast(MAX_LOGS) }
    }

    fun clear() {
        _lines.value = emptyList()
    }
}

/** Một request đã làm sạch kèm thời điểm nhận. */
class DebugRequest(val id: Long, val timeMs: Long, val entry: HttpTrafficEntry)

/** Bộ đệm vòng chứa lưu lượng API (Graph và token), mới nhất ở cuối. Dữ liệu đã được `:core:network` làm sạch (CH-06). */
object ApiTrafficStore : HttpTrafficRecorder {
    private var nextId = 0L
    private val _requests = MutableStateFlow<List<DebugRequest>>(emptyList())
    val requests: StateFlow<List<DebugRequest>> = _requests.asStateFlow()

    override fun record(entry: HttpTrafficEntry) {
        val request = DebugRequest(synchronized(this) { nextId++ }, System.currentTimeMillis(), entry)
        _requests.update { (it + request).takeLast(MAX_REQUESTS) }
    }

    fun clear() {
        _requests.value = emptyList()
    }
}

/** LogWriter của Kermit đẩy log vào [DebugLogStore]. */
class DebugLogWriter : LogWriter() {
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        DebugLogStore.add(severity, tag, message, throwable)
    }
}

object DebugLogging {
    /** Bản debug: ghi ra Logcat và vào màn Debug, từ mức Verbose. */
    fun install() {
        Logger.setLogWriters(platformLogWriter(), DebugLogWriter())
        Logger.setMinSeverity(Severity.Verbose)
    }
}

/** Binding Koin: cài bộ ghi lưu lượng cho `:core:network` (bản release không có nên không ghi gì). */
val debugModule = module {
    single<HttpTrafficRecorder> { ApiTrafficStore }
}
