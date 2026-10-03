package com.lambao.odv.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.lambao.odv.core.domain.model.LockState
import com.lambao.odv.core.domain.repository.SecurityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Tự khóa khi rời app (CH-03, ADR-0014). Gắn vào `ProcessLifecycleOwner`: `onStop` của nó chỉ chạy khi **cả process** không
 * còn Activity nào hiển thị, sau độ trễ khoảng 700 ms. Nên xoay màn hình (Activity tạo lại), chuyển sang `DebugActivity`
 * hay hiện BiometricPrompt không khóa nhầm, còn bấm Home hay chuyển app thì khóa.
 *
 * Khóa ngay, không có thời gian ân hạn (đặc tả chưa nói; Lát 9 có thể thêm cài đặt). [SecurityRepository.lock] không làm
 * gì ở chế độ thiết bị. [onLocked] là móc cho công cụ debug xóa log API (ADR-0013): gọi mỗi khi trạng thái chuyển sang
 * Locked, kể cả khi khóa xảy ra muộn (thao tác bật PIN đang dở bị hoàn tác sau khi app xuống nền), nên quan sát
 * [SecurityRepository.lockState] thay vì kiểm tra một lần lúc `onStop`.
 */
class AppLockController(
    private val security: SecurityRepository,
    private val onLocked: () -> Unit,
) : DefaultLifecycleObserver {

    // Sống cùng process (Application giữ controller), nên không cần hủy.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.launch {
            security.lockState.collect { state -> if (state == LockState.Locked) onLocked() }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        security.lock()
    }
}
