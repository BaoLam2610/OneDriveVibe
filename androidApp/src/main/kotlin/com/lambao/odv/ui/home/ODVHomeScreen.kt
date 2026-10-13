package com.lambao.odv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.odv.R
import com.lambao.odv.core.designsystem.component.LocalODVNavBarOwnsInset
import com.lambao.odv.core.designsystem.component.ODVAccountButton
import com.lambao.odv.core.designsystem.component.ODVSnackbarHost
import com.lambao.odv.core.designsystem.component.ODVNavBadge
import com.lambao.odv.core.designsystem.component.ODVNavBar
import com.lambao.odv.core.designsystem.component.ODVNavItem
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVDuration
import com.lambao.odv.core.designsystem.theme.ODVEasing
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.designsystem.theme.ODVTransitions
import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.core.domain.model.SecretExpiryNotice
import com.lambao.odv.core.domain.model.ViewerContext
import com.lambao.odv.feature.browser.ODVBrowserScreen
import com.lambao.odv.feature.library.ODVLibraryScreen
import com.lambao.odv.feature.settings.AccountEvents
import com.lambao.odv.feature.settings.ODVSettingsScreen
import com.lambao.odv.feature.settings.PinPurpose
import com.lambao.odv.feature.shorts.LocalODVShortSeekGuard
import com.lambao.odv.feature.shorts.ODVShortSeekGuard
import com.lambao.odv.feature.shorts.ODVShortSeekGuardStrip
import com.lambao.odv.feature.shorts.ODVShortsScreen
import com.lambao.odv.ui.splash.ODVSplashContent
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn chính với thanh điều hướng đáy (ADR-0023, DH-01 → DH-08, thiet-ke-ui.md mục 4.7, 5.2): các tab Thư mục, Thư viện, Short, Cài đặt (Short
 * chỉ hiện khi bật Video và đã đồng bộ xong lần đầu, DH-01). Chỉ tab đang chọn được dựng; trạng thái giao diện của tab vừa rời (vị trí cuộn, chip lọc...) được giữ bằng
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
    onOpenAddAccount: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenFile: (DriveItem, ViewerContext) -> Unit = { _, _ -> },
    onUpdateSecret: (requiresPin: Boolean) -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
) {
    val initialTab by viewModel.initialTab.collectAsStateWithLifecycle()
    val start = initialTab
    if (start == null) {
        // DH-06: chờ đọc tab dùng gần nhất (DataStore, vài mili giây) để khỏi nháy sang tab sai. Vẽ hình Splash (trùng với màn trước nó) để
        // chuyển từ Splash sang Màn chính liền mạch, không xen một khung nền trơn.
        ODVSplashContent(modifier)
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
        onOpenAddAccount = onOpenAddAccount,
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
    onOpenAddAccount: () -> Unit,
    modifier: Modifier,
) {
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val scope by viewModel.scope.collectAsStateWithLifecycle()
    val protectionEnabled by viewModel.protectionEnabled.collectAsStateWithLifecycle()
    val shortAvailable by viewModel.shortAvailable.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf(start) }
    // DH-04: mỗi lần chạm lại tab đang chọn tăng bộ đếm của tab đó; tab nhận qua `reselectSignal` rồi cuộn lên đầu.
    var foldersReselect by rememberSaveable { mutableIntStateOf(0) }
    var libraryReselect by rememberSaveable { mutableIntStateOf(0) }
    var settingsReselect by rememberSaveable { mutableIntStateOf(0) }
    var shortReselect by rememberSaveable { mutableIntStateOf(0) }
    val stateHolder = rememberSaveableStateHolder()

    // Nhiều tài khoản (Lát T, TA-03, TA-04, TA-06): sheet Chọn tài khoản, Snackbar và việc bỏ trạng thái các tab khi phạm vi xem đổi.
    var showAccountPicker by rememberSaveable { mutableStateOf(false) }
    // Tăng mỗi lần phạm vi đổi: nằm trong khóa của SaveableStateProvider nên trạng thái giao diện cũ (cuộn, chip lọc) của Thư mục, Thư viện, Short
    // bị bỏ; Cài đặt giữ nguyên (TA-06). Trạng thái dữ liệu nằm trong ViewModel của từng tab, tự bỏ khi thấy phạm vi đổi.
    // U-M1: epoch và phạm vi đã áp nằm ở HomeViewModel để sống qua việc HomeContent rời composition (màn Thêm tài khoản che Màn chính).
    val scopeEpoch by viewModel.scopeEpoch.collectAsStateWithLifecycle()
    LaunchedEffect(scope) {
        val now = scope ?: return@LaunchedEffect
        val oldEpoch = viewModel.scopeEpoch.value
        if (viewModel.onScopeObserved(now)) {
            // U-L7: bỏ trạng thái giao diện đã lưu của epoch trước cho các tab theo phạm vi (khóa mới sẽ không bao giờ dùng lại chúng).
            listOf(HomeTab.Folders, HomeTab.Library, HomeTab.Short).forEach { stateHolder.removeState("${it.name}#$oldEpoch") }
            // TA-06 bước 3: đang ở Thư mục, Thư viện hoặc Short thì về gốc tab Thư mục; đang ở Cài đặt thì ở lại.
            if (selected != HomeTab.Settings) selected = HomeTab.Folders
        }
    }
    val snackbarHost = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    // U-L6: LocalResources (theo cấu hình hiện tại); rememberUpdatedState để effect dài hạn luôn dùng ngôn ngữ mới nhất.
    val resources by rememberUpdatedState(LocalResources.current)
    val accountEvents = koinInject<AccountEvents>()
    LaunchedEffect(accountEvents) {
        // TA-07: màn Thêm tài khoản đã đóng; hiện "Đã thêm …" ở đây vì từ giờ phạm vi xem là tài khoản mới.
        accountEvents.added.collect { name -> snackbarHost.showSnackbar(resources.getString(R.string.account_added, name)) }
    }
    // Nút tài khoản trên AppBar (TA-03): chỉ khi đã có tài khoản; avatar gộp ở phạm vi Tất cả; chấm lỗi theo TA-10.
    val accountButton: @Composable () -> Unit = {
        if (accounts.isNotEmpty() && scope != null) {
            val aggregate = scope == AccountScope.All
            val current = accounts.firstOrNull { (scope as? AccountScope.One)?.accountId == it.account.id } ?: accounts.first()
            val hasError = if (aggregate) accounts.any { it.inaccessible } else current.inaccessible
            val base = if (aggregate) {
                stringResource(R.string.account_button_description_all)
            } else {
                stringResource(R.string.account_button_description, current.account.displayName, current.account.email)
            }
            ODVAccountButton(
                name = current.account.displayName,
                contentDescription = if (hasError) base + stringResource(R.string.account_button_has_error) else base,
                onClick = { showAccountPicker = true },
                aggregate = aggregate,
                hasError = hasError,
            )
        }
    }

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

    val reduceMotion = ODVTheme.motion.reduceMotion
    val banner: @Composable () -> Unit = {
        // D9 (CD-06, Q4): banner secret sắp hết hạn ở đầu Danh sách. "Cập nhật" chuyển sang tab Cài đặt rồi mở luồng cập nhật.
        // Trượt vào / mờ đi như banner đồng bộ (mục 2.8); khóa chỉ là có hay không có thông báo, số ngày đổi thì không chạy lại hoạt ảnh.
        AnimatedContent(
            targetState = notice,
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = { ODVTransitions.banner(reduceMotion) },
            contentKey = { it != null },
            label = "secretExpiryBanner",
        ) { shown ->
            if (shown != null) {
                SecretExpiryBanner(
                    notice = shown,
                    onUpdate = {
                        selected = HomeTab.Settings
                        onUpdateSecret(protectionEnabled)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }

    // Cầu nối để thanh tua của Short ăn luôn dải trên cùng của thanh đáy, tránh chạm hụt rơi vào tab (xem ODVShortSeekGuard).
    val seekGuard = remember { ODVShortSeekGuard() }
    CompositionLocalProvider(LocalODVShortSeekGuard provides seekGuard) {
    Column(modifier.fillMaxSize().background(ODVTheme.colors.bg)) {
        // zIndex: vẽ vùng nội dung đè lên thanh đáy để núm thanh tua của Short (tâm ở rãnh sát đường tiếp xúc, tràn xuống vài dp) không bị
        // thanh đáy che. Các tab khác không vẽ ra ngoài vùng của mình nên không bị ảnh hưởng.
        Box(Modifier.weight(1f).fillMaxWidth().zIndex(1f)) {
            // Thanh đáy tự đệm inset thanh điều hướng hệ thống nên khung của từng tab không đệm đáy nữa khi thanh đang hiện.
            CompositionLocalProvider(LocalODVNavBarOwnsInset provides barVisible) {
                // Đổi tab: nội dung cross-fade `fast`, không trượt ngang (mục 2.8). Trong lúc chuyển cả hai tab cùng được dựng; riêng cặp có tab
                // Short thì đổi ngay, không hoạt ảnh, để ExoPlayer và thanh hệ thống của Short không chạy chồng lên tab kia (ADR-0023).
                // Nội dung dựng theo `tab` của AnimatedContent, không theo `selected`, nếu không tab đang mờ đi sẽ bị vẽ thành tab mới.
                AnimatedContent(
                    targetState = selected,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        if (initialState == HomeTab.Short || targetState == HomeTab.Short) ODVTransitions.none() else ODVTransitions.crossFade()
                    },
                    label = "homeTab",
                ) { tab ->
                    // Khóa có thêm `scopeEpoch` cho các tab theo phạm vi xem (TA-06); Cài đặt giữ khóa cũ để không mất vị trí cuộn.
                    stateHolder.SaveableStateProvider(if (tab == HomeTab.Settings) tab.name else "${tab.name}#$scopeEpoch") {
                        when (tab) {
                            HomeTab.Folders -> ODVBrowserScreen(
                                onOpenFile = onOpenFile,
                                banner = banner,
                                accountButton = accountButton,
                                reselectSignal = foldersReselect,
                                onShowMessage = { message -> coroutineScope.launch { snackbarHost.showSnackbar(message) } },
                            )
                            HomeTab.Library -> ODVLibraryScreen(
                                onOpenFile = onOpenFile,
                                onShowFolders = { selected = HomeTab.Folders },
                                banner = banner,
                                accountButton = accountButton,
                                reselectSignal = libraryReselect,
                            )
                            HomeTab.Settings -> ODVSettingsScreen(
                                onOpenSecuritySetup = onOpenSecuritySetup,
                                onOpenPin = onOpenPin,
                                onOpenSecretForm = onOpenSecretForm,
                                onDisconnected = onDisconnected,
                                onOpenAccountPicker = { showAccountPicker = true },
                                onOpenAddAccount = onOpenAddAccount,
                                reselectSignal = settingsReselect,
                            )
                            // SH9: nút "Mở Cài đặt" ở trạng thái trống chuyển sang tab Cài đặt để tăng thời lượng tối đa (SV-15).
                            HomeTab.Short -> ODVShortsScreen(
                                onOpenSettings = { selected = HomeTab.Settings },
                                reselectSignal = shortReselect,
                            )
                        }
                    }
                }
            }
            // Snackbar "Đang xem …" / "Đã thêm …": nằm ở đáy vùng nội dung nên cách mép trên thanh đáy 16 và không đè lên thanh (thiet-ke-ui.md
            // mục 4.2); phủ lên nội dung chứ không đẩy nó.
            ODVSnackbarHost(snackbarHost, Modifier.align(Alignment.BottomCenter))
        }
        // Bàn phím đóng thì thanh đáy fade in `fast`. Chiều ra (bàn phím mở) giữ tức thì: hoạt ảnh thoát sẽ giữ thanh trong khung thêm 150ms
        // trong lúc bàn phím đã đẩy nội dung lên, làm bố cục giật.
        AnimatedVisibility(
            visible = barVisible,
            enter = fadeIn(tween(ODVDuration.fast, easing = ODVEasing.easeOut)),
            exit = ExitTransition.None,
        ) {
            val items = tabs.mapIndexed { index, tab ->
                tab.navItem(index, tabs.size, selected = tab == selected, notice = notice)
            }
            Box {
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
                                // DH-04: Short cuộn mượt về video đầu rồi xáo lại khi đang ở video không phải đầu.
                                HomeTab.Short -> shortReselect++
                            }
                        } else {
                            selected = tab
                        }
                    },
                )
                // SV-07 (2026-10-11): ở tab Short, dải đầu thanh đáy thuộc về thanh tua; đặt sau thanh đáy để nhận chạm trước.
                if (selected == HomeTab.Short) ODVShortSeekGuardStrip(Modifier.align(Alignment.TopCenter))
            }
        }
    }
    }

    // AC1 đến AC3 (TA-04): sheet phủ lên cả thanh đáy. Chạm một hàng đổi phạm vi và đóng sheet; Snackbar nói đang xem gì.
    if (showAccountPicker) {
        val viewingAll = stringResource(R.string.account_viewing_all)
        ODVAccountPickerSheet(
            accounts = accounts,
            scope = scope,
            onSelect = { chosen ->
                showAccountPicker = false
                if (chosen != scope) {
                    viewModel.selectScope(chosen)
                    val message = when (chosen) {
                        AccountScope.All -> viewingAll
                        is AccountScope.One -> accounts.firstOrNull { it.account.id == chosen.accountId }?.account?.displayName
                            ?.let { resources.getString(R.string.account_viewing, it) }
                    }
                    if (message != null) coroutineScope.launch { snackbarHost.showSnackbar(message) }
                }
            },
            onAddAccount = {
                showAccountPicker = false
                onOpenAddAccount()
            },
            onDismiss = { showAccountPicker = false },
        )
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
