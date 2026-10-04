package com.lambao.odv.feature.imageviewer

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.ViewerContext
import com.lambao.odv.core.domain.repository.DriveRepository
import com.lambao.odv.core.domain.repository.OriginalImageRef
import com.lambao.odv.core.domain.repository.OriginalImageRepository
import com.lambao.odv.core.domain.repository.OriginalImageState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Màn xem ảnh (AN-01 → AN-07). Danh sách ảnh đọc từ Room theo [context] (ADR-0007): cùng thư mục hoặc cùng bộ lọc Thư
 * viện, đúng thứ tự người dùng thấy ở nơi mở (AN-03), tự cập nhật khi đồng bộ đổi dữ liệu. Ảnh gốc do
 * [OriginalImageRepository] tải và cache riêng; mỗi trang tự thu [originalOf] nên chỉ trang đang hiện mới tải.
 */
class ImageViewerViewModel(
    private val context: ViewerContext,
    private val startItemId: String,
    private val drives: DriveRepository,
    private val originals: OriginalImageRepository,
) : BaseMviViewModel<ImageViewerState, ImageViewerIntent, ImageViewerEffect>(ImageViewerState(currentId = startItemId)) {

    private var infoJob: Job? = null
    private var closed = false

    init {
        observeImages()
    }

    /**
     * Ảnh gốc của [item] (AN-01). Không phải Intent vì là luồng dữ liệu của từng trang chứ không phải hành động của người
     * dùng; tải gắn với vòng đời của trang nên hủy khi vuốt đi hoặc app xuống nền (phần đã tải được giữ, BN-03).
     */
    fun originalOf(item: DriveItem): Flow<OriginalImageState> = originals.open(OriginalImageRef(item.id, item.cTag))

    override fun onIntent(intent: ImageViewerIntent) {
        when (intent) {
            is ImageViewerIntent.PageChanged -> setState { copy(currentId = intent.itemId) }
            ImageViewerIntent.ToggleControls -> setState { copy(controlsVisible = !controlsVisible) }
            ImageViewerIntent.ShowInfo -> showInfo()
            ImageViewerIntent.HideInfo -> {
                infoJob?.cancel()
                setState { copy(info = null) }
            }
        }
    }

    private fun observeImages() {
        viewModelScope.launch {
            drives.observeViewerItems(context, MediaKind.Image).collect { images ->
                if (images.isEmpty()) {
                    setState { copy(images = emptyList(), isLoaded = true) }
                    if (!closed) {
                        closed = true
                        sendEffect(ImageViewerEffect.Close)
                    }
                    return@collect
                }
                setState {
                    copy(
                        images = images,
                        // Chỉ tính vị trí mở đầu ở danh sách đầu tiên; sau đó giao diện giữ đúng ảnh theo currentId.
                        initialIndex = if (isLoaded) initialIndex else images.indexOfFirst { it.id == startItemId }.coerceAtLeast(0),
                        isLoaded = true,
                    )
                }
            }
        }
    }

    private fun showInfo() {
        val item = currentState.images.firstOrNull { it.id == currentState.currentId } ?: return
        infoJob?.cancel()
        setState { copy(info = ImageViewerInfo(item)) }
        infoJob = viewModelScope.launch {
            try {
                coroutineScope {
                    val path = async { drives.folderPathOf(item.id) }
                    val details = async { drives.getImageInfo(item.id) }
                    // await là hàm suspend nên lấy kết quả ra trước, không gọi trong lambda của updateInfo.
                    val folderPath = path.await()
                    updateInfo(item.id) { it.copy(folderPath = folderPath) }
                    // Offline hoặc lỗi: bỏ qua, bảng vẫn hiện các dòng đã có trong Room.
                    val loaded = (details.await() as? AppResult.Success)?.value
                    updateInfo(item.id) { it.copy(details = loaded, isLoadingDetails = false) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Lỗi đọc Room: bảng thông tin chỉ thiếu vài dòng, không đáng làm sập app (viewModelScope không có handler).
                updateInfo(item.id) { it.copy(isLoadingDetails = false) }
            }
        }
    }

    private fun updateInfo(itemId: String, transform: (ImageViewerInfo) -> ImageViewerInfo) {
        setState {
            val current = info
            if (current != null && current.item.id == itemId) copy(info = transform(current)) else this
        }
    }
}
