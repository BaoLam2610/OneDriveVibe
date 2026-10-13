package com.lambao.odv.core.domain.usecase.settings

import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.settings.SettingsPreferences

/**
 * Bật hoặc tắt một loại tệp hiển thị (CD-01). **Không cho tắt hết**: tắt loại cuối cùng bị từ chối và trả `false`. Tắt không xóa
 * lịch sử xem hay cache của loại đó; bật lại thì hiện lại như cũ. Thay đổi áp dụng ngay vì các màn đọc cùng nguồn.
 */
class ToggleFileKindUseCase(
    private val settings: SettingsPreferences,
) {
    /** `true` nếu đã đổi, `false` nếu bị từ chối vì là loại cuối cùng đang bật. */
    suspend operator fun invoke(kind: MediaKind, enabled: Boolean): Boolean =
        // Đọc-sửa-ghi nguyên tử trong một lần edit của kho (chạm nhanh hai chip không mất cập nhật); luật CD-01 nằm trong transform.
        settings.updateEnabledKinds { current ->
            val next = if (enabled) current + kind else current - kind
            next.takeIf { it.isNotEmpty() }
        }
}
