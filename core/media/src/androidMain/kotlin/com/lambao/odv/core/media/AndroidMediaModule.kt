package com.lambao.odv.core.media

import android.content.Context
import com.lambao.odv.core.domain.hook.CacheStore
import com.lambao.odv.core.domain.hook.ConnectionResetter
import org.koin.dsl.binds
import org.koin.dsl.module

/**
 * Binding Koin của `:core:media` cần Android (Media3, ADR-0025). Ghép ở MainApplication.
 * [VideoCache] cũng là [ConnectionResetter] nên `DisconnectUseCase` (getAll) tự thấy và xóa cache video khi ngắt kết nối (CD-05), và
 * là [CacheStore] nên Cài đặt đo, xóa và dọn theo giới hạn (CD-07). Chuyển từ `androidPlayerModule` của `:feature:player`.
 */
val androidMediaModule = module {
    single { VideoCache(get<Context>(), get(), get()) } binds arrayOf(ConnectionResetter::class, CacheStore::class)
    single { ExoPlayerFactory(get<Context>(), get(), get()) }
    // Tải trước đầu video kế tiếp của tab Short vào cache video chung (SV-13, ADR-0026).
    single { VideoPreloader(get(), get(), get()) }
}
