package com.lambao.odv.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.domain.hook.ConnectionResetter
import com.lambao.odv.core.domain.model.AppLanguage
import com.lambao.odv.core.domain.platform.AppLocaleController
import kotlinx.coroutines.withContext

/**
 * Chọn ngôn ngữ app bằng `AppCompatDelegate.setApplicationLocales` (CD-10, ADR-0011): áp dụng ngay, không cần khởi động lại app.
 * `MainActivity` là `AppCompatActivity` và tự xử lý thay đổi `locale` trong manifest nên Activity **không bị tạo lại**: AppCompat
 * cập nhật Resources rồi gọi `onConfigurationChanged`, Compose dựng lại chữ qua `LocalConfiguration` (không chớp màn hình). Back
 * stack và ViewModel đương nhiên giữ nguyên. Lựa chọn do hệ điều hành lưu (Android 13+) hoặc `AppLocalesMetadataHolderService` (Android 12 trở xuống),
 * nên không nằm trong DataStore.
 *
 * Ngắt kết nối đưa về "Theo hệ thống" (CD-05, CD-10): đăng ký làm [ConnectionResetter].
 */
internal class AndroidAppLocaleController(
    private val dispatchers: DispatcherProvider,
) : AppLocaleController, ConnectionResetter {

    override fun current(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.size() == 0) return AppLanguage.System
        return when (locales.get(0)?.language) {
            VI -> AppLanguage.Vietnamese
            EN -> AppLanguage.English
            else -> AppLanguage.System
        }
    }

    override fun set(language: AppLanguage) {
        val tags = when (language) {
            AppLanguage.System -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.Vietnamese -> LocaleListCompat.forLanguageTags(VI)
            AppLanguage.English -> LocaleListCompat.forLanguageTags(EN)
        }
        AppCompatDelegate.setApplicationLocales(tags)
    }

    // Phải chạy trên luồng chính; ConnectionResetter có thể được gọi từ dispatcher khác.
    override suspend fun reset() = withContext(dispatchers.main) { set(AppLanguage.System) }

    private companion object {
        const val VI = "vi"
        const val EN = "en"
    }
}
