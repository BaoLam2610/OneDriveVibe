package com.lambao.odv.core.domain.usecase.viewer

import com.lambao.odv.core.domain.model.OriginalImageRef
import com.lambao.odv.core.domain.model.OriginalImageState
import com.lambao.odv.core.domain.repository.OriginalImageRepository
import kotlinx.coroutines.flow.Flow

/** Ảnh gốc của [OriginalImageRef] (AN-01): có sẵn thì phát `Ready` ngay, chưa thì tải kèm tiến trình và tải tiếp phần dở (BN-03). */
class OpenOriginalImageUseCase(
    private val originals: OriginalImageRepository,
) {
    operator fun invoke(ref: OriginalImageRef): Flow<OriginalImageState> = originals.open(ref)
}
