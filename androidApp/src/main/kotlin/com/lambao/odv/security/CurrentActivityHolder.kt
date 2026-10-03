package com.lambao.odv.security

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.withResumed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.lang.ref.WeakReference

/**
 * Giữ `FragmentActivity` đang dùng (`MainActivity`) để hiện `BiometricPrompt` từ tầng không có Activity (ADR-0014). Giữ yếu
 * và bỏ khi Activity bị hủy nên không rò rỉ. Chỉ nhận `FragmentActivity`: `DebugActivity` (ComponentActivity) bị bỏ qua.
 */
class CurrentActivityHolder : Application.ActivityLifecycleCallbacks {

    private var current: WeakReference<FragmentActivity>? = null

    /**
     * Activity khi đã ở trạng thái RESUMED, hoặc null nếu chưa có. `BiometricPrompt` chỉ hiện được trên Activity đang hiển thị
     * (khởi động nguội: màn Khóa tự gọi từ lúc Activity còn đang tạo). Chờ cho tới khi RESUMED; bị hủy khi người gọi hủy.
     */
    suspend fun awaitResumed(): FragmentActivity? {
        val activity = current?.get() ?: return null
        return try {
            activity.lifecycle.withResumed { activity }
        } catch (e: CancellationException) {
            // Activity bị hủy (xoay màn hình, đổi ngôn ngữ) trong lúc chờ làm `withResumed` ném CancellationException dù
            // coroutine gọi vẫn sống. Không rethrow thì ViewModel (sống qua xoay màn hình) không bao giờ nhận kết quả và
            // bàn phím bị khóa mãi. Nếu chính coroutine gọi bị hủy thì ensureActive ném lại.
            currentCoroutineContext().ensureActive()
            null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is FragmentActivity) current = WeakReference(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (current?.get() === activity) current = null
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
