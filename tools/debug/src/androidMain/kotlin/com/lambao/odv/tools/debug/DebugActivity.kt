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
 * hiển thị đã được làm sạch ở nguồn (không Authorization, không token, không body endpoint token, che downloadUrl) và
 * module chỉ có trong bản debug.
 */
class DebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ODVTheme {
                ODVDebugScreen(onBack = ::finish)
            }
        }
    }
}
