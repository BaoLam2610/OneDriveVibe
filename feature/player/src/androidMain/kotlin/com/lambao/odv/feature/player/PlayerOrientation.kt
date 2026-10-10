package com.lambao.odv.feature.player

import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Hướng màn hình của màn xem video (VD-07, VD-19). Toàn app khóa Dọc ở manifest; đây là nơi duy nhất cho phép xoay ngang. Hướng
 * **chỉ đổi bằng nút xoay**, không theo cảm biến: vào màn là khóa Dọc, [landscape] true thì khóa Ngang (cả hai chiều ngang, vì quay
 * máy 180° ở hướng ngang vẫn là ngang). Chuyển video trước/sau giữ nguyên [landscape] vì trạng thái này nằm ngoài vòng đời của từng
 * video. Rời màn thì về Dọc và **giữ Dọc** (VD-19: bấm Back một lần, màn hình trở về hướng dọc); không trả `UNSPECIFIED` cho hệ thống
 * vì đó là ghi đè khóa dọc của manifest và app sẽ xoay tự do ở mọi màn.
 *
 * Activity khai `configChanges` (AndroidManifest) nên đổi hướng không tạo lại Activity, không làm gián đoạn phát.
 */
@Composable
internal fun PlayerOrientationEffect(landscape: Boolean) {
    val view = LocalView.current
    val activity = view.context.findHostActivity() ?: return

    DisposableEffect(landscape) {
        activity.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { }
    }
    DisposableEffect(activity) {
        onDispose {
            // Về Dọc và ở lại Dọc: toàn app chỉ hiển thị dọc, máy đang nằm ngang cũng phải trở về dọc ở Danh sách.
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}
