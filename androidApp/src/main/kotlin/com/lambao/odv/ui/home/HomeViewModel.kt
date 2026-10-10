package com.lambao.odv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.domain.model.HomeListTab
import com.lambao.odv.core.domain.model.SecretExpiryNotice
import com.lambao.odv.core.domain.usecase.security.ObserveProtectionUseCase
import com.lambao.odv.core.domain.usecase.settings.ObserveLastListTabUseCase
import com.lambao.odv.core.domain.usecase.settings.ObserveSecretExpiryNoticeUseCase
import com.lambao.odv.core.domain.usecase.settings.SetLastListTabUseCase
import com.lambao.odv.core.domain.usecase.shorts.ObserveShortTabAvailableUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Trạng thái ngoài giao diện của Màn chính (thanh điều hướng đáy, ADR-0023). Gộp từ `HomeBannerViewModel` cũ.
 * - [notice]: có giá trị thì hiện banner "Client Secret sắp hết hạn" (CD-06, Q4, D9) và chấm nhắc trên mục Cài đặt (DH-07).
 * - [protectionEnabled]: nút "Cập nhật" phải qua bước nhập PIN (CD-04) hay vào thẳng form.
 * - [initialTab]: tab mở đầu tiên, `null` cho tới khi đọc xong cài đặt (DH-06: tab Thư mục hoặc Thư viện dùng gần nhất, không bao giờ là
 *   Short hay Cài đặt). Chỉ đọc một lần; sau đó tab đang chọn do giao diện giữ (`rememberSaveable`, DH-02).
 */
class HomeViewModel(
    observeNotice: ObserveSecretExpiryNoticeUseCase,
    observeProtection: ObserveProtectionUseCase,
    observeLastListTab: ObserveLastListTabUseCase,
    private val setLastListTab: SetLastListTabUseCase,
    observeShortAvailable: ObserveShortTabAvailableUseCase,
) : ViewModel() {
    /**
     * Mục Short có hiện trên thanh đáy không (DH-01): loại Video bật và đồng bộ lần đầu xong. `null` là chưa biết (đang đọc cài đặt), để
     * khôi phục tab Short sau khi tiến trình bị thu hồi không bị đẩy về Thư mục oan trước khi luồng này kịp phát giá trị đầu.
     */
    val shortAvailable: StateFlow<Boolean?> =
        observeShortAvailable().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val notice: StateFlow<SecretExpiryNotice?> =
        observeNotice().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val protectionEnabled: StateFlow<Boolean> =
        observeProtection().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val initialTab: StateFlow<HomeTab?> =
        flow { emit(observeLastListTab().first().toHomeTab()) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

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
