package com.lambao.odv.feature.player

import org.koin.dsl.module

/**
 * Binding Koin của `:feature:player` cần Android (Media3). Ghép ở MainApplication cùng [playerModule]. `VideoCache` và `ExoPlayerFactory`
 * đã chuyển sang `androidMediaModule` của `:core:media` (ADR-0025); `VideoCache` vẫn là `ConnectionResetter` và `CacheStore` ở đó.
 */
val androidPlayerModule = module {
    single { VideoPlayerFactory(get(), get()) }
}
