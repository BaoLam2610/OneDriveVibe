package com.lambao.odv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.model.AccountStatus
import com.lambao.odv.core.domain.model.HomeListTab
import com.lambao.odv.core.domain.model.SecretExpiryNotice
import com.lambao.odv.core.domain.usecase.account.ObserveAccountScopeUseCase
import com.lambao.odv.core.domain.usecase.account.ObserveAccountStatusesUseCase
import com.lambao.odv.core.domain.usecase.account.SelectAccountScopeUseCase
import com.lambao.odv.core.domain.usecase.security.ObserveProtectionUseCase
import com.lambao.odv.core.domain.usecase.settings.ObserveLastListTabUseCase
import com.lambao.odv.core.domain.usecase.settings.ObserveSecretExpiryNoticeUseCase
import com.lambao.odv.core.domain.usecase.settings.SetLastListTabUseCase
import com.lambao.odv.core.domain.usecase.shorts.ObserveShortTabAvailableUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Trạng thái ngoài giao diện của Màn chính (thanh điều hướng đáy, ADR-0023). Gộp từ `HomeBannerViewModel` cũ.
 * - [notice]: có giá trị thì hiện banner "Client Secret sắp hết hạn" (CD-06, Q4, D9) và chấm nhắc trên mục Cài đặt (DH-07).
 * - [protectionEnabled]: nút "Cập nhật" phải qua bước nhập PIN (CD-04) hay vào thẳng form.
 * - [accounts], [scope]: tài khoản kèm trạng thái và phạm vi xem (TA-03, TA-04) cho nút tài khoản trên AppBar và sheet Chọn tài khoản;
 *   [selectScope] đổi phạm vi (nhớ lựa chọn và đưa tài khoản lên đầu hàng đợi đồng bộ).
 * - [initialTab]: tab mở đầu tiên, `null` cho tới khi đọc xong cài đặt (DH-06: tab Thư mục hoặc Thư viện dùng gần nhất, không bao giờ là
 *   Short hay Cài đặt). Chỉ đọc một lần; sau đó tab đang chọn do giao diện giữ (`rememberSaveable`, DH-02).
 */
class HomeViewModel(
    observeNotice: ObserveSecretExpiryNoticeUseCase,
    observeProtection: ObserveProtectionUseCase,
    observeLastListTab: ObserveLastListTabUseCase,
    private val setLastListTab: SetLastListTabUseCase,
    observeShortAvailable: ObserveShortTabAvailableUseCase,
    observeAccountStatuses: ObserveAccountStatusesUseCase,
    observeAccountScope: ObserveAccountScopeUseCase,
    private val selectAccountScope: SelectAccountScopeUseCase,
) : ViewModel() {
    /**
     * Mục Short có hiện trên thanh đáy không (DH-01): loại Video bật và đồng bộ lần đầu xong. `null` là chưa biết (đang đọc cài đặt), để
     * khôi phục tab Short sau khi tiến trình bị thu hồi không bị đẩy về Thư mục oan trước khi luồng này kịp phát giá trị đầu.
     */
    val shortAvailable: StateFlow<Boolean?> =
        observeShortAvailable().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    // U-L8: Eagerly để danh sách tài khoản đã sẵn khi Màn chính dựng (nút tài khoản trên AppBar khỏi hiện trễ gây nhảy bố cục); luồng nhẹ (Room).
    val accounts: StateFlow<List<AccountStatus>> =
        observeAccountStatuses().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Phạm vi xem đã đưa về giá trị hợp lệ (TA-04); `null` là chưa biết (đang đọc config). */
    val scope: StateFlow<AccountScope?> =
        observeAccountScope().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val notice: StateFlow<SecretExpiryNotice?> =
        observeNotice().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val protectionEnabled: StateFlow<Boolean> =
        observeProtection().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val initialTab: StateFlow<HomeTab?> =
        flow { emit(observeLastListTab().first().toHomeTab()) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // U-M1: phạm vi đã áp lên giao diện và bộ đếm epoch nằm ở ViewModel (sống khi Màn chính nằm dưới màn Thêm tài khoản trên back stack),
    // không ở `remember` của HomeContent (mất khi HomeContent rời composition nên lần phát đầu sau khi quay lại bị coi là "lần đầu").
    private var appliedScope: AccountScope? = null
    private val _scopeEpoch = MutableStateFlow(0)

    /** Tăng mỗi lần phạm vi xem đổi so với lần giao diện đã áp (TA-06); nằm trong khóa trạng thái của các tab theo phạm vi. */
    val scopeEpoch: StateFlow<Int> = _scopeEpoch.asStateFlow()

    /**
     * Giao diện báo phạm vi vừa thấy. Trả `true` (và tăng [scopeEpoch]) khi nó khác phạm vi đã áp trước đó; lần đầu tiên không tính là đổi.
     */
    fun onScopeObserved(now: AccountScope): Boolean {
        val before = appliedScope
        appliedScope = now
        if (before == null || before == now) return false
        _scopeEpoch.value += 1
        return true
    }

    /** Chọn phạm vi xem (TA-04). Không hỏi PIN: app đã mở khóa. Việc bỏ trạng thái các tab (TA-06) do giao diện làm khi thấy [scope] đổi. */
    fun selectScope(scope: AccountScope) {
        viewModelScope.launch { selectAccountScope(scope) }
    }

    /** Ghi nhớ tab danh sách vừa dùng (DH-06). Short và Cài đặt không được ghi nhớ. */
    fun onTabSelected(tab: HomeTab) {
        val listTab = tab.toListTab() ?: return
        viewModelScope.launch { setLastListTab(listTab) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private fun HomeListTab.toHomeTab(): HomeTab = when (this) {
    HomeListTab.Folders -> HomeTab.Folders
    HomeListTab.Library -> HomeTab.Library
}

private fun HomeTab.toListTab(): HomeListTab? = when (this) {
    HomeTab.Folders -> HomeListTab.Folders
    HomeTab.Library -> HomeListTab.Library
    HomeTab.Short, HomeTab.Settings -> null
}
