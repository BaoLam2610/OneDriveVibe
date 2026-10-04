package com.lambao.odv.feature.player

import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Hướng màn hình của màn xem video (VD-07, VD-19). Hướng **chỉ đổi bằng nút xoay**, không theo cảm biến: vào màn là khóa Dọc,
 * [landscape] true thì khóa Ngang (cả hai chiều ngang, vì quay máy 180° ở hướng ngang vẫn là ngang). Chuyển video trước/sau giữ
 * nguyên [landscape] vì trạng thái này nằm ngoài vòng đời của từng video. Rời màn thì về Dọc rồi trả cho hệ thống (VD-19: bấm
 * Back một lần, màn hình trở về hướng dọc).
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
            // Về Dọc ngay rồi mới trả quyền cho hệ thống, nếu không máy đang nằm ngang sẽ giữ ngang khi về Danh sách.
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            view.post { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }
}
