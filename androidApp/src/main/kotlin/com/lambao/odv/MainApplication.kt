package com.lambao.odv

import android.app.Application
import com.lambao.odv.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // ADR-0004: khởi động Koin một lần cho cả app. Thêm module của từng tầng/feature vào `modules(...)`.
        startKoin {
            // Chỉ log lỗi: Koin không được in giá trị đã tiêm (có thể là config hay token, CH-06).
            androidLogger(Level.ERROR)
            androidContext(this@MainApplication)
            modules(appModule)
        }
    }
}
