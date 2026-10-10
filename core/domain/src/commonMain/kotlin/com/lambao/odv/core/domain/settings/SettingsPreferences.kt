package com.lambao.odv.core.domain.settings

import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.PdfReadingStyle
import com.lambao.odv.core.domain.model.ThemeMode
import com.lambao.odv.core.domain.model.VideoFit
import kotlinx.coroutines.flow.Flow

/**
 * Tùy chọn do người dùng đặt ở màn Cài đặt (Lát 7), lưu trên máy và bị xóa khi Ngắt kết nối (CD-05). Chỉ là tùy chọn hiển thị,
 * không chứa bí mật; đọc lỗi hoặc giá trị lạ thì dùng mặc định. Ngôn ngữ không nằm ở đây mà ở
 * [com.lambao.odv.core.domain.platform.AppLocaleController] vì hệ điều hành tự lưu và áp dụng (CD-10). Chế độ phát và khung hình
 * "nhớ lần gần nhất" của màn xem video nằm ở `PlayerPreferences`, tab Thư mục ở `BrowserPreferences`.
 *
 * Feature được đọc interface này trực tiếp (như `PlayerPreferences`, ADR-0016 mục Đính chính); luật có logic (vd. không cho tắt
 * hết loại tệp) nằm ở UseCase.
 */
interface SettingsPreferences {
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    /** Loại tệp được hiển thị (CD-01). Luôn có ít nhất một loại; mặc định cả ba. */
    val enabledKinds: Flow<Set<MediaKind>>

    suspend fun setEnabledKinds(kinds: Set<MediaKind>)

    /** Bước tua khi chạm đúp, giây; một trong `VideoSettingOptions.SEEK_STEPS` (mặc định 10). */
    val seekStepSeconds: Flow<Int>

    suspend fun setSeekStepSeconds(seconds: Int)

    /** Tốc độ phát mặc định khi mở video; một trong `VideoSettingOptions.SPEEDS` (mặc định 1x). */
    val defaultSpeed: Flow<Float>

    suspend fun setDefaultSpeed(speed: Float)

    /** Khung hình mặc định; `null` là "nhớ lần gần nhất" (mặc định, VD-06). */
    val defaultVideoFit: Flow<VideoFit?>

    suspend fun setDefaultVideoFit(fit: VideoFit?)

    /** Mở video ở hướng ngang (mặc định dọc, VD-07, VD-19). */
    val openVideoLandscape: Flow<Boolean>

    suspend fun setOpenVideoLandscape(landscape: Boolean)

    /** Nhớ vị trí xem video (mặc định bật). Lát 9 (VD-12) dùng; hiện chỉ lưu. */
    val rememberVideoPosition: Flow<Boolean>

    suspend fun setRememberVideoPosition(enabled: Boolean)

    /** Kiểu đọc PDF (mặc định cuộn dọc). Lát 8 (PD-02) dùng; hiện chỉ lưu. */
    val pdfReadingStyle: Flow<PdfReadingStyle>

    suspend fun setPdfReadingStyle(style: PdfReadingStyle)

    /** Ngày hết hạn Client Secret người dùng tự nhập (epoch day theo giờ địa phương), `null` nếu chưa đặt (CD-06). */
    val secretExpiryEpochDay: Flow<Long?>

    suspend fun setSecretExpiryEpochDay(epochDay: Long?)
}
