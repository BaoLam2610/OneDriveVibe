package com.lambao.odv.feature.settings

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Cầu nối một chiều từ màn Cập nhật Client Secret về màn Cài đặt: form báo "đã lưu" rồi đóng, Cài đặt hiện Snackbar S5 (thiet-ke-ui.md
 * mục 5.4). Back stack do app sở hữu (ADR-0003) nên không có kết quả trả về theo route; sự kiện đi qua singleton này. Kênh conflated:
 * Cài đặt chưa kịp thu (đang dựng lại) thì sự kiện vẫn còn đó, và mất sự kiện cũng chỉ là mất một dòng thông báo.
 */
class SecretUpdateEvents {
    private val channel = Channel<Unit>(Channel.CONFLATED)

    val saved: Flow<Unit> = channel.receiveAsFlow()

    /** Bỏ sự kiện cũ còn nằm lại (cập nhật từ banner ở Danh sách không có Cài đặt nào để hiện Snackbar). */
    fun clear() {
        channel.tryReceive()
    }

    fun notifySaved() {
        channel.trySend(Unit)
    }
}
