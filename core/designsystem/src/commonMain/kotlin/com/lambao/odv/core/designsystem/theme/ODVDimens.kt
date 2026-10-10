// Sinh từ .claude/docs/odv-tokens.json. Token đổi thì sửa file JSON trước, rồi cập nhật file này cho khớp.
package com.lambao.odv.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Lưới 4dp (mục 2.4). `s4` = lề màn hình. */
object ODVSpacing {
    /** Khe ô Thư viện */
    val half = 2.dp
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    /** Lề màn hình */
    val s4 = 16.dp
    val s6 = 24.dp
    val s8 = 32.dp
}

/** Bo góc (mục 2.5). */
object ODVRadius {
    /** Ô Thư viện, badge */
    val xs = 6.dp
    /** Ô nhập, Banner, Snackbar, thumbnail hàng, thẻ lưới */
    val sm = 10.dp
    /** Thẻ, nhóm Cài đặt, phím PIN */
    val md = 14.dp
    /** Dialog, Sheet, bảng bên phải */
    val lg = 22.dp
    /** Button, Chip, Tabs, Switch, tiến độ */
    val full = 9999.dp
}

/** Hình bo góc dựng sẵn từ [ODVRadius]. `full` là hình viên thuốc (Button, Chip, Tabs, Switch). */
object ODVShapes {
    val xs = RoundedCornerShape(ODVRadius.xs)
    val sm = RoundedCornerShape(ODVRadius.sm)
    val md = RoundedCornerShape(ODVRadius.md)
    val lg = RoundedCornerShape(ODVRadius.lg)
    val full = RoundedCornerShape(percent = 50)
}

/** Kích thước cố định (mục 2.6). */
object ODVSize {
    /** Vùng chạm tối thiểu */
    val tapTarget = 48.dp
    val iconMd = 24.dp
    val iconSm = 18.dp
    /** Thumbnail FileRow */
    val thumbRow = 56.dp
    val appBar = 56.dp
    /** Nút phát giữa */
    val playButton = 72.dp
    val button = 48.dp
    val buttonSm = 36.dp
    /** Khung ô nhập */
    val field = 52.dp
    val chip = 36.dp
    val tabItem = 40.dp
    /** Chiều cao thanh điều hướng đáy, chưa tính system inset (mục 4.7, Lát 8) */
    val navBar = 64.dp
    /** Viên chọn mục thanh điều hướng đáy */
    val navIndicatorWidth = 56.dp
    val navIndicatorHeight = 32.dp
    /** Chấm nhắc trên mục Cài đặt (DH-07) */
    val navDot = 8.dp
    val switchWidth = 52.dp
    val switchHeight = 32.dp
    val radio = 22.dp
    val settingsRowMin = 56.dp
    val infoRowMin = 48.dp
    val fileRowMin = 72.dp
    val snackbarMin = 48.dp
    val dialogMaxWidth = 360.dp
    val sheetHandleWidth = 36.dp
    val sheetHandleHeight = 4.dp
    val pinDot = 16.dp
    val pinDotGap = 16.dp
    val keypadKeyHeight = 56.dp
    val keypadGap = 12.dp
    val keypadWidth = 358.dp
    val folderCardThumb = 104.dp
    val fileCardThumb = 120.dp
    val continueCardWidth = 232.dp
    val continueCardThumb = 130.dp
    /** Thanh xem dở trên thumbnail */
    val mediaProgress = 4.dp
    val fastScrollerTouch = 28.dp
    val fastScrollerHandleWidth = 12.dp
    val fastScrollerHandleHeight = 36.dp
    val emptyIconWell = 72.dp
    val loaderRing = 72.dp
    val seekTouch = 24.dp
    val seekTrack = 4.dp
    val seekThumb = 16.dp
    /** Khi ẩn điều khiển video */
    val miniProgress = 3.dp
    val hudWidth = 56.dp
    val hudHeight = 200.dp
    val lockHoldRing = 72.dp
    val lockHoldButton = 52.dp
    val countdownRing = 56.dp
    /** Bảng bên phải ở hướng ngang */
    val sidePanelWidth = 360.dp
}
