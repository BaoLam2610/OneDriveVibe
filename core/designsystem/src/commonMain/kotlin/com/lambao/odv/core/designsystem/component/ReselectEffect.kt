package com.lambao.odv.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Chạy [onReselect] mỗi khi [signal] đổi sau lần dựng đầu, dùng cho "chạm lại tab đang chọn" (DH-04): thanh đáy tăng bộ đếm của tab,
 * tab cuộn lên đầu. Giá trị đã xử lý được lưu cùng trạng thái tab (`rememberSaveable`) nên khi tab được dựng lại sau lúc rời đi
 * (đổi tab, màn xem phủ lên) mà bộ đếm vẫn là số cũ thì **không** tự cuộn.
 */
@Composable
fun ODVReselectEffect(signal: Int, onReselect: suspend () -> Unit) {
    var handled by rememberSaveable { mutableIntStateOf(signal) }
    val action by rememberUpdatedState(onReselect)
    LaunchedEffect(signal) {
        if (signal != handled) {
            handled = signal
            action()
        }
    }
}
