package com.lambao.odv.feature.player

import android.content.Context
import com.lambao.odv.core.domain.hook.ConnectionResetter
import org.koin.dsl.binds
import org.koin.dsl.module

/**
 * Binding Koin của `:feature:player` cần Android (Media3). Ghép ở MainApplication cùng [playerModule].
 * [VideoCache] cũng là [ConnectionResetter] nên `DisconnectUseCase` (getAll) tự thấy và xóa cache video khi ngắt kết nối (CD-05).
 */
val androidPlayerModule = module {
    single { VideoCache(get<Context>(), get()) } binds arrayOf(ConnectionResetter::class)
    single { VideoPlayerFactory(get<Context>(), get(), get()) }
}
