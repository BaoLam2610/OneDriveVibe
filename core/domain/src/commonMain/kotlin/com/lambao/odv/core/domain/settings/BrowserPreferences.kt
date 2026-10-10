package com.lambao.odv.core.domain.settings

import com.lambao.odv.core.domain.model.HomeListTab
import com.lambao.odv.core.domain.model.SortOrder
import com.lambao.odv.core.domain.model.ViewMode
import kotlinx.coroutines.flow.Flow

/**
 * Lựa chọn của tab Thư mục được nhớ giữa các lần mở app: sắp xếp (TM-05) và dạng lưới/danh sách (TM-06). Chỉ là tùy chọn
 * hiển thị, không chứa bí mật. Lát 9 (Cài đặt) sẽ đọc cùng nơi lưu này; chưa có thì giữ mặc định khi đọc lỗi.
 */
interface BrowserPreferences {
    val sortOrder: Flow<SortOrder>
    val viewMode: Flow<ViewMode>

    /** Tab Thư mục hoặc Thư viện dùng gần nhất, để mở app vào đúng tab đó (DH-06). Mặc định [HomeListTab.Folders]. */
    val lastListTab: Flow<HomeListTab>

    suspend fun setSortOrder(order: SortOrder)
    suspend fun setViewMode(mode: ViewMode)
    suspend fun setLastListTab(tab: HomeListTab)
}
