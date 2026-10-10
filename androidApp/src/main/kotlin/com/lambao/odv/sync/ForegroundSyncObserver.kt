package com.lambao.odv.sync

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.lambao.odv.core.domain.usecase.sync.SyncOnForegroundUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Tự đồng bộ khi process vào foreground (DS-04). Gắn vào `ProcessLifecycleOwner` như `AppLockController`: `onStart` của nó chạy
 * khi Activity đầu tiên hiện, không chạy khi xoay màn hình hay chuyển giữa các Activity của app. Điều kiện (đã mở khóa, đã có
 * config, quá 15 phút) nằm ở [SyncOnForegroundUseCase]; ở đây chỉ nối vòng đời. Tách khỏi `AppLockController` để mỗi lớp một việc.
 */
class ForegroundSyncObserver(
    private val syncOnForeground: SyncOnForegroundUseCase,
) : DefaultLifecycleObserver {

    // Sống cùng process (Application giữ observer), nên không cần hủy.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStart(owner: LifecycleOwner) {
        scope.launch { syncOnForeground() }
    }
}
