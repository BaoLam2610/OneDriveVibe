package com.lambao.odv.core.domain.usecase.settings

import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.settings.SettingsPreferences
import kotlinx.coroutines.flow.first

/**
 * Bật hoặc tắt một loại tệp hiển thị (CD-01). **Không cho tắt hết**: tắt loại cuối cùng bị từ chối và trả `false`. Tắt không xóa
 * lịch sử xem hay cache của loại đó; bật lại thì hiện lại như cũ. Thay đổi áp dụng ngay vì các màn đọc cùng nguồn.
 */
class ToggleFileKindUseCase(
    private val settings: SettingsPreferences,
) {
    /** `true` nếu đã đổi, `false` nếu bị từ chối vì là loại cuối cùng đang bật. */
    suspend operator fun invoke(kind: MediaKind, enabled: Boolean): Boolean {
        val current = settings.enabledKinds.first()
        val next = if (enabled) current + kind else current - kind
        if (next.isEmpty()) return false
        if (next != current) settings.setEnabledKinds(next)
        return true
    }
}
