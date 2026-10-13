package com.lambao.odv.feature.settings

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Cầu nối một chiều từ màn Thêm tài khoản về Màn chính: thêm xong, màn đóng rồi Màn chính hiện Snackbar "Đã thêm <tên>" (TA-07). Back stack do
 * app sở hữu (ADR-0003) nên không có kết quả trả về theo route; sự kiện đi qua singleton này, cùng cách với [SecretUpdateEvents]. Kênh conflated:
 * mất sự kiện cũng chỉ là mất một dòng thông báo.
 */
class AccountEvents {
    private val channel = Channel<String>(Channel.CONFLATED)

    /** Tên hiển thị của tài khoản vừa thêm. */
    val added: Flow<String> = channel.receiveAsFlow()

    fun notifyAdded(displayName: String) {
        channel.trySend(displayName)
    }
}
