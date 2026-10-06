package com.lambao.odv.core.domain.platform

import kotlinx.coroutines.flow.StateFlow

/**
 * Theo dõi kết nối mạng của máy để hiện thanh "Đang offline" (DS-05). Bản Android dùng `ConnectivityManager` và nằm ở
 * `:androidApp` (cần Context). Chỉ phản ánh máy có đường ra Internet hay không; lỗi gọi Graph cụ thể vẫn do `AppError`.
 */
interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}
