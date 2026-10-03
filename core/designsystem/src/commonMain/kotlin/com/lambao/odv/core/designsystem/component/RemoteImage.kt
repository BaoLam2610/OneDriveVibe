package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/**
 * Ảnh tải từ xa (thumbnail ảnh/video) đặt lên trên [placeholder]. Khung giữ chỗ luôn nằm dưới và ảnh phủ lên khi tải xong
 * (Coil làm mờ dần), nên ô không nhấp nháy và vẫn đúng khi ảnh không có hoặc tải lỗi (offline, tệp không có thumbnail):
 * lúc đó chỉ còn khung giữ chỗ.
 *
 * [model] là đối tượng Coil hiểu được (ở app là `ThumbnailSource`); null thì chỉ hiện [placeholder]. ImageLoader lấy từ
 * singleton của Coil do app cấu hình. TalkBack bỏ qua ảnh: nhãn đọc do ô chứa nó đặt.
 */
@Composable
fun ODVRemoteImage(
    model: Any?,
    modifier: Modifier = Modifier,
    placeholder: @Composable () -> Unit,
) {
    Box(modifier) {
        placeholder()
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
