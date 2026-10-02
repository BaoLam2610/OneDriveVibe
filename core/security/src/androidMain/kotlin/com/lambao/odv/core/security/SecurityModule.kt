package com.lambao.odv.core.security

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Binding Koin của `:core:security` (bản Android). Ghép ở MainApplication. */
val securityModule = module {
    single<SecretStore> { KeystoreSecretStore(androidContext(), get()) }
}
