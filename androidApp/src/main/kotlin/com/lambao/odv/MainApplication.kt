package com.lambao.odv

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.lambao.odv.core.data.dataModule
import com.lambao.odv.core.domain.repository.SecurityRepository
import com.lambao.odv.core.network.networkModule
import com.lambao.odv.core.security.securityModule
import com.lambao.odv.debug.DebugTools
import com.lambao.odv.di.appModule
import com.lambao.odv.feature.auth.authModule
import com.lambao.odv.feature.browser.browserModule
import com.lambao.odv.security.AppLockController
import com.lambao.odv.security.CurrentActivityHolder
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Trước startKoin để log khởi động cũng vào màn Debug. Release gỡ hết writer (ADR-0012, CH-06).
        DebugTools.install(this)
        // ADR-0004: khởi động Koin một lần cho cả app. Thêm module của từng tầng/feature vào `modules(...)`.
        startKoin {
            // Chỉ log lỗi: Koin không được in giá trị đã tiêm (có thể là config hay token, CH-06).
            androidLogger(Level.ERROR)
            androidContext(this@MainApplication)
            modules(listOf(appModule, securityModule, networkModule, dataModule, authModule, browserModule) + DebugTools.koinModules)
        }
        // CH-03, ADR-0014: tự khóa khi cả app xuống nền. Giữ tham chiếu mạnh vì Lifecycle chỉ giữ yếu observer.
        ProcessLifecycleOwner.get().lifecycle.addObserver(lockController)
        // Để BiometricPrompt biết hiện trên Activity nào (ADR-0014).
        registerActivityLifecycleCallbacks(activityHolder)
    }

    private val security: SecurityRepository by inject()

    private val activityHolder: CurrentActivityHolder by inject()

    private val lockController by lazy { AppLockController(security) { DebugTools.onAppLocked() } }
}
