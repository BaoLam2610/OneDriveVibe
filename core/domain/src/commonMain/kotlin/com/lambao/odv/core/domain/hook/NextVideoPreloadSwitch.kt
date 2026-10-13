package com.lambao.odv.core.domain.hook

/**
 * Công tắc bật/tắt việc chuẩn bị sẵn video kế tiếp của tab Short trong RAM (ADR-0028). Bản phát hành luôn bật; bản debug cho tắt từ màn Debug
 * (tab Khác) để so thời gian tới khung hình đầu giữa `nguồn=ram` và `nguồn=thường` trong cùng một phiên thử. Đọc lại mỗi lần cập nhật cửa sổ tải
 * trước, nên đổi công tắc có hiệu lực ngay từ lần vuốt kế tiếp, không cần khởi động lại app. Cùng kiểu với [ThumbnailQuality].
 */
interface NextVideoPreloadSwitch {
    fun isEnabled(): Boolean
}
