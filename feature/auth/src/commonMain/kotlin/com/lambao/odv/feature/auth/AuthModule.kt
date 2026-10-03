package com.lambao.odv.feature.auth

import com.lambao.odv.feature.auth.connect.ConnectViewModel
import com.lambao.odv.feature.auth.lock.LockViewModel
import com.lambao.odv.feature.auth.security.SecuritySetupViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Binding Koin của `:feature:auth`. Ghép ở MainApplication. */
val authModule = module {
    viewModelOf(::ConnectViewModel)
    viewModelOf(::SecuritySetupViewModel)
    viewModelOf(::LockViewModel)
}
