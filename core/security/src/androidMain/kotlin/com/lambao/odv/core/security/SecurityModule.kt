package com.lambao.odv.core.security

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Binding Koin của `:core:security` (bản Android). Ghép ở MainApplication. */
val securityModule = module {
    single<SecretStore> { KeystoreSecretStore(androidContext(), get()) }
    single<PinKeyDeriver> { Argon2PinKeyDeriver(get()) }
    single<PinEnvelopeCodec> { AndroidPinEnvelopeCodec(get()) }
    single<BootAwareClock> { AndroidBootAwareClock(androidContext()) }
}
