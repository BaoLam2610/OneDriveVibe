package com.lambao.odv.core.security

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

internal class AndroidBootAwareClock(context: Context) : BootAwareClock {

    private val resolver = context.applicationContext.contentResolver

    override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()

    // Settings.Global.BOOT_COUNT có từ API 24 (minSdk của app).
    override fun bootCount(): Int = Settings.Global.getInt(resolver, Settings.Global.BOOT_COUNT, -1)
}
