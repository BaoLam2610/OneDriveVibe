package com.lambao.odv.feature.auth

import com.lambao.odv.feature.auth.connect.ConnectViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Binding Koin của `:feature:auth`. Ghép ở MainApplication.
 * Màn Thiết lập bảo mật chưa có ViewModel: bước đặt PIN (BM-01 → BM-07) làm ở Lát 2.
 */
val authModule = module {
    viewModelOf(::ConnectViewModel)
}
