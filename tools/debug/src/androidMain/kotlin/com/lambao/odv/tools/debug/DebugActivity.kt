package com.lambao.odv.tools.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Màn Debug (ADR-0012): bốn tab Log API, Log local, Lưu trữ, Khác. Mở từ nút bọ nổi; chỉ có trong bản debug.
 *
 * Cố ý KHÔNG đặt FLAG_SECURE: đây là công cụ cho người phát triển, cần chụp/quay màn hình để báo lỗi. Đổi lại, dữ liệu
 * log API hiển thị đầy đủ, chưa che (có thể che bằng công tắc ở tab Khác, ADR-0013); module chỉ có trong bản debug. Nên chỉ
 * dùng với secret riêng cho việc phát triển và không chia sẻ ảnh chụp màn hình Debug ra ngoài.
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

        /** App vừa khóa (CH-03): log API chứa token và Client Secret đầy đủ nên xóa và đóng màn Debug nếu đang mở. */
        fun onAppLocked() {
            ApiTrafficStore.clear()
            current?.finish()
        }
    }
}
