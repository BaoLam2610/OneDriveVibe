package com.lambao.odv.debug

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.Composable
import com.lambao.odv.MainActivity
import com.lambao.odv.core.domain.hook.PreferencesInspector
import com.lambao.odv.core.domain.hook.ThumbnailCache
import com.lambao.odv.core.domain.hook.ThumbnailQuality
import com.lambao.odv.core.domain.usecase.DisconnectUseCase
import com.lambao.odv.gallery.FoundationsGalleryActivity
import com.lambao.odv.tools.debug.DebugAction
import com.lambao.odv.tools.debug.DebugActions
import com.lambao.odv.tools.debug.DebugActivity
import com.lambao.odv.tools.debug.DebugHooks
import com.lambao.odv.tools.debug.DebugLogging
import com.lambao.odv.tools.debug.DebugSettings
import com.lambao.odv.tools.debug.ODVDebugBugButton
import com.lambao.odv.tools.debug.debugModule
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

/**
 * Cổng vào công cụ debug (ADR-0012). Bản debug nối `:tools:debug`; bản release (src/release) cùng API nhưng rỗng, nên
 * code ở src/main không bao giờ nhắc tới `:tools:debug` và APK release không chứa nó.
 */
object DebugTools {
    /** Module Koin cài bộ ghi lưu lượng API. */
    val koinModules: List<Module> = listOf(
        debugModule,
        // Đứng sau androidDataModule trong MainApplication nên ghi đè ThumbnailQuality cố định 100% bằng giá trị chỉnh ở màn
        // Debug (Koin 4 cho ghi đè mặc định). Bản release không có module này nên luôn 100%.
        module { single<ThumbnailQuality> { DebugThumbnailQuality } },
    )

    private object DebugThumbnailQuality : ThumbnailQuality {
        override fun scalePercent(): Int = DebugSettings.thumbnailScalePercent.value
    }

    /**
     * Gọi trước `startKoin`: ghi log ra Logcat và vào màn Debug, nạp cài đặt debug (FLAG_SECURE toàn app, che log API),
     * đăng ký công cụ riêng của app. Chỉ có MỘT icon launcher: Foundations gallery mở từ tab "Khác" của màn Debug.
     */
    fun install(application: Application) {
        DebugLogging.install()
        DebugSettings.install(application)
        // Tab Lưu trữ: "Xóa dữ liệu local" = Ngắt kết nối (DisconnectUseCase xóa config, khóa Keystore, PIN, sinh trắc học, token,
        // dữ liệu các lát sau) rồi khởi động lại app ở một task mới để back stack và trạng thái trong bộ nhớ về ban đầu: Splash
        // thấy chưa có config nên sang màn Kết nối. Koin chỉ có sau startKoin nên lấy lúc bấm, không lấy ở đây.
        DebugHooks.clearLocalData = {
            KoinPlatform.getKoin().get<DisconnectUseCase>().invoke()
            application.startActivity(
                Intent(application, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
        }
        // Tab Khác, nhóm Thumbnail: xóa cache ảnh thu nhỏ. Koin chỉ có sau startKoin nên lấy lúc bấm.
        DebugHooks.clearThumbnailCache = { KoinPlatform.getKoin().get<ThumbnailCache>().clear() }
        // Tab Lưu trữ, mục DataStore: xem khóa và giá trị (chỉ đọc).
        DebugHooks.dumpPreferences = { KoinPlatform.getKoin().get<PreferencesInspector>().dump() }
        DebugActions.register(
            DebugAction(
                title = "Foundations gallery",
                description = "Token, icon và component của design system để so với canvas thiết kế",
                onClick = { context -> context.startActivity(Intent(context, FoundationsGalleryActivity::class.java)) },
            ),
        )
    }

    /** Nút bọ nổi vẽ trong Activity, đặt ở gốc của app. */
    @Composable
    fun Overlay() = ODVDebugBugButton(asWindow = false)

    /**
     * Cung cấp cho `LocalODVTopOverlay`: Dialog và BottomSheet gọi nó để vẽ lại nút bọ trong cửa sổ riêng nằm trên
     * chúng (ADR-0012). Null ở bản release.
     */
    val topOverlay: (@Composable (asWindow: Boolean) -> Unit)? = { asWindow -> ODVDebugBugButton(asWindow = asWindow) }

    /**
     * App vừa khóa (ADR-0014): đóng màn Debug nếu đang mở. Log API và log local được giữ (ADR-0017). Bản release là hàm rỗng.
     */
    fun onAppLocked() = DebugActivity.onAppLocked()
}
