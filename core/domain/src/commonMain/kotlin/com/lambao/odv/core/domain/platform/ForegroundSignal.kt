package com.lambao.odv.core.domain.platform

import kotlinx.coroutines.flow.Flow

/**
 * Tín hiệu "app vừa vào foreground", để những giá trị tính theo ngày hiện tại (banner secret sắp hết hạn, CD-06) tính lại khi người
 * dùng quay lại app, không phải chờ ngày hết hạn đổi. `commonMain` không có vòng đời tiến trình, nên bản Android nằm ở `:androidApp`
 * như [UtcOffsetProvider]. Flow luôn có giá trị hiện tại ngay khi thu (đếm số lần vào foreground), mỗi lần vào foreground tăng một.
 */
interface ForegroundSignal {
    val entries: Flow<Int>
}
