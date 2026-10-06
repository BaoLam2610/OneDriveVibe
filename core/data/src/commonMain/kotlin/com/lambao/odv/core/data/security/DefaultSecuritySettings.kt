package com.lambao.odv.core.data.security

import com.lambao.odv.core.domain.settings.SecuritySettings

/**
 * Bản tạm của [SecuritySettings] cho tới Lát 9: chưa có màn Cài đặt nên tùy chọn luôn tắt, đúng hành vi trước đây
 * (`wipeAfterFailures = { null }`). Lát 9 thay bằng bản đọc từ DataStore mà không phải sửa `SecurityRepositoryImpl`.
 */
internal class DefaultSecuritySettings : SecuritySettings {
    override suspend fun isWipeOnTooManyFailuresEnabled(): Boolean = false
}
