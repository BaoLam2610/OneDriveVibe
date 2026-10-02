package com.lambao.odv.feature.auth

import com.lambao.odv.core.domain.model.ConnectionConfig

/**
 * Giữ config vừa kiểm tra xong giữa màn Kết nối và Thiết lập bảo mật (KN-08: config chỉ được lưu sau bước bảo mật).
 *
 * Chỉ nằm trong bộ nhớ, cố ý KHÔNG truyền qua tham số route Navigation 3: route được lưu cùng saved state nên Client
 * Secret sẽ đi theo xuống nơi lưu trạng thái của hệ thống (CH-01, CH-06). Mất khi tiến trình bị hệ thống thu hồi: màn
 * Thiết lập bảo mật thấy trống thì đưa người dùng về Kết nối.
 */
class PendingConnection {
    var config: ConnectionConfig? = null
        private set

    fun hold(config: ConnectionConfig) {
        this.config = config
    }

    fun clear() {
        config = null
    }
}
