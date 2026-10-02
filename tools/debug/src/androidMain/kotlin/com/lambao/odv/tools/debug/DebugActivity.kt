package com.lambao.odv.tools.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lambao.odv.core.designsystem.component.ODVSecureWindow
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Màn Debug (ADR-0012): ba tab Log API, Log local, Lưu trữ. Mở từ nút bọ nổi; chỉ có trong bản debug.
 * Chặn chụp màn hình vì log có UPN, đường dẫn tệp và dữ liệu local.
 */
class DebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ODVTheme {
                ODVSecureWindow()
                ODVDebugScreen(onBack = ::finish)
            }
        }
    }
}
