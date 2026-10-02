package com.lambao.odv.tools.debug

import android.content.Context

/** Một mục ở tab "Khác" của màn Debug. [onClick] nhận Context của DebugActivity. */
class DebugAction(val title: String, val description: String, val onClick: (Context) -> Unit)

/**
 * Nơi `:androidApp` đăng ký các công cụ riêng của app vào màn Debug mà `:tools:debug` không phải phụ thuộc ngược vào app
 * (vd. mở Foundations gallery, vốn là Activity của `:androidApp`). Đăng ký một lần ở `DebugTools.install()`.
 */
object DebugActions {
    private val registered = mutableListOf<DebugAction>()

    val items: List<DebugAction> get() = registered.toList()

    fun register(action: DebugAction) {
        // Idempotent theo tiêu đề: LazyColumn dùng tiêu đề làm key nên đăng ký trùng sẽ gây crash.
        registered.removeAll { it.title == action.title }
        registered += action
    }
}
