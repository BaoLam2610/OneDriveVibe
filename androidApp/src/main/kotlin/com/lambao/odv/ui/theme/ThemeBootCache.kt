package com.lambao.odv.ui.theme

import android.content.Context
import androidx.annotation.StyleRes
import com.lambao.odv.R
import com.lambao.odv.core.domain.model.ThemeMode
import androidx.core.content.edit

/**
 * Bản sao giao diện Sáng/Tối của app để đọc ĐỒNG BỘ lúc khởi động. Giá trị thật nằm ở DataStore (đọc bất đồng bộ, vài mili giây đầu chưa có),
 * nên nếu chỉ dựa vào đó thì khung đầu tiên vẽ theo giao diện của MÁY rồi mới đổi sang giao diện đã chọn trong app (lỗi 2026-10-13: máy tối,
 * app sáng thì thấy nền tối rồi mới sáng). Bản sao này phục vụ hai việc: màu khởi đầu của Compose (`ODVApp`) và theme cửa sổ XML
 * ([windowTheme]) cùng theme splash hệ thống ([MainActivity]).
 *
 * Chỉ là bộ đệm khởi động của tầng giao diện, không phải nguồn dữ liệu: `MainActivity` ghi lại mỗi khi DataStore phát giá trị mới, nên
 * sau khi Ngắt kết nối xóa cài đặt (CD-05) bản sao cũng về "Theo hệ thống". Không chứa bí mật.
 */
object ThemeBootCache {
    private const val FILE = "odv_boot"
    private const val KEY_THEME_MODE = "theme_mode"

    fun read(context: Context): ThemeMode {
        val name = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_THEME_MODE, null)
        return ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.System
    }

    fun write(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit {
            putString(
                KEY_THEME_MODE,
                mode.name
            )
        }
    }

    /**
     * Theme cửa sổ ép theo giao diện của app (nền cửa sổ Sáng hoặc Tối cố định, không theo giao diện của máy). `null` với [ThemeMode.System]:
     * giữ theme mặc định, vốn đã theo máy qua `values-night`.
     */
    @StyleRes
    fun windowTheme(mode: ThemeMode): Int? = when (mode) {
        ThemeMode.System -> null
        ThemeMode.Light -> R.style.Theme_OneDriveVibe_Light
        ThemeMode.Dark -> R.style.Theme_OneDriveVibe_Dark
    }
}
