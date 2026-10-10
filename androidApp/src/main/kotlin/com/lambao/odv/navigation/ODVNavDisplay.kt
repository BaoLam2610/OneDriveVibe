package com.lambao.odv.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.domain.model.LockState
import com.lambao.odv.core.domain.model.MediaKind
import com.lambao.odv.core.domain.usecase.security.InitializeSecurityUseCase
import com.lambao.odv.core.domain.usecase.security.ObserveLockStateUseCase
import com.lambao.odv.feature.auth.connect.ODVConnectScreen
import com.lambao.odv.feature.auth.lock.ODVLockScreen
import com.lambao.odv.feature.auth.security.ODVSecuritySetupScreen
import com.lambao.odv.feature.imageviewer.ODVImageViewerScreen
import com.lambao.odv.feature.player.ODVPlayerScreen
import com.lambao.odv.feature.settings.ODVPinFlowScreen
import com.lambao.odv.feature.settings.ODVSettingsScreen
import com.lambao.odv.feature.settings.ODVUpdateSecretScreen
import com.lambao.odv.feature.settings.PinPurpose
import com.lambao.odv.ui.home.ODVHomeScreen
import com.lambao.odv.ui.splash.ODVSplashScreen
import org.koin.compose.koinInject

/**
 * Gốc điều hướng (ADR-0003). Back stack là state do app giữ: luồng khóa (Lát 2) thao tác thẳng trên danh sách này.
 * Màn con không nhận back stack; chúng nhận lambda như `onSetupPin`, `onSkipPin`.
 */
@Composable
fun ODVNavDisplay(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(AppRoute.Splash)
    val observeLockState = koinInject<ObserveLockStateUseCase>()
    val initializeSecurity = koinInject<InitializeSecurityUseCase>()
    // Cùng một StateFlow của repository (không bọc lại): .value đọc đồng bộ ở onUnlocked bên dưới.
    val lockStateFlow = observeLockState()
    // collectAsState, không phải collectAsStateWithLifecycle: khi app ở nền mà bị khóa (ON_STOP), lifecycle-aware collector
    // dừng nên composition giữ lockState cũ (Unlocked); lúc quay lại vài khung hình đầu vẽ nội dung cũ rồi mới trượt Lock vào.
    // StateFlow nằm trong bộ nhớ nên thu liên tục không tốn gì (code review e8027e03, M1).
    val lockState by lockStateFlow.collectAsState()

    // Đọc chế độ bảo mật từ tệp config: chạy ngoài NavDisplay để không phụ thuộc vào màn nào đang hiện (ADR-0014).
    LaunchedEffect(initializeSecurity) { initializeSecurity() }

    // Cổng khóa: chế độ PIN thì màn Khóa luôn nằm trên cùng khi lockState là Locked (khởi động nguội, khôi phục sau
    // process death, hoặc vừa rời app). Mở khóa xong, chính màn Khóa tự bỏ chính nó (onUnlocked). Khóa theo cả đỉnh
    // back stack để nếu Lock bị bỏ nhầm lúc app vừa bị khóa lại thì được đẩy lên lại, không kẹt ở màn trống.
    val top = backStack.lastOrNull()
    LaunchedEffect(lockState, top) {
        if (lockState == LockState.Locked && top != AppRoute.Lock) backStack.add(AppRoute.Lock)
    }

    val covered = lockState == LockState.Locked && top != AppRoute.Lock
    // NavDisplay đã được dựng ít nhất một lần chưa. `remember` nên mất khi process chết hoặc Activity tạo lại (xoay màn hình):
    // khởi động nguội luôn bắt đầu false; còn sau khi xoay thì Lock thường đã ở trên cùng nên `covered` là false, vô hại.
    var navShown by remember { mutableStateOf(false) }

    // Chưa biết chế độ, hoặc khởi động nguội mà đang khóa: chưa dựng NavDisplay. Nếu dựng khi Lock chưa lên trên cùng thì
    // Splash/Kết nối (màn dưới cùng của back stack) sẽ chạy và tự điều hướng khi config còn khóa, và lúc Lock trượt vào
    // thì Splash nhấp nháy. Chờ Lock lên trên cùng rồi mới dựng, nên màn đầu tiên người dùng thấy là màn Khóa.
    if (lockState == LockState.Unknown || (!navShown && covered)) {
        Box(modifier.fillMaxSize().background(ODVTheme.colors.bg))
        return
    }
    if (!navShown) LaunchedEffect(Unit) { navShown = true }
    // Đã dựng rồi mà bị khóa (xuống nền) trong khi màn Khóa chưa lên trên cùng: giữ NavDisplay trong composition (để không
    // mất rememberSaveable của màn đang xem) nhưng phủ kín nền lên trên (`covered`).

    Box(modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier,
            // Ở màn gốc NavDisplay không bắt nút Back nên hệ thống đóng app; điều kiện chỉ để không bao giờ làm rỗng back stack.
            // Màn Khóa tự xử lý Back (thu app xuống nền), không bao giờ pop để lộ màn bên dưới.
            onBack = { if (backStack.size > 1 && backStack.lastOrNull() != AppRoute.Lock) backStack.removeLastOrNull() },
            entryDecorators = listOf(
                // Giữ rememberSaveable của từng màn khi màn đó còn trong back stack.
                rememberSaveableStateHolderNavEntryDecorator(),
                // Mỗi màn có ViewModelStore riêng; ViewModel bị hủy khi màn rời back stack.
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<AppRoute.Splash> {
                    ODVSplashScreen(
                        onConnectRequired = { backStack.resetTo(AppRoute.Connect) },
                        onReady = { backStack.resetTo(AppRoute.Home) },
                    )
                }
                entry<AppRoute.Connect> {
                    // Config đã lưu trước khi hộp thoại KN-13 hiện. "Để sau" bỏ màn Kết nối khỏi back stack để Back không
                    // quay lại form; "Thiết lập mã PIN" đặt màn bảo mật lên trên, Back (BM-08) quay lại đây và hộp thoại hiện lại.
                    ODVConnectScreen(
                        // Guard: K6 là cửa sổ riêng nên còn chạm được trong lúc chuyển màn, bấm đôi không được thêm 2 entry.
                        onSetupPin = { if (backStack.lastOrNull() !is AppRoute.SecuritySetup) backStack.add(AppRoute.SecuritySetup()) },
                        onSkipPin = { backStack.resetTo(AppRoute.Home) },
                    )
                }
                entry<AppRoute.SecuritySetup> { route ->
                    ODVSecuritySetupScreen(
                        // BM-08: Back quay lại màn Kết nối, nơi hộp thoại KN-13 hiện lại (hoặc về Cài đặt nếu mở từ đó, CD-02).
                        onBack = { backStack.removeLastOrNull() },
                        onDone = {
                            if (route.fromSettings) {
                                // CD-02: bật bảo vệ từ Cài đặt xong thì quay về Cài đặt.
                                if (backStack.lastOrNull() == route) backStack.removeLastOrNull()
                            } else {
                                // Đã bật bảo vệ (BM-04): bỏ Kết nối và Bảo mật khỏi back stack để Back không quay lại form.
                                backStack.resetTo(AppRoute.Home)
                            }
                        },
                    )
                }
                entry<AppRoute.Lock> {
                    // KH: Back ở màn Khóa thu app xuống nền (không về màn bên dưới). ON_STOP sau đó chỉ khóa lại, đã khóa rồi.
                    val activity = LocalContext.current.findActivity()
                    BackHandler { activity?.moveTaskToBack(true) }
                    ODVLockScreen(
                        // Nhập đúng PIN: bỏ màn Khóa để thấy lại màn đang xem.
                        // Chỉ khi thật sự đang mở khóa: nếu app bị khóa lại giữa chừng (xuống nền) thì giữ màn Khóa.
                        onUnlocked = {
                            if (lockStateFlow.value == LockState.Unlocked && backStack.lastOrNull() == AppRoute.Lock) {
                                backStack.removeLastOrNull()
                            }
                        },
                        // Đã xóa dữ liệu (Quên mã PIN, sai quá nhiều): về Kết nối với back stack sạch.
                        onDisconnected = { backStack.resetTo(AppRoute.Connect) },
                    )
                }
                entry<AppRoute.Home> {
                    ODVHomeScreen(
                        // Guard: chạm đôi bánh răng không được đẩy hai màn Cài đặt.
                        onOpenSettings = { if (backStack.lastOrNull() != AppRoute.Settings) backStack.add(AppRoute.Settings) },
                        // Banner hết hạn secret (CD-06): bảo vệ bật thì xác thực lại bằng PIN trước (CD-04), tắt thì vào thẳng form.
                        onUpdateSecret = { requiresPin ->
                            if (backStack.lastOrNull() == AppRoute.Home) {
                                backStack.add(if (requiresPin) AppRoute.SettingsPin(PinPurpose.UpdateSecret.name) else AppRoute.UpdateSecret)
                            }
                        },
                        onOpenFile = { item, viewerContext ->
                            // Guard: chạm đôi một ô không được đẩy hai màn xem. PDF (Lát 7) chưa có màn xem.
                            val top = backStack.lastOrNull()
                            if (top !is AppRoute.ImageViewer && top !is AppRoute.VideoPlayer) {
                                when (item.mediaKind) {
                                    MediaKind.Image -> backStack.add(viewerContext.toImageViewerRoute(item.id))
                                    MediaKind.Video -> backStack.add(viewerContext.toVideoPlayerRoute(item.id))
                                    else -> Unit
                                }
                            }
                        },
                    )
                }
                entry<AppRoute.Settings> { route ->
                    ODVSettingsScreen(
                        // Chỉ bỏ khi đang ở trên cùng (không bỏ nhầm màn Khóa).
                        onBack = { if (backStack.size > 1 && backStack.lastOrNull() == route) backStack.removeLastOrNull() },
                        // Guard: bấm đôi công tắc không được đẩy hai màn.
                        onOpenSecuritySetup = { if (backStack.lastOrNull() == route) backStack.add(AppRoute.SecuritySetup(fromSettings = true)) },
                        onOpenPin = { purpose -> if (backStack.lastOrNull() == route) backStack.add(AppRoute.SettingsPin(purpose.name)) },
                        onOpenSecretForm = { if (backStack.lastOrNull() == route) backStack.add(AppRoute.UpdateSecret) },
                        // CD-05: đã xóa sạch dữ liệu, về Kết nối với back stack sạch.
                        onDisconnected = { backStack.resetTo(AppRoute.Connect) },
                    )
                }
                entry<AppRoute.SettingsPin> { route ->
                    val purpose = PinPurpose.entries.firstOrNull { it.name == route.purpose }
                    if (purpose == null) {
                        // Tên lạ (route cũ sau khi cập nhật app): quay về Cài đặt thay vì kẹt ở màn trống.
                        LaunchedEffect(route) { if (backStack.lastOrNull() == route) backStack.removeLastOrNull() }
                    } else {
                        ODVPinFlowScreen(
                            purpose = purpose,
                            onBack = { if (backStack.lastOrNull() == route) backStack.removeLastOrNull() },
                            onDone = {
                                if (backStack.lastOrNull() == route) {
                                    backStack.removeLastOrNull()
                                    // CD-04: P1 xong (xác thực lại) thì mở form nhập Client Secret thay cho màn PIN.
                                    if (purpose == PinPurpose.UpdateSecret) backStack.add(AppRoute.UpdateSecret)
                                }
                            },
                            // KH-06: nhập sai quá nhiều khi CD-08 bật, dữ liệu đã bị xóa: về Kết nối với back stack sạch.
                            onDisconnected = { backStack.resetTo(AppRoute.Connect) },
                        )
                    }
                }
                entry<AppRoute.UpdateSecret> { route ->
                    ODVUpdateSecretScreen(
                        onBack = { if (backStack.lastOrNull() == route) backStack.removeLastOrNull() },
                        onSaved = { if (backStack.lastOrNull() == route) backStack.removeLastOrNull() },
                    )
                }
                entry<AppRoute.ImageViewer> { route ->
                    ODVImageViewerScreen(
                        context = route.toViewerContext(),
                        startItemId = route.startItemId,
                        // Nút quay lại và "không còn ảnh nào" cùng đi qua đây; chỉ bỏ khi nó đang ở trên cùng (không bỏ nhầm màn Khóa).
                        onBack = { if (backStack.size > 1 && backStack.lastOrNull() == route) backStack.removeLastOrNull() },
                    )
                }
                entry<AppRoute.VideoPlayer> { route ->
                    ODVPlayerScreen(
                        context = route.toViewerContext(),
                        startItemId = route.startItemId,
                        // Cùng cách đóng với màn xem ảnh: chỉ bỏ khi nó đang ở trên cùng (không bỏ nhầm màn Khóa).
                        onBack = { if (backStack.size > 1 && backStack.lastOrNull() == route) backStack.removeLastOrNull() },
                    )
                }
            },
        )
        // Chỉ phủ khi đang khóa mà màn Khóa chưa lên trên cùng; mọi trường hợp khác phải thấy NavDisplay.
        if (covered) Box(Modifier.fillMaxSize().background(ODVTheme.colors.bg))
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Thay toàn bộ back stack bằng một route. Khác `replaceAll` của MutableList (java) nên đặt tên riêng. */
private fun MutableList<NavKey>.resetTo(route: NavKey) {
    clear()
    add(route)
}
