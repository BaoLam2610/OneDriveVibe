package com.lambao.odv.core.domain.usecase.shorts

import com.lambao.odv.core.domain.model.ShortVideo
import com.lambao.odv.core.domain.repository.ShortRepository

/** Chi tiết một video của tab Short (tên, thư mục chứa, kích thước hình, cTag cho khóa cache). Null nếu không còn trong Room. */
class GetShortVideoUseCase(
    private val shorts: ShortRepository,
) {
    suspend operator fun invoke(id: String): ShortVideo? = shorts.getVideo(id)
}
