package com.lambao.odv.feature.shorts

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Binding Koin của `:feature:shorts` cần Android (Media3). Ghép ở MainApplication cùng [shortsModule]. `ExoPlayerFactory` và `VideoCache` đến từ
 * `androidMediaModule` của `:core:media` (ADR-0025).
 */
val androidShortsModule = module {
    viewModel { ShortPlayerHolder(get(), get()) }
}
