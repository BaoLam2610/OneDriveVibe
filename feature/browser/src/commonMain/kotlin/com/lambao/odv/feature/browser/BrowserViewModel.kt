package com.lambao.odv.feature.browser

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.repository.DriveRepository
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * Tab Thư mục ở Lát 1 (TM-01, TM-02, TM-07): gọi thẳng API liệt kê thư mục vì chưa có Room (đồng bộ delta là Lát 3).
 * Từ Lát 3, [DriveRepository.listChildren] được thay bằng luồng đọc từ Room và ViewModel này chỉ đổi nguồn dữ liệu.
 */
class BrowserViewModel(
    private val drives: DriveRepository,
) : BaseMviViewModel<BrowserState, BrowserIntent, BrowserEffect>(BrowserState()) {

    // Nhớ nội dung các thư mục đã mở trong phiên màn hình này để lên/xuống cấp không phải tải lại.
    // Lát 3 bỏ đi vì Room là nguồn dữ liệu.
    private val cache = mutableMapOf<String?, List<DriveItem>>()
    private var loadJob: Job? = null
    private val log = Logger.withTag("Browser")

    init {
        load()
    }

    override fun onIntent(intent: BrowserIntent) {
        when (intent) {
            is BrowserIntent.Open ->
                if (intent.item.isFolder) {
                    navigateTo(currentState.path + Crumb(intent.item.id, intent.item.name))
                } else {
                    sendEffect(BrowserEffect.OpenFile(intent.item))
                }
            BrowserIntent.GoUp -> if (currentState.path.isNotEmpty()) navigateTo(currentState.path.dropLast(1))
            is BrowserIntent.GoToCrumb ->
                if (intent.index in 0 until currentState.path.size) navigateTo(currentState.path.take(intent.index))
            BrowserIntent.Retry -> load()
        }
    }

    private fun navigateTo(path: List<Crumb>) {
        setState { copy(path = path) }
        load()
    }

    private fun load() {
        val folderId = currentState.path.lastOrNull()?.id
        loadJob?.cancel()
        cache[folderId]?.let { cached ->
            setState { copy(items = cached, isLoading = false, error = null) }
            return
        }
        setState { copy(items = emptyList(), isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            val result = drives.listChildren(folderId)
            // Người dùng đã đi nơi khác trong lúc chờ: bỏ kết quả cũ, tránh ghi đè danh sách của thư mục mới.
            ensureActive()
            when (result) {
                is AppResult.Success -> {
                    val visible = result.value.toVisibleItems()
                    cache[folderId] = visible
                    log.d { "Tải thư mục: ${result.value.size} mục, hiển thị ${visible.size}" }
                    setState { copy(items = visible, isLoading = false) }
                }
                is AppResult.Failure -> {
                    val error = result.error.toBrowserError()
                    log.w { "Tải thư mục thất bại: ${error.kind} code=${error.code}" }
                    setState { copy(isLoading = false, error = error) }
                }
            }
        }
    }

    /**
     * TM-03: chỉ giữ thư mục và tệp thuộc loại được hỗ trợ (Lát 1 coi cả ba loại đều bật; bật/tắt từng loại là Lát 9).
     * TM-02: thư mục trước, rồi theo tên không phân biệt hoa thường (sắp xếp tùy chọn là TM-05, Lát 3).
     */
    private fun List<DriveItem>.toVisibleItems(): List<DriveItem> =
        filter { it.isFolder || it.mediaKind != null }
            .sortedWith(compareByDescending<DriveItem> { it.isFolder }.thenBy { it.name.lowercase() })
}
