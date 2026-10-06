package com.lambao.odv.core.domain.usecase.sync

import com.lambao.odv.core.domain.repository.SyncRepository

/** Yêu cầu đồng bộ nếu quét lần đầu chưa xong hoặc lần trước đã quá 15 phút (DS-04). Không chờ kết quả; gọi khi đang đồng bộ thì bỏ qua. */
class SyncIfStaleUseCase(
    private val sync: SyncRepository,
) {
    operator fun invoke() = sync.syncIfStale()
}
