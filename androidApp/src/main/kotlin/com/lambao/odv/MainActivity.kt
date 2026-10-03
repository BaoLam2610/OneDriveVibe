package com.lambao.odv

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.lambao.odv.core.designsystem.component.LocalODVTopOverlay
import com.lambao.odv.core.designsystem.component.ODVSecureWindow
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.domain.repository.SecurityRepository
import com.lambao.odv.debug.DebugTools
import com.lambao.odv.navigation.ODVNavDisplay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject

// AppCompatActivity thay ComponentActivity của template: AppCompatDelegate.setApplicationLocales() chỉ áp dụng ngay
// (không cần khởi động lại app) với AppCompatActivity trên Android 12 trở xuống (ADR-0011, CD-10).
class MainActivity : AppCompatActivity() {
    private val security: SecurityRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Bảo mật BẬT thì ẩn ảnh chụp của app ở danh sách app gần đây (CH-05, ADR-0014). Phải áp ngay từ lúc bật PIN, không đợi
        // tới lúc khóa: ảnh được lấy khi app rời màn hình, trước khi ProcessLifecycleOwner kịp báo ON_STOP. Android 13+ có API
        // riêng chỉ ẩn ảnh này mà vẫn cho chụp màn hình trong app; bản thấp hơn dùng FLAG_SECURE toàn cửa sổ (xem ODVApp).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lifecycleScope.launch {
                security.isProtected.collect { enabled -> setRecentsScreenshotEnabled(!enabled) }
            }
        }

        setContent {
            ODVApp()
        }
    }
}

@Composable
fun ODVApp() {
    ODVTheme {
        val security = koinInject<SecurityRepository>()
        val isProtected by security.isProtected.collectAsState()
        // Android 12 trở xuống không có API ẩn riêng ảnh ở danh sách app gần đây nên dùng FLAG_SECURE toàn cửa sổ khi bảo mật
        // BẬT (đánh đổi: mất chụp màn hình trong app, chấp nhận theo ADR-0014). Android 13+ xử lý ở MainActivity.onCreate.
        ODVSecureWindow(enabled = isProtected && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
        // Nút bọ debug luôn hiện ở mọi nơi trong bản debug, kể cả màn Khóa, lúc chưa biết trạng thái khóa và trên Dialog
        // (yêu cầu của người dùng, thay cho việc ẩn khi khóa ở ADR-0014; rủi ro ghi ở ADR-0013). Bản release: `topOverlay` là
        // null và `Overlay()` là hàm rỗng nên không có gì được vẽ (ADR-0012).
        // Dialog/BottomSheet đọc LocalODVTopOverlay để vẽ lại nút bọ phía trên chúng.
        CompositionLocalProvider(LocalODVTopOverlay provides DebugTools.topOverlay) {
            Box(Modifier.fillMaxSize()) {
                ODVNavDisplay()
                DebugTools.Overlay()
            }
        }
    }
}
