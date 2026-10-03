package com.lambao.odv.core.data

import com.lambao.odv.core.data.config.ConfigRepositoryImpl
import com.lambao.odv.core.data.config.ConfigVault
import com.lambao.odv.core.data.drive.DriveRepositoryImpl
import com.lambao.odv.core.data.security.LockoutStore
import com.lambao.odv.core.data.security.SecurityRepositoryImpl
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.domain.repository.DriveRepository
import com.lambao.odv.core.domain.repository.SecurityRepository
import com.lambao.odv.core.domain.usecase.DisconnectUseCase
import org.koin.dsl.module

/** Binding Koin của `:core:data`. Ghép ở MainApplication sau networkModule và securityModule. */
val dataModule = module {
    // ConfigVault dùng chung cho ConfigRepository và SecurityRepository: một bản config trong bộ nhớ, một trạng thái khóa.
    single { ConfigVault(get(), get()) }
    single { LockoutStore(get(), get()) }
    single<ConfigRepository> { ConfigRepositoryImpl(get(), get()) }
    // BiometricAuthenticator do :androidApp cung cấp (cần Activity để hiện BiometricPrompt).
    single<SecurityRepository> { SecurityRepositoryImpl(get(), get(), get(), get(), get(), get()) }
    single<DriveRepository> { DriveRepositoryImpl(get(), get()) }
    // getAll: mỗi lát đăng ký ConnectionResetter của mình (Room, cache, cài đặt) mà không phải sửa chỗ này.
    factory { DisconnectUseCase(getAll(), get()) }
}
