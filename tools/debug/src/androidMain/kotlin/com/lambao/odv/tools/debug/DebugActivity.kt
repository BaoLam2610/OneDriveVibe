package com.lambao.odv.tools.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Màn Debug (ADR-0012): bốn tab Log API, Log local, Lưu trữ, Khác. Mở từ nút bọ nổi; chỉ có trong bản debug.
 *
 * Không tự đặt FLAG_SECURE, nhưng **theo cài đặt của người dùng** như mọi màn khác: khi bật Cài đặt › Bảo mật › Bảo vệ màn hình
 * (`ODVSecureWindowPolicy.appWide`) thì màn này cũng chặn chụp màn hình, vì log API trong đây chứa secret. Debug không có trạng thái riêng
 * (ADR-0020): tab Khác có công tắc Bảo vệ màn hình điều khiển chính cài đặt đó; muốn chụp/quay màn hình Debug để báo lỗi thì tắt nó. Dữ liệu log API hiển thị đầy đủ,
 * chưa che (che bằng công tắc ở tab Khác, ADR-0013); module chỉ có trong bản debug. Nên chỉ dùng với secret riêng cho việc phát
 * triển và không chia sẻ ảnh chụp màn hình Debug ra ngoài.
 */
class DebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        current = this
        setContent {
            ODVTheme {
                ODVDebugScreen(onBack = ::finish)
            }
        }
    }

    override fun onDestroy() {
        if (current === this) current = null
        super.onDestroy()
    }

    companion object {
        // Chỉ để đóng màn Debug khi app khóa (ADR-0013, ADR-0014). Xóa trong onDestroy nên không giữ Activity.
        private var current: DebugActivity? = null

        /**
         * App vừa khóa (CH-03): chỉ đóng màn Debug nếu đang mở. Log API và log local được giữ nguyên (ADR-0017, thay phần "xóa log
         * API khi khóa" của ADR-0013/0014) để còn xem lại sau khi mở khóa.
         */
        fun onAppLocked() {
            current?.finish()
        }
    }
}
