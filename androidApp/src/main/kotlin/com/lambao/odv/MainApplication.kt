package com.lambao.odv

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.lambao.odv.core.data.coreModules
import com.lambao.odv.core.domain.settings.SecuritySettings
import com.lambao.odv.core.domain.usecase.security.LockAppUseCase
import com.lambao.odv.core.domain.usecase.security.ObserveLockStateUseCase
import com.lambao.odv.core.domain.usecase.sync.SyncOnForegroundUseCase
import com.lambao.odv.debug.DebugTools
import com.lambao.odv.di.appModule
import com.lambao.odv.feature.auth.authModule
import com.lambao.odv.feature.browser.browserModule
import com.lambao.odv.feature.imageviewer.imageViewerModule
import com.lambao.odv.feature.library.libraryModule
import com.lambao.odv.feature.player.androidPlayerModule
import com.lambao.odv.feature.player.playerModule
import com.lambao.odv.feature.settings.settingsModule
import com.lambao.odv.security.AppLockController
import com.lambao.odv.security.CurrentActivityHolder
import com.lambao.odv.sync.ForegroundSyncObserver
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

// SingletonImageLoader.Factory: AsyncImage của Coil lấy ImageLoader (Fetcher thumbnail + cache đĩa của :core:data) từ Koin.
class MainApplication : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader

    private val imageLoader: ImageLoader by inject()

    override fun onCreate() {
        super.onCreate()
        // Trước startKoin để log khởi động cũng vào màn Debug. Release gỡ hết writer (ADR-0012, CH-06).
        DebugTools.install(this)
        // ADR-0004: khởi động Koin một lần cho cả app. Thêm module của từng tầng/feature vào `modules(...)`.
        startKoin {
            // Chỉ log lỗi: Koin không được in giá trị đã tiêm (có thể là config hay token, CH-06).
            androidLogger(Level.ERROR)
            androidContext(this@MainApplication)
            modules(
                // Debug đứng cuối để ghi đè binding của bản phát hành (vd. ThumbnailQuality).
                listOf(appModule) + coreModules + listOf(
                    authModule, browserModule, libraryModule, imageViewerModule, playerModule, androidPlayerModule, settingsModule,
                ) + DebugTools.koinModules,
            )
        }
        // CH-03, ADR-0014: tự khóa khi cả app xuống nền. Giữ tham chiếu mạnh vì Lifecycle chỉ giữ yếu observer.
        ProcessLifecycleOwner.get().lifecycle.addObserver(lockController)
        // DS-04: vào foreground thì đồng bộ nếu quá hạn (chế độ không PIN không có bước mở khóa nào đánh thức đồng bộ).
        ProcessLifecycleOwner.get().lifecycle.addObserver(foregroundSync)
        // Để BiometricPrompt biết hiện trên Activity nào (ADR-0014).
        registerActivityLifecycleCallbacks(activityHolder)
    }

    private val observeLockState: ObserveLockStateUseCase by inject()

    private val lockApp: LockAppUseCase by inject()

    private val activityHolder: CurrentActivityHolder by inject()

    private val syncOnForeground: SyncOnForegroundUseCase by inject()

    // Giữ tham chiếu mạnh (Lifecycle chỉ giữ yếu observer), như lockController.
    private val foregroundSync by lazy { ForegroundSyncObserver(syncOnForeground) }

    private val securitySettings: SecuritySettings by inject()

    private val lockController by lazy { AppLockController(observeLockState, lockApp, securitySettings) { DebugTools.onAppLocked() } }
}
