package com.lambao.odv.security

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.lambao.odv.core.domain.model.AutoLockDelay
import com.lambao.odv.core.domain.model.LockState
import com.lambao.odv.core.domain.settings.SecuritySettings
import com.lambao.odv.core.domain.usecase.security.LockAppUseCase
import com.lambao.odv.core.domain.usecase.security.ObserveLockStateUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tự khóa khi rời app (CH-03, ADR-0014, ADR-0019). Gắn vào `ProcessLifecycleOwner`: `onStop` của nó chỉ chạy khi **cả process** không
 * còn Activity nào hiển thị, sau độ trễ khoảng 700 ms. Nên xoay màn hình (Activity tạo lại), chuyển sang `DebugActivity` hay hiện
 * BiometricPrompt không khóa nhầm, còn bấm Home hay chuyển app thì bắt đầu đếm.
 *
 * Khóa sau [AutoLockDelay] người dùng chọn ở Cài đặt (mặc định 1 phút): quay lại trước mốc đó thì không khóa; [AutoLockDelay.Immediately]
 * khóa ngay trong `onStop` như trước Lát 7c. Mốc tính bằng `SystemClock.elapsedRealtime` (đếm cả lúc máy ngủ) và được kiểm tra lại ở
 * `onStart`, nên Doze trì hoãn bộ hẹn giờ cũng không làm app sống quá hạn. Process bị hệ thống thu hồi trong lúc chờ thì lần mở sau
 * là khởi động nguội: luôn vào màn Khóa (an toàn hơn mốc đã chọn).
 *
 * [LockAppUseCase] không làm gì ở chế độ thiết bị. [onLocked] là móc cho công cụ debug xóa log API (ADR-0013): gọi mỗi khi trạng thái
 * chuyển sang Locked, kể cả khi khóa xảy ra muộn (thao tác bật PIN đang dở bị hoàn tác sau khi app xuống nền), nên quan sát
 * [ObserveLockStateUseCase] thay vì kiểm tra một lần lúc `onStop`.
 */
class AppLockController(
    private val observeLockState: ObserveLockStateUseCase,
    private val lockApp: LockAppUseCase,
    securitySettings: SecuritySettings,
    private val onLocked: () -> Unit,
) : DefaultLifecycleObserver {

    // Sống cùng process (Application giữ controller), nên không cần hủy.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Giữ giá trị mới nhất để `onStop` quyết định đồng bộ (không được chờ gì lúc app xuống nền). Chưa đọc xong thì khóa ngay: phía an toàn.
    @Volatile
    private var autoLock = AutoLockDelay.Immediately

    private var pendingLock: Job? = null
    private var stoppedAtMs = 0L
    private var isStopped = false

    init {
        scope.launch {
            observeLockState().collect { state -> if (state == LockState.Locked) onLocked() }
        }
        scope.launch {
            securitySettings.autoLockDelay.collect { autoLock = it }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        pendingLock?.cancel()
        pendingLock = null
        if (!isStopped) return
        isStopped = false
        // Hẹn giờ có thể bị trì hoãn (Doze): quá hạn rồi thì khóa ngay, trước khi người dùng kịp thao tác.
        if (SystemClock.elapsedRealtime() - stoppedAtMs >= autoLock.millis) lockApp()
    }

    override fun onStop(owner: LifecycleOwner) {
        val chosen = autoLock
        if (chosen.millis <= 0L) {
            lockApp()
            return
        }
        isStopped = true
        stoppedAtMs = SystemClock.elapsedRealtime()
        pendingLock?.cancel()
        pendingLock = scope.launch {
            delay(chosen.millis)
            lockApp()
        }
    }
}
