package com.lambao.odv.core.media

import android.content.Context
import com.lambao.odv.core.domain.hook.AccountDataCleaner
import com.lambao.odv.core.domain.hook.CacheStore
import com.lambao.odv.core.domain.hook.ConnectionResetter
import com.lambao.odv.core.domain.hook.NextVideoPreloadSwitch
import org.koin.dsl.binds
import org.koin.dsl.module

/**
 * Binding Koin của `:core:media` cần Android (Media3, ADR-0025). Ghép ở MainApplication.
 * [VideoCache] cũng là [ConnectionResetter] nên `DisconnectUseCase` (getAll) tự thấy và xóa cache video khi ngắt kết nối (CD-05), và
 * là [CacheStore] nên Cài đặt đo, xóa và dọn theo giới hạn (CD-07). Chuyển từ `androidPlayerModule` của `:feature:player`.
 */
val androidMediaModule = module {
    // Cũng là AccountDataCleaner: xóa tài khoản khỏi app thì xóa các đoạn video đã giữ của tài khoản đó (TA-08, ADR-0030).
    single { VideoCache(get<Context>(), get(), get()) } binds arrayOf(ConnectionResetter::class, CacheStore::class, AccountDataCleaner::class)
    // Công tắc chuẩn bị sẵn video +1 trong RAM (ADR-0028): mặc định luôn bật; bản debug ghi đè bằng công tắc ở màn Debug (DebugTools.koinModules).
    single<NextVideoPreloadSwitch> { AlwaysEnabledNextVideoPreload }
    // Link phát dùng chung của tab Short, một yêu cầu cho một id (ADR-0029); cũng là ConnectionResetter nên Ngắt kết nối xóa link (CD-05).
    single { SharedStreamUrls(get(), get()) } binds arrayOf(ConnectionResetter::class)
    single { ExoPlayerFactory(get<Context>(), get(), get(), get(), get(), get()) }
    // Tải trước đầu video kế tiếp của tab Short vào cache video chung (SV-13, ADR-0026).
    single { VideoPreloader(get(), get(), get()) }
    // Khung hình đầu thật làm poster khi vuốt (tab Short, 2026-10-11).
    single { FirstFrameProvider(get(), get()) }
}
