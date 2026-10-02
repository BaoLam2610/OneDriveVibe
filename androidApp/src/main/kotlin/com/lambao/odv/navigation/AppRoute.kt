package com.lambao.odv.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Các đích điều hướng (ADR-0003). Mỗi route là một [NavKey] @Serializable để back stack lưu được qua xoay màn hình,
 * đổi ngôn ngữ (CD-10) và khi hệ thống thu hồi tiến trình.
 *
 * Lát 0 chỉ có các màn khởi động và Danh sách, nội dung còn rỗng. Màn xem và Cài đặt thêm khi làm lát tương ứng.
 */
sealed interface AppRoute : NavKey {

    /** Splash (S1). Lát 1 sẽ quyết định đi tiếp Kết nối hay Danh sách tại đây (đặc tả mục 2). */
    @Serializable
    data object Splash : AppRoute

    /** Kết nối OneDrive (KN). */
    @Serializable
    data object Connect : AppRoute

    /** Thiết lập bảo mật (BM). */
    @Serializable
    data object SecuritySetup : AppRoute

    /** Màn Khóa (KH). */
    @Serializable
    data object Lock : AppRoute

    /** Danh sách: tab Thư mục và Thư viện. */
    @Serializable
    data object Home : AppRoute
}
