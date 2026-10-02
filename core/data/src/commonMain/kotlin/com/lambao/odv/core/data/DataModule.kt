package com.lambao.odv.core.data

import com.lambao.odv.core.data.config.ConfigRepositoryImpl
import com.lambao.odv.core.data.drive.DriveRepositoryImpl
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.domain.repository.DriveRepository
import org.koin.dsl.module

/** Binding Koin của `:core:data`. Ghép ở MainApplication sau networkModule và securityModule. */
val dataModule = module {
    single<ConfigRepository> { ConfigRepositoryImpl(get(), get()) }
    single<DriveRepository> { DriveRepositoryImpl(get(), get()) }
}
