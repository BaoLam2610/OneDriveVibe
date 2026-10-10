package com.lambao.odv.feature.shorts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.media.ExoPlayerFactory
import com.lambao.odv.core.media.VideoCache
import kotlinx.coroutines.launch

/**
 * Giữ [ShortPlayerController] suốt vòng đời Màn chính (ADR-0024 mục 6): rời tab Short chỉ tạm dừng, ExoPlayer và bộ giải mã chỉ được giải
 * phóng khi Màn chính bị bỏ (Ngắt kết nối, thoát app), nên quay lại tab thì phát tiếp đúng vị trí (SV-11). Là ViewModel riêng ở androidMain
 * vì `ShortsViewModel` nằm ở commonMain và không được biết tới kiểu của Media3. Player được dựng ngay khi tab Short mở lần đầu.
 */
internal class ShortPlayerHolder(
    private val factory: ExoPlayerFactory,
    private val videoCache: VideoCache,
) : ViewModel() {

    /** Null cho tới khi dựng xong (mở cache lần đầu có thể chờ đĩa ngắn). */
    var controller by mutableStateOf<ShortPlayerController?>(null)
        private set

    init {
        // viewModelScope chạy trên luồng chính: ExoPlayer phải được tạo và dùng trên cùng một luồng.
        viewModelScope.launch {
            controller = ShortPlayerController(factory.create(), videoCache)
        }
    }

    override fun onCleared() {
        controller?.release()
        controller = null
    }
}
