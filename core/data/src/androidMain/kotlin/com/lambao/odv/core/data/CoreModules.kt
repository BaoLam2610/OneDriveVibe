package com.lambao.odv.core.data

import com.lambao.odv.core.database.databaseModule
import com.lambao.odv.core.network.networkModule
import com.lambao.odv.core.security.securityModule
import org.koin.core.module.Module

/**
 * Mọi module Koin của tầng core (bản Android), gom ở đây để `:androidApp` chỉ ghép một danh sách mà không phải phụ thuộc
 * `:core:network` và `:core:database` chỉ để import `networkModule`, `databaseModule` (ADR-0010). Thêm module core mới thì
 * thêm vào đây, không sửa `MainApplication`. MVP2: bản `iosMain` cùng tên với các module iOS tương ứng.
 *
 * Thứ tự giữ như cũ. Module debug (`DebugTools.koinModules`) phải đứng sau danh sách này để ghi đè `ThumbnailQuality`.
 */
val coreModules: List<Module> = listOf(
    securityModule,
    networkModule,
    databaseModule,
    dataModule,
    androidDataModule,
    useCaseModule,
)
