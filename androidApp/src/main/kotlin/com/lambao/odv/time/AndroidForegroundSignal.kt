package com.lambao.odv.time

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.lambao.odv.core.domain.platform.ForegroundSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * [ForegroundSignal] cho Android: gắn vào `ProcessLifecycleOwner` như `AppLockController`, `onStart` của nó chỉ chạy khi Activity
 * đầu tiên hiện (không chạy khi xoay màn hình hay chuyển giữa các Activity của app).
 */
class AndroidForegroundSignal : ForegroundSignal, DefaultLifecycleObserver {

    private val count = MutableStateFlow(0)

    override val entries: Flow<Int> = count.asStateFlow()

    override fun onStart(owner: LifecycleOwner) {
        count.update { it + 1 }
    }
}
