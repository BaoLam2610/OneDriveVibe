package com.lambao.odv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.odv.R
import com.lambao.odv.core.designsystem.component.LocalODVNavBarOwnsInset
import com.lambao.odv.core.designsystem.component.ODVNavBadge
import com.lambao.odv.core.designsystem.component.ODVNavBar
import com.lambao.odv.core.designsystem.component.ODVNavItem
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.SecretExpiryNotice
import com.lambao.odv.core.domain.model.ViewerContext
import com.lambao.odv.feature.browser.ODVBrowserScreen
import com.lambao.odv.feature.library.ODVLibraryScreen
import com.lambao.odv.feature.settings.ODVSettingsScreen
import com.lambao.odv.feature.settings.PinPurpose
import com.lambao.odv.feature.shorts.ODVShortsScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn chính với thanh điều hướng đáy (ADR-0023, DH-01 → DH-08, thiet-ke-ui.md mục 4.7, 5.2): các tab Thư mục, Thư viện, Cài đặt (Short
 * thêm ở 8b). Chỉ tab đang chọn được dựng; trạng thái giao diện của tab vừa rời (vị trí cuộn, chip lọc...) được giữ bằng
 * `SaveableStateProvider`, còn trạng thái dữ liệu (thư mục đang mở, từ khóa tìm) nằm trong ViewModel gắn với màn này nên cũng còn
 * nguyên khi quay lại (DH-02). Tab đang chọn lưu bằng `rememberSaveable` nên sống qua xoay màn hình và khi hệ điều hành thu hồi tiến trình.
 *
 * - Back (DH-03): ở tab khác Thư mục thì về Thư mục (tab Thư mục tự xử lý Back lên một cấp và đóng tìm kiếm trước).
 * - Chạm lại tab đang chọn (DH-04): tăng bộ đếm của tab, tab tự cuộn lên đầu.
 * - Thanh ẩn khi bàn phím mở để tìm kiếm (DH-05). Các màn xem, Khóa, màn con của Cài đặt nằm trên back stack của app nên che cả thanh.
 * - `FLAG_SECURE` (DH-08) theo cả cửa sổ chứ không theo tab, nên đổi tab không cần xử lý riêng (xem `ODVSettingsScreen`).
 *
 * [onUpdateSecret] chạy khi bấm "Cập nhật" ở banner hết hạn (D9): chuyển sang tab Cài đặt rồi mở luồng cập nhật. Các callback còn lại của
 * Cài đặt đi thẳng tới back stack của app.
 */
@Composable
fun ODVHomeScreen(
    onOpenSecuritySetup: () -> Unit,
    onOpenPin: (PinPurpose) -> Unit,
    onOpenSecretForm: () -> Unit,
    onDisconnected: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenFile: (DriveItem, ViewerContext) -> Unit = { _, _ -> },
    onUpdateSecret: (requiresPin: Boolean) -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
) {
    val initialTab by viewModel.initialTab.collectAsStateWithLifecycle()
    val start = initialTab
    if (start == null) {
        // DH-06: chờ đọc tab dùng gần nhất (DataStore, vài mili giây) để khỏi nháy sang tab sai.
        Box(modifier.fillMaxSize().background(ODVTheme.colors.bg))
        return
    }
    HomeContent(
        start = start,
        viewModel = viewModel,
        onOpenFile = onOpenFile,
        onUpdateSecret = onUpdateSecret,
        onOpenSecuritySetup = onOpenSecuritySetup,
        onOpenPin = onOpenPin,
        onOpenSecretForm = onOpenSecretForm,
        onDisconnected = onDisconnected,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeContent(
    start: HomeTab,
    viewModel: HomeViewModel,
    onOpenFile: (DriveItem, ViewerContext) -> Unit,
    onUpdateSecret: (requiresPin: Boolean) -> Unit,
    onOpenSecuritySetup: () -> Unit,
    onOpenPin: (PinPurpose) -> Unit,
    onOpenSecretForm: () -> Unit,
    onDisconnected: () -> Unit,
    modifier: Modifier,
) {
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val protectionEnabled by viewModel.protectionEnabled.collectAsStateWithLifecycle()
    val shortAvailable by viewModel.shortAvailable.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf(start) }
    // DH-04: mỗi lần chạm lại tab đang chọn tăng bộ đếm của tab đó; tab nhận qua `reselectSignal` rồi cuộn lên đầu.
    var foldersReselect by rememberSaveable { mutableIntStateOf(0) }
    var libraryReselect by rememberSaveable { mutableIntStateOf(0) }
    var settingsReselect by rememberSaveable { mutableIntStateOf(0) }
    val stateHolder = rememberSaveableStateHolder()

    // DH-06: nhớ tab danh sách dùng gần nhất (Short và Cài đặt bị bỏ qua trong ViewModel).
    LaunchedEffect(selected) { viewModel.onTabSelected(selected) }
    // DH-03: ở gốc tab Thư viện, Short hoặc Cài đặt thì Back về tab Thư mục (giữ nguyên trạng thái Thư mục).
    BackHandler(enabled = selected != HomeTab.Folders) { selected = HomeTab.Folders }

    // DH-01: Short chỉ có trên thanh khi loại Video bật và đồng bộ lần đầu xong; thiếu một trong hai thì còn 3 mục.
    val tabs = remember(shortAvailable) {
        buildList {
            add(HomeTab.Folders)
            add(HomeTab.Library)
            if (shortAvailable == true) add(HomeTab.Short)
            add(HomeTab.Settings)
        }
    }
    // Tắt loại Video (hoặc mất điều kiện hiện Short) khi đang ở tab Short thì về Thư mục. `null` là chưa biết nên không đụng tới.
    LaunchedEffect(shortAvailable, selected) {
        if (shortAvailable == false && selected == HomeTab.Short) selected = HomeTab.Folders
    }
    // DH-05: thanh ẩn khi bàn phím mở (tìm kiếm); lúc đó khung màn hình đệm đáy theo bàn phím như thường.
    val barVisible = !WindowInsets.isImeVisible

    val banner: @Composable () -> Unit = {
        // D9 (CD-06, Q4): banner secret sắp hết hạn ở đầu Danh sách. "Cập nhật" chuyển sang tab Cài đặt rồi mở luồng cập nhật.
        notice?.let {
            SecretExpiryBanner(
                notice = it,
                onUpdate = {
                    selected = HomeTab.Settings
                    onUpdateSecret(protectionEnabled)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }

    Column(modifier.fillMaxSize().background(ODVTheme.colors.bg)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            // Thanh đáy tự đệm inset thanh điều hướng hệ thống nên khung của từng tab không đệm đáy nữa khi thanh đang hiện.
            CompositionLocalProvider(LocalODVNavBarOwnsInset provides barVisible) {
                stateHolder.SaveableStateProvider(selected) {
                    when (selected) {
                        HomeTab.Folders -> ODVBrowserScreen(
                            onOpenFile = onOpenFile,
                            banner = banner,
                            reselectSignal = foldersReselect,
                        )
                        HomeTab.Library -> ODVLibraryScreen(
                            onOpenFile = onOpenFile,
                            onShowFolders = { selected = HomeTab.Folders },
                            banner = banner,
                            reselectSignal = libraryReselect,
                        )
                        HomeTab.Settings -> ODVSettingsScreen(
                            onOpenSecuritySetup = onOpenSecuritySetup,
                            onOpenPin = onOpenPin,
                            onOpenSecretForm = onOpenSecretForm,
                            onDisconnected = onDisconnected,
                            reselectSignal = settingsReselect,
                        )
                        // SH9: nút "Mở Cài đặt" ở trạng thái trống chuyển sang tab Cài đặt để tăng thời lượng tối đa (SV-15).
                        HomeTab.Short -> ODVShortsScreen(onOpenSettings = { selected = HomeTab.Settings })
                    }
                }
            }
        }
        if (barVisible) {
            val items = tabs.mapIndexed { index, tab ->
                tab.navItem(index, tabs.size, selected = tab == selected, notice = notice)
            }
            ODVNavBar(
                items = items,
                selectedIndex = tabs.indexOf(selected).coerceAtLeast(0),
                // SV-10: ở tab Short thanh đáy chuyển sang bản nền tối N4, không phụ thuộc giao diện Sáng/Tối.
                media = selected == HomeTab.Short,
                onSelect = { index ->
                    val tab = tabs[index]
                    if (tab == selected) {
                        when (tab) {
                            HomeTab.Folders -> foldersReselect++
                            HomeTab.Library -> libraryReselect++
                            HomeTab.Settings -> settingsReselect++
                            HomeTab.Short -> Unit
                        }
                    } else {
                        selected = tab
                    }
                },
            )
        }
    }
}

/** Mục trên thanh đáy: nhãn, icon, chấm nhắc secret ở Cài đặt (DH-07) và nhãn TalkBack "Thư mục, tab 1 trên 3, đang chọn". */
@Composable
private fun HomeTab.navItem(index: Int, count: Int, selected: Boolean, notice: SecretExpiryNotice?): ODVNavItem {
    val tab = this
    val label = stringResource(
        when (tab) {
            HomeTab.Folders -> R.string.home_tab_folders
            HomeTab.Library -> R.string.home_tab_library
            HomeTab.Short -> R.string.home_tab_short
            HomeTab.Settings -> R.string.home_tab_settings
        },
    )
    val badge = if (tab == HomeTab.Settings && notice != null) {
        if (notice.isExpired) ODVNavBadge.Danger else ODVNavBadge.Warning
    } else {
        null
    }
    val description = buildString {
        append(stringResource(R.string.home_tab_description, label, index + 1, count))
        if (selected) append(stringResource(R.string.home_tab_selected))
        if (badge != null) append(stringResource(R.string.home_tab_badge))
    }
    return remember(tab, label, description, badge) {
        ODVNavItem(
            label = label,
            icon = when (tab) {
                HomeTab.Folders -> ODVIcon.Folder
                HomeTab.Library -> ODVIcon.Image
                HomeTab.Short -> ODVIcon.Short
                HomeTab.Settings -> ODVIcon.Settings
            },
            contentDescription = description,
            badge = badge,
        )
    }
}
