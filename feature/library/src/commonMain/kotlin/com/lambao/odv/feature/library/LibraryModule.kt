package com.lambao.odv.feature.library

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Binding Koin của `:feature:library`. Ghép ở MainApplication. */
val libraryModule = module {
    viewModelOf(::LibraryViewModel)
}
