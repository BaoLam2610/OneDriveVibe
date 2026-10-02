package com.lambao.odv.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Màu của các lớp phủ nằm chồng lên thumbnail ảnh/video (huy hiệu thời lượng, nút tròn, thanh xem dở, icon loại tệp).
 * Giá trị cố định, không đổi theo theme vì nền phía sau là ảnh. Cùng giá trị với màu "trên media" nhưng tách tên để
 * component danh sách không phải biết khái niệm màn xem.
 */
object ODVOverlayColors {
    /** Nền mờ đen của huy hiệu, nút tròn, nút X (`#00000099`). */
    val scrim = Color(0x99000000)

    /** Chữ và icon trên [scrim] và trên ảnh. */
    val onScrim = Color(0xFFFFFFFF)

    /** Rãnh thanh xem dở trên ảnh (`#ffffff66`). */
    val progressTrack = Color(0x66FFFFFF)

    /** Phần đã xem trên ảnh: `volt` sáng, giữ cố định ở cả hai theme. */
    val progressIndicator = Color(0xFFC5F23A)
}
