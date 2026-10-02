package com.lambao.odv.feature.browser

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.domain.model.DriveItem

/** Một cấp trong đường dẫn từ thư mục gốc xuống thư mục đang xem (không gồm thư mục gốc). */
data class Crumb(val id: String, val name: String)

enum class BrowserErrorKind { Network, Other }

/** [code] để người dùng tra cứu (mã Graph hoặc HTTP); không chứa bí mật. Null với lỗi mạng. */
data class BrowserError(val kind: BrowserErrorKind, val code: String?)

data class BrowserState(
    /** Rỗng = đang ở thư mục gốc (TM-01). */
    val path: List<Crumb> = emptyList(),
    /** Đã lọc và sắp xếp để hiển thị: thư mục trước (TM-02), rồi theo tên. */
    val items: List<DriveItem> = emptyList(),
    val isLoading: Boolean = true,
    /** Lỗi tải nằm trong State để còn sau khi xoay màn hình; "Thử lại" xóa nó. */
    val error: BrowserError? = null,
)

sealed interface BrowserIntent {
    /** Chạm một mục: thư mục thì đi vào, tệp thì mở trong màn xem. */
    data class Open(val item: DriveItem) : BrowserIntent

    /** Nút Back / mũi tên trên thanh tiêu đề: lên một cấp (TM-01). */
    data object GoUp : BrowserIntent

    /** Chạm breadcrumb: 0 là thư mục gốc, 1 là cấp đầu tiên dưới gốc, và cứ thế. */
    data class GoToCrumb(val index: Int) : BrowserIntent
    data object Retry : BrowserIntent
}

sealed interface BrowserEffect {
    /** Mở tệp trong màn xem tương ứng. Màn xem làm ở Lát 5–7; tới lúc đó nơi gọi chưa xử lý. */
    data class OpenFile(val item: DriveItem) : BrowserEffect
}

internal fun AppError.toBrowserError(): BrowserError = when (this) {
    AppError.Network, AppError.Timeout -> BrowserError(BrowserErrorKind.Network, null)
    is AppError.Http -> BrowserError(BrowserErrorKind.Other, code ?: status.toString())
    AppError.SecureStorage, is AppError.Unknown -> BrowserError(BrowserErrorKind.Other, null)
}
