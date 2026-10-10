package com.lambao.odv.core.domain.platform

import com.lambao.odv.core.domain.model.AppLanguage

/**
 * Chọn ngôn ngữ của app (CD-10). Nền tảng cài đặt và tự lưu lựa chọn (Android: `AppCompatDelegate.setApplicationLocales`), nên
 * [set] áp dụng ngay mà không cần khởi động lại app và không làm mất màn đang mở. Gọi trên luồng chính.
 *
 * Ngắt kết nối đưa ngôn ngữ về [AppLanguage.System] (CD-05): bản cài đặt cũng là một `ConnectionResetter`.
 */
interface AppLocaleController {
    /** Lựa chọn hiện tại; [AppLanguage.System] nếu chưa chọn hoặc nền tảng đang dùng ngôn ngữ ngoài VI/EN. */
    fun current(): AppLanguage

    fun set(language: AppLanguage)
}
