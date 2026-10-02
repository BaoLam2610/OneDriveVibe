// Sinh từ .claude/docs/odv-tokens.json. Token đổi thì sửa file JSON trước, rồi cập nhật file này cho khớp.
package com.lambao.odv.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Hai họ chữ của thiết kế: Be Vietnam Pro (sans) và JetBrains Mono (mono). Phần nền tảng cung cấp file font. */
@Immutable
class ODVFonts(val sans: FontFamily, val mono: FontFamily)

/**
 * 21 style chữ (mục 2.3).
 *
 * Căn chữ giữa chiều cao dòng bằng [LineHeightStyle] (Center, Trim.None). `includeFontPadding` không cần tắt thủ công:
 * từ Compose 1.6 mặc định đã là false.
 */
@Immutable
class ODVTypography(fonts: ODVFonts) {
    /** Tiêu đề lớn: Kết nối OneDrive, Thiết lập bảo mật */
    val display: TextStyle = style(fonts.sans, 32, 40, FontWeight.ExtraBold, -0.02)
    /** Thanh tiêu đề (OneDrive, Cài đặt), tiêu đề Dialog */
    val title: TextStyle = style(fonts.sans, 22, 28, FontWeight.Bold, -0.01)
    /** Xem tiếp / Đọc tiếp, tiêu đề Sheet, dòng tiêu đề trong Bảo mật */
    val heading: TextStyle = style(fonts.sans, 17, 24, FontWeight.SemiBold, 0.0)
    /** Nội dung, tên hàng Cài đặt */
    val body: TextStyle = style(fonts.sans, 15, 22, FontWeight.Normal, 0.0)
    /** Tên tệp trong FileRow, nhóm ngày Thư viện, tên tệp trên trình xem */
    val bodyStrong: TextStyle = style(fonts.sans, 15, 22, FontWeight.SemiBold, 0.0)
    /** Nhãn Button cỡ md */
    val button: TextStyle = style(fonts.sans, 15, 20, FontWeight.SemiBold, 0.0)
    /** Nhãn Button cỡ sm, Chip, Tabs */
    val buttonSm: TextStyle = style(fonts.sans, 14, 20, FontWeight.SemiBold, 0.0)
    /** Meta, mô tả hàng, trợ giúp dưới ô nhập */
    val caption: TextStyle = style(fonts.sans, 13, 18, FontWeight.Normal, 0.0)
    /** Nhãn nhỏ */
    val label: TextStyle = style(fonts.sans, 12, 16, FontWeight.SemiBold, 0.0)
    /** 12:04 / 1:26:02, tốc độ 1x, số trang */
    val timecode: TextStyle = style(fonts.mono, 13, 16, FontWeight.Medium, 0.0)
    /** ID đã che a1b2••••9f0e, mã lỗi */
    val code: TextStyle = style(fonts.mono, 13, 20, FontWeight.Normal, 0.0)
    /** Nhập mã PIN, Tạm khóa nhập mã PIN */
    val screenTitle: TextStyle = style(fonts.sans, 24, 32, FontWeight.Bold, -0.01)
    /** Trạng thái trống, lỗi trên media */
    val stateTitle: TextStyle = style(fonts.sans, 20, 28, FontWeight.Bold, 0.0)
    /** Snackbar, Breadcrumb, Banner */
    val bodySm: TextStyle = style(fonts.sans, 14, 20, FontWeight.Normal, 0.0)
    /** Tên trong thẻ lưới, mục cuối Breadcrumb */
    val bodySmStrong: TextStyle = style(fonts.sans, 14, 20, FontWeight.SemiBold, 0.0)
    /** Nhãn ô nhập, tên nhóm Cài đặt */
    val captionStrong: TextStyle = style(fonts.sans, 13, 18, FontWeight.SemiBold, 0.0)
    /** Meta trong thẻ lưới, đường dẫn kết quả tìm */
    val meta: TextStyle = style(fonts.sans, 12, 16, FontWeight.Normal, 0.0)
    /** BƯỚC 1 / 2 */
    val stepLabel: TextStyle = style(fonts.mono, 12, 16, FontWeight.Medium, 0.04)
    /** Badge thời lượng, viên thuốc 72 / 310 */
    val timecodeSm: TextStyle = style(fonts.mono, 12, 16, FontWeight.Normal, 0.0)
    /** Badge thời lượng trong ô Thư viện */
    val timecodeXs: TextStyle = style(fonts.mono, 11, 16, FontWeight.Normal, 0.0)
    /** Đếm ngược khóa tạm 00:30 */
    val timer: TextStyle = style(fonts.mono, 56, 64, FontWeight.Medium, -0.02)
}

private fun style(family: FontFamily, size: Int, lineHeight: Int, weight: FontWeight, letterSpacingEm: Double): TextStyle =
    TextStyle(
        fontFamily = family,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        fontWeight = weight,
        letterSpacing = letterSpacingEm.em,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )
