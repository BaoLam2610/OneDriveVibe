package com.lambao.odv.core.designsystem.component

import androidx.compose.runtime.Composable

/**
 * Chặn chụp màn hình và ẩn nội dung trong danh sách app gần đây khi [enabled] (KN-10, KH-04, CH-05, mục 1.12).
 * Dùng ở màn Kết nối, Khóa, nhập PIN và Cài đặt (nhóm Bảo mật, Kết nối). Android: đặt `FLAG_SECURE` cho cửa sổ của Activity
 * khi vào composition và gỡ khi không còn màn nào yêu cầu (có đếm tham chiếu, nên chuyển màn có animation giữa hai màn bảo mật không
 * làm mất cờ). Đặt ở gốc của từng màn cần chặn.
 */
@Composable
expect fun ODVSecureWindow(enabled: Boolean = true)
