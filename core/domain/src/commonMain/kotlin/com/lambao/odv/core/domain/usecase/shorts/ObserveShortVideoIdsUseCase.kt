package com.lambao.odv.core.domain.usecase.shorts

import com.lambao.odv.core.domain.repository.ShortRepository
import kotlinx.coroutines.flow.Flow

/** Id các video vào tab Short với thời lượng tối đa [maxMinutes] phút (SV-01, CD-13); phát lại khi đồng bộ đổi dữ liệu (SV-16). */
class ObserveShortVideoIdsUseCase(
    private val shorts: ShortRepository,
) {
    operator fun invoke(maxMinutes: Int): Flow<List<String>> = shorts.observeVideoIds(maxMinutes * MILLIS_PER_MINUTE)

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
