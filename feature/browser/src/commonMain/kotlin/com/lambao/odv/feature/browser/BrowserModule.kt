package com.lambao.odv.feature.browser

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Binding Koin của `:feature:browser`. Ghép ở MainApplication. */
val browserModule = module {
    viewModelOf(::BrowserViewModel)
}
