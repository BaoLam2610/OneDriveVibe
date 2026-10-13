package com.lambao.odv.core.domain.settings

import com.lambao.odv.core.domain.model.AccountScope
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

    /**
     * Phạm vi xem đã chọn (TA-04); null = chưa chọn. Giá trị thô: **chưa** kiểm tra tài khoản còn tồn tại, `AccountRepository.observeScope`
     * làm việc đó. Bị xóa cùng các tùy chọn khác khi ngắt kết nối (CD-05).
     */
    val accountScope: Flow<AccountScope?>

    suspend fun setSortOrder(order: SortOrder)
    suspend fun setViewMode(mode: ViewMode)
    suspend fun setLastListTab(tab: HomeListTab)
    suspend fun setAccountScope(scope: AccountScope)
}
