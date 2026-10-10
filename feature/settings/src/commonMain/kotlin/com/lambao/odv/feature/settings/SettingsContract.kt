package com.lambao.odv.feature.settings

import com.lambao.odv.core.domain.model.AppLanguage
import com.lambao.odv.core.domain.model.AutoLockDelay
import com.lambao.odv.core.domain.model.BiometricStatus
import com.lambao.odv.core.domain.model.CachePolicy
import com.lambao.odv.core.domain.model.CacheShares
import com.lambao.odv.core.domain.model.CacheUsage
import com.lambao.odv.core.domain.model.ConnectionInfo
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.model.PdfReadingStyle
import com.lambao.odv.core.domain.model.PlayMode
import com.lambao.odv.core.domain.model.SecretExpiryNotice
import com.lambao.odv.core.domain.model.ThemeMode
import com.lambao.odv.core.domain.model.VideoFit
import com.lambao.odv.core.domain.model.VideoSettingOptions

/** Bảng chọn (OptionRow trong sheet, không có nút Lưu) đang mở. */
enum class SettingsSheet { Theme, Language, AutoLock, SeekStep, Speed, VideoFit, Orientation, PlayMode, PdfStyle, CacheLimit, CacheShares, SecretExpiry }

/** Hộp thoại xác nhận đang mở ở màn Cài đặt (các hộp thoại sau bước nhập PIN nằm trong màn PIN, xem [PinFlowState]). */
enum class SettingsDialog {
    /** D2: bật "Xóa dữ liệu khi nhập sai quá nhiều" (CD-08), bấm tiếp thì sang bước nhập PIN. */
    WipeOnFailures,

    /** D5: xóa toàn bộ bộ nhớ đệm (lịch sử xem được giữ lại). */
    ClearCache,

    /** D6: giảm giới hạn xuống [SettingsState.pendingLimitGb] khi dung lượng đang dùng vượt giới hạn mới (CD-07), dọn ngay. */
    ShrinkCache,

    /** D3: Ngắt kết nối, bước 1 (CD-05). */
    DisconnectStep1,

    /** D4: Ngắt kết nối, bước 2: xóa toàn bộ dữ liệu trên máy (CD-05). */
    DisconnectStep2,
}

/**
 * Việc cần xác nhận bằng PIN hiện tại trước khi làm (CD-03, CD-08, CD-09; 7e thêm cập nhật Client Secret, CD-04). Tên lưu trong route
 * điều hướng nên **không đổi tên** giá trị đã có.
 */
enum class PinPurpose { DisableProtection, EnableWipe, ChangePin, UpdateSecret }

/**
 * Trạng thái màn Cài đặt (CD). Lát 7a: Hiển thị (Giao diện, Ngôn ngữ) và Kết nối chỉ đọc. Lát 7b: Loại tệp, nhóm Video và PDF.
 * Bảo mật, Bộ nhớ đệm và phần còn lại của Kết nối thêm ở 7c → 7e.
 */
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.System,
    val language: AppLanguage = AppLanguage.System,
    /** Null khi chưa đọc được config (đang tải hoặc app khóa): nhóm Kết nối chưa hiện. */
    val connection: ConnectionInfo? = null,
    val sheet: SettingsSheet? = null,
    /** Loại tệp hiển thị (CD-01); luôn có ít nhất một loại. */
    val enabledKinds: Set<MediaKind> = MediaKind.entries.toSet(),
    val seekStepSeconds: Int = VideoSettingOptions.DEFAULT_SEEK_STEP,
    val defaultSpeed: Float = VideoSettingOptions.DEFAULT_SPEED,
    /** Null là "nhớ lần gần nhất" (VD-06). */
    val defaultVideoFit: VideoFit? = null,
    val openVideoLandscape: Boolean = false,
    val playMode: PlayMode = PlayMode.AutoNext,
    val rememberVideoPosition: Boolean = true,
    val pdfReadingStyle: PdfReadingStyle = PdfReadingStyle.Vertical,
    /** Đang bật bảo vệ bằng PIN (CD-02, CD-03). Tắt thì các hàng phụ thuộc PIN trong nhóm Bảo mật không hiện. */
    val protectionEnabled: Boolean = false,
    /** Máy có sinh trắc học mạnh dùng được và app đã bật chưa (CD-02). */
    val biometric: BiometricStatus = BiometricStatus(available = false, enabled = false),
    val autoLockDelay: AutoLockDelay = AutoLockDelay.OneMinute,
    /** Đã bật "Xóa dữ liệu khi nhập sai quá nhiều" (CD-08). */
    val wipeOnFailures: Boolean = false,
    val dialog: SettingsDialog? = null,
    /** "Bảo vệ màn hình": FLAG_SECURE toàn app (độc lập với PIN). */
    val screenProtection: Boolean = false,
    /** Giới hạn chung của bộ nhớ đệm, GB (từ 1 đến 10) và tỉ lệ chia theo loại (tổng 100). */
    val cacheLimitGb: Int = CachePolicy.DEFAULT_LIMIT_GB,
    val cacheShares: CacheShares = CacheShares.Default,
    /** Dung lượng bộ nhớ đệm đang dùng theo loại; đo lại khi mở màn và sau mỗi thao tác. */
    val cacheUsage: CacheUsage = CacheUsage(),
    /** Giới hạn mới đang chờ xác nhận ở hộp thoại D6. */
    val pendingLimitGb: Int? = null,
    /** Ngày hết hạn Client Secret người dùng đã đặt (epoch day theo giờ địa phương), null nếu chưa (CD-06). */
    val secretExpiryEpochDay: Long? = null,
    /** Có thì hiện banner "Client Secret hết hạn sau N ngày" ở đầu màn (C7, CD-06). */
    val expiryNotice: SecretExpiryNotice? = null,
    /** Đang xóa dữ liệu khi Ngắt kết nối: khóa nút để không bấm đôi (CD-05). */
    val isDisconnecting: Boolean = false,
) {
    /** Chỉ còn một loại tệp bật: chip đó không tắt được và hiện cảnh báo "Phải bật ít nhất 1 loại tệp" (CD-01). */
    val onlyOneKindLeft: Boolean get() = enabledKinds.size == 1
}

sealed interface SettingsIntent {
    data class ShowSheet(val sheet: SettingsSheet) : SettingsIntent

    data object DismissSheet : SettingsIntent

    /** Chạm một lựa chọn trong sheet Giao diện: đặt giá trị và đóng sheet ngay (không có nút Lưu). */
    data class SelectTheme(val mode: ThemeMode) : SettingsIntent

    /** Chạm một lựa chọn trong sheet Ngôn ngữ (CD-10): áp dụng ngay, không khởi động lại app. */
    data class SelectLanguage(val language: AppLanguage) : SettingsIntent

    /** Chạm một chip Loại tệp (CD-01): đảo bật/tắt; bị từ chối nếu là loại cuối cùng đang bật. */
    data class ToggleKind(val kind: MediaKind) : SettingsIntent

    data class SelectSeekStep(val seconds: Int) : SettingsIntent

    data class SelectSpeed(val speed: Float) : SettingsIntent

    /** [fit] null là "nhớ lần gần nhất". */
    data class SelectVideoFit(val fit: VideoFit?) : SettingsIntent

    data class SelectOrientation(val landscape: Boolean) : SettingsIntent

    data class SelectPlayMode(val mode: PlayMode) : SettingsIntent

    data class SetRememberPosition(val enabled: Boolean) : SettingsIntent

    data class SelectPdfStyle(val style: PdfReadingStyle) : SettingsIntent

    /** Màn mở (lại) lên: đọc lại trạng thái sinh trắc học vì có thể vừa đổi ở màn thiết lập hay đổi PIN (CD-09 tắt sinh trắc học). */
    data object Refresh : SettingsIntent

    /** Công tắc "Bảo vệ ứng dụng": bật thì sang luồng thiết lập PIN (CD-02), tắt thì phải nhập PIN hiện tại (CD-03). */
    data class ToggleProtection(val enable: Boolean) : SettingsIntent

    data object ChangePin : SettingsIntent

    data class ToggleBiometric(val enable: Boolean) : SettingsIntent

    data class SelectAutoLock(val delay: AutoLockDelay) : SettingsIntent

    /** Bật thì hiện cảnh báo D2 rồi nhập PIN (CD-08); tắt thì tắt ngay. */
    data class ToggleWipe(val enable: Boolean) : SettingsIntent

    /** D2, nút "Nhập PIN để bật". */
    data object ConfirmWipeDialog : SettingsIntent

    data object DismissDialog : SettingsIntent

    data class SetScreenProtection(val enabled: Boolean) : SettingsIntent

    /** Bảng giới hạn chung, nút "Áp dụng". Dung lượng đang dùng vượt giới hạn mới thì hỏi D6 trước (CD-07). */
    data class ApplyCacheLimit(val gb: Int) : SettingsIntent

    data object ConfirmShrinkCache : SettingsIntent

    /** Bảng tỉ lệ chia, nút "Áp dụng": [shares] đã tổng 100 (do `CacheShares.withShare` bảo đảm). */
    data class ApplyCacheShares(val shares: CacheShares) : SettingsIntent

    /** Hàng "Xóa bộ nhớ đệm": hỏi D5. */
    data object AskClearCache : SettingsIntent

    data object ConfirmClearCache : SettingsIntent

    /** Hàng "Cập nhật Client Secret" (CD-04): bảo mật bật thì xác thực lại bằng PIN (P1) rồi mới tới form, tắt thì vào form luôn. */
    data object UpdateSecret : SettingsIntent

    /** Sheet "Ngày hết hạn secret" (S4), nút "Lưu": [epochDay] null là xóa ngày (CD-06). */
    data class SaveSecretExpiry(val epochDay: Long?) : SettingsIntent

    /** Hàng "Ngắt kết nối": hiện D3 (CD-05). */
    data object AskDisconnect : SettingsIntent

    /** D3, nút "Tiếp tục": sang D4. */
    data object ContinueDisconnect : SettingsIntent

    /** D4, nút xác nhận cuối: xóa toàn bộ dữ liệu rồi về màn Kết nối. */
    data object ConfirmDisconnect : SettingsIntent
}

/** Điều hướng một lần ra khỏi màn Cài đặt (route do app sở hữu, ADR-0003). 7e sẽ thêm cập nhật secret và ngắt kết nối. */
sealed interface SettingsEffect {
    /** Bật bảo vệ: sang màn Thiết lập bảo mật (BM, CD-02) và quay lại Cài đặt khi xong. */
    data object OpenSecuritySetup : SettingsEffect

    data class OpenPin(val purpose: PinPurpose) : SettingsEffect

    /** Bảo vệ đang tắt: vào thẳng form cập nhật Client Secret (CD-04). */
    data object OpenSecretForm : SettingsEffect

    /** Form cập nhật Client Secret vừa lưu xong: hiện Snackbar S5 "Đã cập nhật Client Secret". */
    data object ShowSecretSaved : SettingsEffect

    /** Đã ngắt kết nối và xóa dữ liệu (CD-05): app về màn Kết nối với back stack sạch. */
    data object Disconnected : SettingsEffect
}
