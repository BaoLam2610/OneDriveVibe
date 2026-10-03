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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.domain.model.LockState
import com.lambao.odv.core.domain.repository.SecurityRepository
import com.lambao.odv.feature.auth.connect.ODVConnectScreen
import com.lambao.odv.feature.auth.lock.ODVLockScreen
import com.lambao.odv.feature.auth.security.ODVSecuritySetupScreen
import com.lambao.odv.feature.browser.ODVBrowserScreen
import com.lambao.odv.ui.splash.ODVSplashScreen
import org.koin.compose.koinInject

/**
 * Gốc điều hướng (ADR-0003). Back stack là state do app giữ: luồng khóa (Lát 2) thao tác thẳng trên danh sách này.
 * Màn con không nhận back stack; chúng nhận lambda như `onSetupPin`, `onSkipPin`.
 */
@Composable
fun ODVNavDisplay(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(AppRoute.Splash)
    val security = koinInject<SecurityRepository>()
    val lockState by security.lockState.collectAsStateWithLifecycle()

    // Đọc chế độ bảo mật từ tệp config: chạy ngoài NavDisplay để không phụ thuộc vào màn nào đang hiện (ADR-0014).
    LaunchedEffect(security) { security.initialize() }

    // Cổng khóa: chế độ PIN thì màn Khóa luôn nằm trên cùng khi lockState là Locked (khởi động nguội, khôi phục sau
    // process death, hoặc vừa rời app). Mở khóa xong, chính màn Khóa tự bỏ chính nó (onUnlocked). Khóa theo cả đỉnh
    // back stack để nếu Lock bị bỏ nhầm lúc app vừa bị khóa lại thì được đẩy lên lại, không kẹt ở màn trống.
    val top = backStack.lastOrNull()
    LaunchedEffect(lockState, top) {
        if (lockState == LockState.Locked && top != AppRoute.Lock) backStack.add(AppRoute.Lock)
    }

    // Chưa biết chế độ: không vẽ gì để màn khôi phục từ back stack không lọt ra dù một khung hình.
    if (lockState == LockState.Unknown) {
        Box(modifier.fillMaxSize().background(ODVTheme.colors.bg))
        return
    }
    // Đang khóa mà màn Khóa chưa lên trên cùng: giữ NavDisplay trong composition (để không mất rememberSaveable của màn
    // đang xem) nhưng phủ kín nền lên trên.
    val covered = lockState == LockState.Locked && top != AppRoute.Lock

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
                        onSetupPin = { if (backStack.lastOrNull() != AppRoute.SecuritySetup) backStack.add(AppRoute.SecuritySetup) },
                        onSkipPin = { backStack.resetTo(AppRoute.Home) },
                    )
                }
                entry<AppRoute.SecuritySetup> {
                    ODVSecuritySetupScreen(
                        // BM-08: Back quay lại màn Kết nối, nơi hộp thoại KN-13 hiện lại.
                        onBack = { backStack.removeLastOrNull() },
                        // Đã bật bảo vệ (BM-04): bỏ Kết nối và Bảo mật khỏi back stack để Back không quay lại form.
                        onDone = { backStack.resetTo(AppRoute.Home) },
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
                            if (security.lockState.value == LockState.Unlocked && backStack.lastOrNull() == AppRoute.Lock) {
                                backStack.removeLastOrNull()
                            }
                        },
                        // Đã xóa dữ liệu (Quên mã PIN, sai quá nhiều): về Kết nối với back stack sạch.
                        onDisconnected = { backStack.resetTo(AppRoute.Connect) },
                    )
                }
                // Màn xem (Lát 5–7) sẽ truyền vào onOpenFile.
                entry<AppRoute.Home> { ODVBrowserScreen() }
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
