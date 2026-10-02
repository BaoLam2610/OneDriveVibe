package com.lambao.odv.core.common.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel MVI dùng chung (ADR-0002).
 *
 * - [S] State: data class bất biến mô tả toàn bộ màn hình, UI đọc qua [state].
 * - [I] Intent: hành động của người dùng, UI gửi qua [onIntent].
 * - [E] Effect: thao tác một lần (điều hướng, snackbar, mở player), UI thu qua [effects]. Không đặt những việc này
 *   vào State, tránh chạy lại khi xoay màn hình hoặc đổi ngôn ngữ.
 *
 * Effect đi qua [Channel] nên mỗi effect chỉ được nhận một lần; effect gửi lúc UI chưa thu sẽ chờ trong bộ đệm.
 * Chỉ thu [effects] ở **một** nơi (màn `XxxScreen`, theo vòng đời STARTED): hai nơi cùng thu sẽ chia nhau effect.
 */
abstract class BaseMviViewModel<S : Any, I : Any, E : Any>(initialState: S) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    // UNLIMITED + trySend: không bao giờ treo và giữ đúng thứ tự gửi (BUFFERED + launch có thể đảo thứ tự khi đầy bộ đệm).
    private val _effects = Channel<E>(Channel.UNLIMITED)
    val effects: Flow<E> = _effects.receiveAsFlow()

    /** State hiện tại, dùng trong xử lý intent. */
    protected val currentState: S get() = _state.value

    /** Điểm vào duy nhất cho mọi hành động từ UI. */
    abstract fun onIntent(intent: I)

    /** Cập nhật State bằng một reducer thuần, an toàn khi gọi đồng thời. */
    protected fun setState(reduce: S.() -> S) {
        _state.update(reduce)
    }

    protected fun sendEffect(effect: E) {
        _effects.trySend(effect)
    }
}
