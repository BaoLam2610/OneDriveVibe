package com.lambao.odv.feature.shorts

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Binding Koin của `:feature:shorts` dùng chung mọi nền tảng. Ghép ở MainApplication cùng [androidShortsModule]. */
val shortsModule = module {
    viewModelOf(::ShortsViewModel)
}
