package com.lambao.odv.feature.shorts

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.domain.usecase.settings.ObserveShortMaxMinutesUseCase
import com.lambao.odv.core.domain.usecase.shorts.GetShortVideoUseCase
import com.lambao.odv.core.domain.usecase.shorts.ObserveShortTabAvailableUseCase
import com.lambao.odv.core.domain.usecase.shorts.ObserveShortVideoIdsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel tab Short (MVI, ADR-0002). Sống suốt vòng đời Màn chính (ADR-0023) nên chuyển tab rồi quay lại vẫn đúng thứ tự và video
 * đang xem (DH-02). Chỉ giữ **id** đã xáo cộng chi tiết vài video quanh trang hiện tại, không giữ cả bản ghi (ADR-0024 mục 5).
 *
 * Thứ tự xáo xác định bởi (danh sách gốc theo id, seed) nên chỉ cần lưu seed để dựng lại sau khi tiến trình bị thu hồi (SV-02).
 * Video bị xóa trên OneDrive thì gỡ khỏi danh sách đang xáo, video mới đồng bộ thì **không** chèn vào (SV-16).
 */
class ShortsViewModel(
    private val observeMaxMinutes: ObserveShortMaxMinutesUseCase,
    private val observeVideoIds: ObserveShortVideoIdsUseCase,
    private val getVideo: GetShortVideoUseCase,
    observeAvailable: ObserveShortTabAvailableUseCase,
) : BaseMviViewModel<ShortsState, ShortsIntent, ShortsEffect>(ShortsState()) {

    private var buildJob: Job? = null
    private var trackJob: Job? = null

    /** Danh sách gốc theo thứ tự cố định của DB (id tăng dần), nền để xáo lại và xáo bằng seed. */
    private var sourceIds: List<String> = emptyList()
    private var liveIds: Set<String> = emptySet()

    init {
        // DH-01: tắt loại Video (hoặc chưa đồng bộ xong) thì mục Short biến mất và trạng thái bị bỏ; bật lại thì lần vào kế tiếp xáo mới (SV-02).
        viewModelScope.launch {
            observeAvailable().collect { available -> if (!available) reset() }
        }
    }

    override fun onIntent(intent: ShortsIntent) {
        when (intent) {
            is ShortsIntent.Enter -> viewModelScope.launch { enter(intent.restore) }
            is ShortsIntent.PageSettled -> {
                setState { copy(index = intent.index) }
                loadAround(intent.index)
            }
            ShortsIntent.Reshuffle -> reshuffle()
            ShortsIntent.RestoreConsumed -> setState { copy(resumePositionMs = 0L, startPaused = false) }
        }
    }

    private suspend fun enter(restore: ShortsSnapshot?) {
        val max = observeMaxMinutes().first()
        val state = currentState
        when {
            buildJob?.isActive == true -> Unit
            state.phase == ShortsPhase.Loading ->
                if (restore != null && restore.maxMinutes == max) {
                    build(max, restore.seed, restore.index, restore.positionMs, paused = true)
                } else {
                    build(max, Random.nextLong(), index = 0, positionMs = 0L, paused = false)
                }
            // SV-16: đã đổi "Thời lượng tối đa của Short" từ lần xem trước thì lọc lại và xáo mới.
            max != state.maxMinutes -> build(max, Random.nextLong(), index = 0, positionMs = 0L, paused = false)
            else -> Unit
        }
    }

    private fun build(max: Int, seed: Long, index: Int, positionMs: Long, paused: Boolean) {
        buildJob?.cancel()
        trackJob?.cancel()
        buildJob = viewModelScope.launch {
            val source = observeVideoIds(max).first()
            sourceIds = source
            liveIds = source.toSet()
            val ids = ordered(source, seed)
            val start = index.coerceIn(0, (ids.size - 1).coerceAtLeast(0))
            setState {
                copy(
                    phase = if (ids.isEmpty()) ShortsPhase.Empty else ShortsPhase.Ready,
                    ids = ids,
                    index = start,
                    maxMinutes = max,
                    seed = seed,
                    generation = generation + 1,
                    resumePositionMs = positionMs,
                    startPaused = paused,
                    videos = emptyMap(),
                )
            }
            shortsLog.i { "[Short] dựng danh sách ${ids.size} video (tối đa $max phút), bắt đầu ở $start, khôi phục=$paused" }
            if (ids.isNotEmpty()) loadAround(start)
            trackDeletions(max)
        }
    }

    /** SV-16: video bị xóa trên OneDrive thì gỡ khỏi danh sách đang xáo; đang hiện thì chuyển sang video kế tiếp (cùng chỉ số). */
    private fun trackDeletions(max: Int) {
        trackJob = viewModelScope.launch {
            observeVideoIds(max).collect { live ->
                sourceIds = live
                liveIds = live.toSet()
                val state = currentState
                val remaining = state.ids.filter { it in liveIds }
                if (remaining.size == state.ids.size) return@collect
                val currentId = state.ids.getOrNull(state.index)
                val newIndex = remaining.indexOf(currentId).takeIf { it >= 0 }
                    ?: state.index.coerceAtMost((remaining.size - 1).coerceAtLeast(0))
                shortsLog.i { "[Short] gỡ ${state.ids.size - remaining.size} video đã bị xóa khỏi danh sách" }
                setState {
                    copy(
                        ids = remaining,
                        index = newIndex,
                        phase = if (remaining.isEmpty()) ShortsPhase.Empty else phase,
                    )
                }
                if (remaining.isNotEmpty()) loadAround(newIndex)
            }
        }
    }

    /** SV-03: chỉ ở video đầu tiên; xáo toàn bộ và chọn seed sao cho video đầu mới khác video đang xem (khi có từ 2 video). */
    private fun reshuffle() {
        val state = currentState
        if (state.phase != ShortsPhase.Ready || state.index != 0) return
        val previousFirst = state.ids.firstOrNull()
        val seed = if (sourceIds.size >= 2) {
            (0 until ShortsConstants.RESHUFFLE_SEED_ATTEMPTS)
                .map { Random.nextLong() }
                .firstOrNull { ordered(sourceIds, it).firstOrNull() != previousFirst }
                ?: Random.nextLong()
        } else {
            Random.nextLong()
        }
        val ids = ordered(sourceIds, seed)
        setState {
            copy(
                ids = ids,
                index = 0,
                seed = seed,
                generation = generation + 1,
                resumePositionMs = 0L,
                startPaused = false,
            )
        }
        shortsLog.i { "[Short] xáo lại ${ids.size} video" }
        loadAround(0)
        sendEffect(ShortsEffect.Reshuffled)
    }

    private fun ordered(source: List<String>, seed: Long): List<String> = source.shuffled(Random(seed))

    private fun loadAround(center: Int) {
        val radius = ShortsConstants.NEIGHBOR_RADIUS
        for (position in (center - radius)..(center + radius)) {
            val id = currentState.ids.getOrNull(position) ?: continue
            if (id in currentState.videos) continue
            viewModelScope.launch {
                val video = getVideo(id) ?: return@launch
                setState { copy(videos = videos + (id to video)) }
            }
        }
    }

    private fun reset() {
        if (currentState.phase == ShortsPhase.Loading && buildJob == null) return
        buildJob?.cancel()
        trackJob?.cancel()
        buildJob = null
        trackJob = null
        sourceIds = emptyList()
        liveIds = emptySet()
        setState { ShortsState(generation = generation) }
    }
}
