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
import com.lambao.odv.core.domain.model.LockState
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
        // Khi app đang khóa (ADR-0014) không vẽ nút bọ: nó mở được màn Debug mà không cần PIN, và màn Debug có thể
        // chứa token. Cũng không cung cấp lớp phủ cho Dialog trên màn Khóa (Quên mã PIN).
        val security = koinInject<SecurityRepository>()
        // collectAsState (không gắn lifecycle): xem ODVNavDisplay, tránh lộ vài khung hình cũ khi mở lại sau lúc bị khóa.
        val lockState by security.lockState.collectAsState()
        val isProtected by security.isProtected.collectAsState()
        // Chỉ khi đã biết chắc là mở khóa: lúc Unknown (khởi động nguội) chưa biết app có khóa hay không nên chưa hiện.
        val showDebug = lockState == LockState.Unlocked
        // Android 12 trở xuống không có API ẩn riêng ảnh ở danh sách app gần đây nên dùng FLAG_SECURE toàn cửa sổ khi bảo mật
        // BẬT (đánh đổi: mất chụp màn hình trong app, chấp nhận theo ADR-0014). Android 13+ xử lý ở MainActivity.onCreate.
        ODVSecureWindow(enabled = isProtected && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
        // Lớp phủ nổi của công cụ debug: Dialog/BottomSheet đọc LocalODVTopOverlay để vẽ lại nút bọ phía trên chúng. Bản
        // release cung cấp null nên không có gì được vẽ (ADR-0012).
        CompositionLocalProvider(LocalODVTopOverlay provides if (showDebug) DebugTools.topOverlay else null) {
            Box(Modifier.fillMaxSize()) {
                ODVNavDisplay()
                // Nút bọ nổi chỉ có ở bản debug; bản release là hàm rỗng.
                if (showDebug) DebugTools.Overlay()
            }
        }
    }
}
