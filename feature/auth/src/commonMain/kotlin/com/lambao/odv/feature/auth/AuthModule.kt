package com.lambao.odv.feature.auth

import com.lambao.odv.feature.auth.connect.ConnectViewModel
import com.lambao.odv.feature.auth.security.SecuritySetupViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Binding Koin của `:feature:auth`. Ghép ở MainApplication. */
val authModule = module {
    single { PendingConnection() }
    viewModelOf(::ConnectViewModel)
    viewModelOf(::SecuritySetupViewModel)
}
