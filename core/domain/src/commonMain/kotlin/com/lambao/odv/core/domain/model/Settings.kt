package com.lambao.odv.core.domain.model

/** Giao diện Sáng/Tối (CD, mục Hiển thị). [System] theo cài đặt máy. */
enum class ThemeMode { System, Light, Dark }

/** Ngôn ngữ của app (CD-10). [System] theo máy; máy dùng ngôn ngữ khác VI/EN thì app hiện Tiếng Việt. */
enum class AppLanguage { System, Vietnamese, English }

/**
 * Thông tin kết nối hiển thị ở nhóm Kết nối của Cài đặt (CD, mục Kết nối). Tenant ID và Client ID đã che bớt (vd. `a1b2••••9f0e`),
 * **không bao giờ chứa Client Secret**. [lastSyncedAt] là mốc (epoch mili giây) lần đồng bộ trọn vẹn gần nhất, null nếu chưa có.
 *
 * [toString] che toàn bộ để UPN không lọt vào log (CH-06); đừng bỏ override này.
 */
data class ConnectionInfo(
    val upn: String,
    val tenantIdMasked: String,
    val clientIdMasked: String,
    val lastSyncedAt: Long?,
) {
    override fun toString(): String = "ConnectionInfo(***)"
}

/** Kiểu đọc PDF (PD-02, CD mục PDF): cuộn dọc liên tục hoặc lật trang ngang. Mặc định [Vertical]. */
enum class PdfReadingStyle { Vertical, Horizontal }

/**
 * Các lựa chọn cố định của nhóm Video trong Cài đặt (CD mục Video). Nằm ở domain vì cả Cài đặt (liệt kê lựa chọn) lẫn màn xem
 * video (đọc giá trị) cùng dùng, và luật "chỉ nhận giá trị nằm trong danh sách" thuộc về nghiệp vụ.
 */
object VideoSettingOptions {
    /** Bước tua khi chạm đúp (giây). */
    val SEEK_STEPS: List<Int> = listOf(5, 10, 15)
    const val DEFAULT_SEEK_STEP = 10

    /** Tốc độ phát (VD-05); cũng là danh sách ở bảng Tốc độ của màn xem. */
    val SPEEDS: List<Float> = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    const val DEFAULT_SPEED = 1f
}
