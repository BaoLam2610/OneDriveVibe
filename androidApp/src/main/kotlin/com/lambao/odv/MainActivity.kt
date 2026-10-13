package com.lambao.odv

import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.lambao.odv.core.designsystem.component.LocalODVTopOverlay
import com.lambao.odv.core.designsystem.component.ODVSecureWindowPolicy
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.designsystem.theme.ODVThemeMode
import com.lambao.odv.core.domain.model.ThemeMode
import com.lambao.odv.core.domain.settings.SecuritySettings
import com.lambao.odv.core.domain.usecase.security.ObserveProtectionUseCase
import com.lambao.odv.core.domain.usecase.settings.ObserveThemeModeUseCase
import com.lambao.odv.debug.DebugTools
import com.lambao.odv.navigation.ODVNavDisplay
import com.lambao.odv.ui.theme.ThemeBootCache
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject

// AppCompatActivity thay ComponentActivity của template: AppCompatDelegate.setApplicationLocales() chỉ áp dụng ngay
// (không cần khởi động lại app) với AppCompatActivity trên Android 12 trở xuống (ADR-0011, CD-10).
class MainActivity : AppCompatActivity() {
    private val observeProtection: ObserveProtectionUseCase by inject()
    private val observeThemeMode: ObserveThemeModeUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Giao diện app đã chọn, đọc ĐỒNG BỘ từ bản sao khởi động (ThemeBootCache) để khung đầu tiên đã đúng màu: ép nền cửa sổ XML (trước
        // super.onCreate) và làm giá trị khởi đầu của Compose. Nếu đợi DataStore thì máy tối + app sáng sẽ thấy nền tối rồi mới sáng.
        val bootTheme = ThemeBootCache.read(this)
        ThemeBootCache.windowTheme(bootTheme)?.let { setTheme(it) }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Đăng ký Window để "Bảo vệ màn hình" toàn app (ODVSecureWindowPolicy.appWide) áp dụng được cho Activity này ở mọi bản dựng; công cụ
        // debug chỉ đăng ký thay cho các Activity khác.
        ODVSecureWindowPolicy.track(window)

        // Bảo mật BẬT thì ẩn ảnh chụp của app ở danh sách app gần đây (CH-05, ADR-0014). Phải áp ngay từ lúc bật PIN, không đợi
        // tới lúc khóa: ảnh được lấy khi app rời màn hình, trước khi ProcessLifecycleOwner kịp báo ON_STOP. Android 13+ có API
        // riêng chỉ ẩn ảnh này mà vẫn cho chụp màn hình trong app; bản thấp hơn dùng FLAG_SECURE toàn cửa sổ (xem ODVApp).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lifecycleScope.launch {
                observeProtection().collect { enabled -> setRecentsScreenshotEnabled(!enabled) }
            }
        }

        // Giữ bản sao khởi động khớp DataStore (cũng là nơi Ngắt kết nối xóa về "Theo hệ thống", CD-05) và cập nhật theme splash hệ thống.
        lifecycleScope.launch {
            observeThemeMode().collect { mode ->
                ThemeBootCache.write(this@MainActivity, mode)
                applySplashTheme(mode)
            }
        }

        setContent {
            ODVApp(initialThemeMode = bootTheme)
        }
    }

    // Android 13+: hệ thống nhớ theme splash này cho lần khởi động kế tiếp, nên splash hệ thống có nền đúng giao diện của app từ lần sau.
    // Theo hệ thống thì bỏ ghi đè (ID_NULL) để quay về Theme.OneDriveVibe vốn theo máy. Android 12/12L không có API này.
    private fun applySplashTheme(mode: ThemeMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            splashScreen.setSplashScreenTheme(ThemeBootCache.windowTheme(mode) ?: Resources.ID_NULL)
        }
    }
}

@Composable
fun ODVApp(initialThemeMode: ThemeMode = ThemeMode.System) {
    // Giao diện Sáng/Tối người dùng chọn ở Cài đặt (Lát 7). Khởi đầu bằng giá trị đọc đồng bộ từ ThemeBootCache (không phải "Theo hệ
    // thống") để khung đầu tiên đã đúng màu, rồi DataStore phát giá trị thật và đồng bộ lại. Được giữ qua lần tạo lại Activity (rememberSaveable).
    val observeThemeMode = koinInject<ObserveThemeModeUseCase>()
    var themeMode by rememberSaveable { mutableStateOf(initialThemeMode) }
    LaunchedEffect(observeThemeMode) { observeThemeMode().collect { themeMode = it } }
    ODVTheme(mode = themeMode.toDesignSystem()) {
        val observeProtection = koinInject<ObserveProtectionUseCase>()
        val isProtected by observeProtection().collectAsState()
        // Android 12 trở xuống không có API ẩn riêng ảnh ở danh sách app gần đây nên dùng FLAG_SECURE toàn cửa sổ khi bảo mật
        // BẬT (đánh đổi: mất chụp màn hình trong app, chấp nhận theo ADR-0014). Android 13+ xử lý ở MainActivity.onCreate.
        // "Bảo vệ màn hình" (Cài đặt › Bảo mật) bật thì FLAG_SECURE toàn app, bất kể PIN. Chưa đọc xong DataStore thì coi như bật (phía an
        // toàn): người dùng đã tắt chỉ mất khả năng chụp màn hình trong vài mili giây đầu, còn người đã bật không có khoảng hở nào.
        val securitySettings = koinInject<SecuritySettings>()
        val screenProtection by remember(securitySettings) { securitySettings.screenProtection }.collectAsState(initial = true)
        // Đặt ở cấp process (ODVSecureWindowPolicy.appWide) thay vì ODVSecureWindow của riêng cửa sổ này: nhờ vậy mọi Activity khác,
        // gồm cả DebugActivity, cũng theo cùng một cài đặt. Ghi đè của công cụ debug (Luôn bật/Luôn tắt) vẫn cao hơn.
        val appWideSecure = shouldSecureWholeWindow(screenProtection, isProtected)
        SideEffect { ODVSecureWindowPolicy.setAppWide(appWideSecure) }
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

private fun ThemeMode.toDesignSystem(): ODVThemeMode = when (this) {
    ThemeMode.System -> ODVThemeMode.System
    ThemeMode.Light -> ODVThemeMode.Light
    ThemeMode.Dark -> ODVThemeMode.Dark
}

/**
 * Có đặt FLAG_SECURE cho cả cửa sổ không: người dùng bật "Bảo vệ màn hình" ([screenProtection]), hoặc đang bật PIN trên Android 12 trở
 * xuống ([isProtected], không có API ẩn riêng ảnh ở danh sách app gần đây, ADR-0014). Dùng chung cho `ODVApp` và công tắc ở màn Debug để
 * hai nơi luôn cùng một quy tắc.
 */
internal fun shouldSecureWholeWindow(screenProtection: Boolean, isProtected: Boolean): Boolean =
    screenProtection || (isProtected && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)

