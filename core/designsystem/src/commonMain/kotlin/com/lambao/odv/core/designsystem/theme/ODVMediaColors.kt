// Sinh từ .claude/docs/odv-tokens.json. Token đổi thì sửa file JSON trước, rồi cập nhật file này cho khớp.
package com.lambao.odv.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/** Màu màn xem video/ảnh/PDF (mục 2.2). Không đổi theo theme. */
object ODVMediaColors {
    /** Nền màn xem video/ảnh/PDF, không đổi theo theme */
    val background = Color(0xFF000000)
    /** Chữ, icon trên media */
    val onMedia = Color(0xFFFFFFFF)
    /** Chữ phụ trên media */
    val onMediaMuted = Color(0xCCFFFFFF)
    /** Dòng tên tệp và meta nhỏ trên media */
    val onMediaFaint = Color(0x99FFFFFF)
    /** Sau Dialog, Sheet, bảng bên phải */
    val scrim = Color(0x99000000)
    /** Lớp phủ khi thanh điều khiển video hiện */
    val scrimControls = Color(0x80000000)
    /** Sau thẻ Tiếp theo sau 5 giây */
    val scrimStrong = Color(0xB3000000)
    /** Nền viên thuốc: 2,4x, Cắt đầy, 12 / 248; nền HUD */
    val pill = Color(0xB3000000)
    /** Thanh trên và dưới của PDF */
    val toolbar = Color(0xCC000000)
    /** Rãnh thanh tua, rãnh vòng đếm */
    val track = Color(0x33FFFFFF)
    /** Phần đã tải trước; viền nút trên media */
    val buffer = Color(0x66FFFFFF)
    /** Gợn chạm đúp, nền nút mở khóa */
    val ripple = Color(0x26FFFFFF)
    /** Vòng tròn sau icon lỗi trên media */
    val iconWell = Color(0x1FFFFFFF)
    /** Thẻ trên media: Tiếp theo, Mất mạng */
    val card = Color(0xFF1B1D22)
    /** Nền sau trang PDF */
    val pdfCanvas = Color(0xFF2A2C31)
    /** Nút phát, phần đã xem */
    val accent = Color(0xFFC5F23A)
    /** Icon trên nút phát */
    val onAccent = Color(0xFF101217)
}

/** Màu logo (mục 3.2). Cố định, không tint. */
object ODVLogoColors {
    val tile = Color(0xFFC5F23A)
    val fold = Color(0xFF8DB31A)
    val play = Color(0xFF101217)
}
