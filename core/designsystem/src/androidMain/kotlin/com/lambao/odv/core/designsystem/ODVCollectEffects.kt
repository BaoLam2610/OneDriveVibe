package com.lambao.odv.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Thu Effect của ViewModel (ADR-0002): chỉ khi màn đang STARTED, ngừng khi xuống nền và thu lại khi quay về.
 * Mỗi màn gọi đúng một lần, vì hai nơi cùng thu một `effects` sẽ chia nhau effect. Effect gửi lúc màn chưa thu vẫn nằm
 * trong bộ đệm của Channel nên không mất.
 */
@Composable
fun <E> ODVCollectEffects(effects: Flow<E>, onEffect: suspend (E) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEffect = rememberUpdatedState(onEffect)
    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            effects.collect { currentOnEffect.value(it) }
        }
    }
}
