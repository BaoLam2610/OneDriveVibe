package com.lambao.odv

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.lambao.odv.core.designsystem.component.LocalODVTopOverlay
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.debug.DebugTools
import com.lambao.odv.navigation.ODVNavDisplay

// AppCompatActivity thay ComponentActivity của template: AppCompatDelegate.setApplicationLocales() chỉ áp dụng ngay
// (không cần khởi động lại app) với AppCompatActivity trên Android 12 trở xuống (ADR-0011, CD-10).
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ODVApp()
        }
    }
}

@Composable
fun ODVApp() {
    ODVTheme {
        // Lớp phủ nổi của công cụ debug: Dialog/BottomSheet đọc LocalODVTopOverlay để vẽ lại nút bọ phía trên chúng. Bản
        // release cung cấp null nên không có gì được vẽ (ADR-0012).
        CompositionLocalProvider(LocalODVTopOverlay provides DebugTools.topOverlay) {
            Box(Modifier.fillMaxSize()) {
                ODVNavDisplay()
                // Nút bọ nổi chỉ có ở bản debug; bản release là hàm rỗng.
                DebugTools.Overlay()
            }
        }
    }
}
