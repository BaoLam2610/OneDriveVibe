package com.lambao.odv.core.domain.usecase.watch

import com.lambao.odv.core.domain.model.ContinueEntry
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.WatchProgress
import com.lambao.odv.core.domain.repository.ContinueRepository
import com.lambao.odv.core.domain.repository.WatchProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

/**
 * Vị trí xem dở của video để hỏi hoặc tự xem tiếp (VD-12). Null nếu chưa xem lần nào, tệp đã đổi nội dung, hoặc đã chạy hết video:
 * xem xong thì mở lại phát từ đầu, không hỏi.
 */
class GetWatchProgressUseCase(
    private val progress: WatchProgressRepository,
) {
    suspend operator fun invoke(itemId: String, cTag: String?): WatchProgress? =
        progress.get(itemId, cTag)?.takeUnless { it.finished }
}

/**
 * Lưu vị trí xem (VD-12). **Mọi vị trí đều được lưu, không có ngưỡng** (2026-10-13, bỏ luật 5% và 95%); chỉ bỏ qua vị trí 0 (chưa xem gì) và
 * thời lượng chưa biết (≤ 0). Video chạy hết (vị trí chạm thời lượng) thì lưu là đã xem xong (`finished`, rời dải Xem tiếp, mở lại phát từ
 * đầu); dừng ở bất kỳ chỗ nào khác, kể cả sát cuối, vẫn là xem dở. Chế độ Lặp một video chỉ gọi ở vòng đầu (người gọi lo), các vòng sau không lưu.
 */
class SaveWatchProgressUseCase(
    private val progress: WatchProgressRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(itemId: String, cTag: String?, positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L || positionMs <= 0L) return
        val position = positionMs.coerceAtMost(durationMs)
        progress.save(
            WatchProgress(
                itemId = itemId,
                cTag = cTag,
                positionMs = position,
                durationMs = durationMs,
                updatedAt = clock.now().toEpochMilliseconds(),
                finished = position >= durationMs,
            ),
        )
    }
}

/** Xóa vị trí xem của video: "Xem từ đầu" ở hộp thoại hoặc cài đặt "Luôn phát từ đầu" (VD-12). */
class ClearWatchProgressUseCase(
    private val progress: WatchProgressRepository,
) {
    suspend operator fun invoke(itemId: String) = progress.clear(itemId)
}

/** Các mục của dải "Xem tiếp / Đọc tiếp" (DS-02), theo phạm vi xem hiện tại và loại tệp đang bật. */
class ObserveContinueItemsUseCase(
    private val continueItems: ContinueRepository,
) {
    operator fun invoke(enabledKinds: Set<MediaKind>): Flow<List<ContinueEntry>> = continueItems.observe(enabledKinds)
}

/** Bấm X ở thẻ của dải: ẩn khỏi dải, giữ vị trí hoặc trang (DS-02). */
class DismissContinueItemUseCase(
    private val continueItems: ContinueRepository,
) {
    suspend operator fun invoke(itemId: String) = continueItems.dismiss(itemId)
}
