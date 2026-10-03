package com.lambao.odv.core.data

import com.lambao.odv.core.data.config.ConfigRepositoryImpl
import com.lambao.odv.core.data.config.ConfigVault
import com.lambao.odv.core.data.drive.DriveRepositoryImpl
import com.lambao.odv.core.data.security.LockoutStore
import com.lambao.odv.core.data.security.SecurityRepositoryImpl
import com.lambao.odv.core.data.sync.SyncCoordinator
import com.lambao.odv.core.data.sync.SyncEngine
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.domain.repository.ConnectionResetter
import com.lambao.odv.core.domain.repository.DriveRepository
import com.lambao.odv.core.domain.repository.SecurityRepository
import com.lambao.odv.core.domain.repository.SyncRepository
import com.lambao.odv.core.domain.usecase.DisconnectUseCase
import org.koin.dsl.binds
import org.koin.dsl.module
import kotlin.time.Clock

private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** Binding Koin của `:core:data`. Ghép ở MainApplication sau networkModule và securityModule. */
val dataModule = module {
    // ConfigVault dùng chung cho ConfigRepository và SecurityRepository: một bản config trong bộ nhớ, một trạng thái khóa.
    single { ConfigVault(get(), get()) }
    single { LockoutStore(get(), get()) }
    single<ConfigRepository> { ConfigRepositoryImpl(get(), get()) }
    // BiometricAuthenticator do :androidApp cung cấp (cần Activity để hiện BiometricPrompt).
    single<SecurityRepository> { SecurityRepositoryImpl(get(), get(), get(), get(), get(), get()) }
    single<DriveRepository> { DriveRepositoryImpl(get(), get(), get(), get()) }
    // Đồng bộ delta về Room (ADR-0007). SyncCoordinator vừa là SyncRepository vừa là ConnectionResetter của Room nên
    // DisconnectUseCase (getAll) tự thấy nó mà không sửa chỗ khác.
    single { SyncEngine(get(), get(), get(), ::currentTimeMillis) }
    single { SyncCoordinator(get(), get(), get(), ::currentTimeMillis) } binds arrayOf(
        SyncRepository::class,
        ConnectionResetter::class,
    )
    // getAll: mỗi lát đăng ký ConnectionResetter của mình (Room, cache, cài đặt) mà không phải sửa chỗ này.
    factory { DisconnectUseCase(getAll(), get()) }
}
