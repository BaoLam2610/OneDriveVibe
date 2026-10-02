// Sinh từ .claude/docs/odv-tokens.json. Token đổi thì sửa file JSON trước, rồi cập nhật file này cho khớp.
package com.lambao.odv.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as lerpColor

/** Màu theo theme (mục 2.1). Màn xem dùng [ODVMediaColors], không đổi theo theme. */
@Immutable
class ODVColors(
    val isDark: Boolean,
    /** Nền màn hình (Danh sách, Cài đặt, Kết nối, Khóa) */
    val bg: Color,
    /** Thẻ, nhóm Cài đặt, Dialog, Sheet, ô nhập */
    val surface: Color,
    /** Nền phụ: Tabs, phím PIN, chip lọc, hàng đang nhấn */
    val surface2: Color,
    /** Viền thẻ, kẻ giữa các hàng */
    val line: Color,
    /** Viền ô nhập, Radio, Switch tắt, nút secondary */
    val lineStrong: Color,
    /** Chữ chính, icon */
    val ink: Color,
    /** Chữ phụ, mô tả, giá trị Cài đặt */
    val inkMuted: Color,
    /** Chữ rất phụ: đường dẫn kết quả tìm, mũi tên Breadcrumb */
    val inkFaint: Color,
    /** Mảng nhấn: nút chính, Switch bật, chấm PIN, phần đã xem */
    val volt: Color,
    /** Nút chính khi nhấn */
    val voltPressed: Color,
    /** Chữ và icon trên volt */
    val onVolt: Color,
    /** Nền chip chọn, ô thư mục, phím PIN đang nhấn */
    val voltSoft: Color,
    /** Chữ/viền nhấn: nút ghost, Radio chọn, tiêu đề nhóm Cài đặt, focus ring */
    val voltText: Color,
    /** Nhận diện video (chấm chip) */
    val kindVideo: Color,
    /** Nền nhận diện video */
    val kindVideoSoft: Color,
    /** Nhận diện ảnh */
    val kindPhoto: Color,
    /** Nền nhận diện ảnh */
    val kindPhotoSoft: Color,
    /** Nhận diện PDF, tiến độ đọc */
    val kindPdf: Color,
    /** Nền thumbnail PDF */
    val kindPdfSoft: Color,
    /** Lỗi, hành động nguy hiểm */
    val danger: Color,
    /** Nền nút danger tonal, icon lỗi */
    val dangerSoft: Color,
    /** Cảnh báo */
    val warning: Color,
    /** Nền Banner cảnh báo */
    val warningSoft: Color,
    /** Thành công */
    val success: Color,
    /** Nền Banner thành công */
    val successSoft: Color,
    /** Nền Snackbar, bong bóng cuộn nhanh */
    val inverseSurface: Color,
    /** Chữ trên inverse-surface */
    val inverseInk: Color,
    /** Nút hành động trên Snackbar */
    val inverseAccent: Color,
    /** Nền màn xem (luôn đen) */
    val mediaBg: Color,
    /** Scrim sau Dialog, Sheet, bảng bên phải (mục 4.2). Cùng giá trị ở hai theme, không có trong odv-tokens.json. */
    val scrim: Color,
)

internal val LightColors = ODVColors(
    isDark = false,
    bg = Color(0xFFF4F5F7),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFECEEF2),
    line = Color(0xFFE1E4EA),
    lineStrong = Color(0xFF838B99),
    ink = Color(0xFF101217),
    inkMuted = Color(0xFF565E6D),
    inkFaint = Color(0xFF9AA1AD),
    volt = Color(0xFFC5F23A),
    voltPressed = Color(0xFFB0DC22),
    onVolt = Color(0xFF101217),
    voltSoft = Color(0xFFEEFAC8),
    voltText = Color(0xFF456100),
    kindVideo = Color(0xFFD6336A),
    kindVideoSoft = Color(0xFFFDE7EF),
    kindPhoto = Color(0xFF167CC9),
    kindPhotoSoft = Color(0xFFE2F1FC),
    kindPdf = Color(0xFFA86400),
    kindPdfSoft = Color(0xFFFDF0DC),
    danger = Color(0xFFC8242F),
    dangerSoft = Color(0xFFFDE8E9),
    warning = Color(0xFF935600),
    warningSoft = Color(0xFFFFF2D9),
    success = Color(0xFF12734B),
    successSoft = Color(0xFFE2F5EB),
    inverseSurface = Color(0xFF20242C),
    inverseInk = Color(0xFFF2F4F7),
    inverseAccent = Color(0xFFCCF55B),
    mediaBg = Color(0xFF000000),
    scrim = Color(0x99000000),
)

internal val DarkColors = ODVColors(
    isDark = true,
    bg = Color(0xFF0C0E12),
    surface = Color(0xFF15181E),
    surface2 = Color(0xFF1E2229),
    line = Color(0xFF2A2F38),
    lineStrong = Color(0xFF687183),
    ink = Color(0xFFF2F4F7),
    inkMuted = Color(0xFFA3ABB8),
    inkFaint = Color(0xFF5C6472),
    volt = Color(0xFFCCF55B),
    voltPressed = Color(0xFFDCFA86),
    onVolt = Color(0xFF101217),
    voltSoft = Color(0xFF222C0B),
    voltText = Color(0xFFCCF55B),
    kindVideo = Color(0xFFFF6D9A),
    kindVideoSoft = Color(0xFF3A1322),
    kindPhoto = Color(0xFF56C2FF),
    kindPhotoSoft = Color(0xFF0E2A3D),
    kindPdf = Color(0xFFFFB938),
    kindPdfSoft = Color(0xFF35260B),
    danger = Color(0xFFFF7276),
    dangerSoft = Color(0xFF3A1416),
    warning = Color(0xFFFFC24D),
    warningSoft = Color(0xFF33250A),
    success = Color(0xFF4CD99A),
    successSoft = Color(0xFF0F2E20),
    inverseSurface = Color(0xFFF2F4F7),
    inverseInk = Color(0xFF101217),
    inverseAccent = Color(0xFF456100),
    mediaBg = Color(0xFF000000),
    scrim = Color(0x99000000),
)

/** Nội suy từng màu giữa hai theme, dùng cho cross-fade khi đổi theme (mục 2.8). `isDark` lấy theo đích. */
internal fun ODVColors.lerp(stop: ODVColors, fraction: Float): ODVColors = ODVColors(
    isDark = stop.isDark,
    bg = lerpColor(bg, stop.bg, fraction),
    surface = lerpColor(surface, stop.surface, fraction),
    surface2 = lerpColor(surface2, stop.surface2, fraction),
    line = lerpColor(line, stop.line, fraction),
    lineStrong = lerpColor(lineStrong, stop.lineStrong, fraction),
    ink = lerpColor(ink, stop.ink, fraction),
    inkMuted = lerpColor(inkMuted, stop.inkMuted, fraction),
    inkFaint = lerpColor(inkFaint, stop.inkFaint, fraction),
    volt = lerpColor(volt, stop.volt, fraction),
    voltPressed = lerpColor(voltPressed, stop.voltPressed, fraction),
    onVolt = lerpColor(onVolt, stop.onVolt, fraction),
    voltSoft = lerpColor(voltSoft, stop.voltSoft, fraction),
    voltText = lerpColor(voltText, stop.voltText, fraction),
    kindVideo = lerpColor(kindVideo, stop.kindVideo, fraction),
    kindVideoSoft = lerpColor(kindVideoSoft, stop.kindVideoSoft, fraction),
    kindPhoto = lerpColor(kindPhoto, stop.kindPhoto, fraction),
    kindPhotoSoft = lerpColor(kindPhotoSoft, stop.kindPhotoSoft, fraction),
    kindPdf = lerpColor(kindPdf, stop.kindPdf, fraction),
    kindPdfSoft = lerpColor(kindPdfSoft, stop.kindPdfSoft, fraction),
    danger = lerpColor(danger, stop.danger, fraction),
    dangerSoft = lerpColor(dangerSoft, stop.dangerSoft, fraction),
    warning = lerpColor(warning, stop.warning, fraction),
    warningSoft = lerpColor(warningSoft, stop.warningSoft, fraction),
    success = lerpColor(success, stop.success, fraction),
    successSoft = lerpColor(successSoft, stop.successSoft, fraction),
    inverseSurface = lerpColor(inverseSurface, stop.inverseSurface, fraction),
    inverseInk = lerpColor(inverseInk, stop.inverseInk, fraction),
    inverseAccent = lerpColor(inverseAccent, stop.inverseAccent, fraction),
    mediaBg = lerpColor(mediaBg, stop.mediaBg, fraction),
    scrim = lerpColor(scrim, stop.scrim, fraction),
)
