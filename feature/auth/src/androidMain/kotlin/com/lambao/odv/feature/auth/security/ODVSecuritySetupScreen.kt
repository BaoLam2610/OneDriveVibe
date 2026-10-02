package com.lambao.odv.feature.auth.security

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lambao.odv.core.designsystem.component.ODVSecureWindow

/**
 * Màn Thiết lập bảo mật (BM-01 → BM-08), mở từ hộp thoại KN-13. Config đã được lưu ở chế độ thiết bị trước khi tới
 * đây (KN-08), nên màn này không còn giữ hay lưu config. [onBack] quay lại hộp thoại KN-13 (BM-08).
 * Chặn chụp màn hình và ẩn ở danh sách app gần đây (CH-05).
 *
 * Chưa có ViewModel: bước đặt PIN, bàn phím số, mã hóa lại config (BM-02, BM-04 → BM-07) làm ở Lát 2.
 */
@Composable
fun ODVSecuritySetupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ODVSecureWindow()
    ODVSecuritySetupContent(onBack = onBack, modifier = modifier)
}
