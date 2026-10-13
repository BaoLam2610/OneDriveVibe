package com.lambao.odv.core.domain.settings

import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.PdfReadingStyle
import com.lambao.odv.core.domain.model.ResumeBehavior
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

    /**
     * Đọc rồi ghi **nguyên tử** (một lần `edit`) tập loại tệp, để hai lần chạm chip nhanh không ghi đè lên nhau (đọc-sửa-ghi rời rạc
     * sẽ mất cập nhật). [transform] nhận tập hiện tại và trả tập mới, hoặc `null` để từ chối (vd. tắt loại cuối, CD-01). Trả `true`
     * nếu [transform] chấp nhận (kể cả khi tập không đổi), `false` nếu từ chối hoặc trả tập rỗng.
     */
    suspend fun updateEnabledKinds(transform: (Set<MediaKind>) -> Set<MediaKind>?): Boolean

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

    /**
     * Thời lượng tối đa của video vào tab Short, phút (CD-13, SV-01); từ `SHORT_MIN_MINUTES` đến `SHORT_MAX_MINUTES`, mặc định 3.
     * Thuộc nhóm cài đặt nên bị xóa về mặc định khi Ngắt kết nối (CD-05).
     */
    val shortMaxMinutes: Flow<Int>

    suspend fun setShortMaxMinutes(minutes: Int)

    /**
     * Khi mở lại video đã xem dở (VD-12, Lát 10): hỏi trước, luôn xem tiếp hoặc luôn phát từ đầu (mặc định hỏi trước). Thay công tắc
     * "Nhớ vị trí xem" cũ; vị trí xem luôn được lưu vì dải "Xem tiếp" cần nó.
     */
    val videoResumeBehavior: Flow<ResumeBehavior>

    suspend fun setVideoResumeBehavior(behavior: ResumeBehavior)

    /** Kiểu đọc PDF (mặc định cuộn dọc). Lát 8 (PD-02) dùng; hiện chỉ lưu. */
    val pdfReadingStyle: Flow<PdfReadingStyle>

    suspend fun setPdfReadingStyle(style: PdfReadingStyle)

    /** Ngày hết hạn Client Secret người dùng tự nhập (epoch day theo giờ địa phương), `null` nếu chưa đặt (CD-06). */
    val secretExpiryEpochDay: Flow<Long?>

    suspend fun setSecretExpiryEpochDay(epochDay: Long?)
}
