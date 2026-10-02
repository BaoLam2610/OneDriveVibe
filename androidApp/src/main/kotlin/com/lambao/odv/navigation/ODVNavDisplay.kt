package com.lambao.odv.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.lambao.odv.feature.auth.connect.ODVConnectScreen
import com.lambao.odv.feature.auth.security.ODVSecuritySetupScreen
import com.lambao.odv.feature.browser.ODVBrowserScreen
import com.lambao.odv.ui.placeholder.ODVPlaceholderScreen
import com.lambao.odv.ui.splash.ODVSplashScreen

/**
 * Gốc điều hướng (ADR-0003). Back stack là state do app giữ: luồng khóa (Lát 2) thao tác thẳng trên danh sách này.
 * Màn con không nhận back stack; chúng nhận lambda như `onContinue`, `onFinished`.
 */
@Composable
fun ODVNavDisplay(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(AppRoute.Splash)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        // Ở màn gốc NavDisplay không bắt nút Back nên hệ thống đóng app; điều kiện chỉ để không bao giờ làm rỗng back stack.
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
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
                // Back từ Thiết lập bảo mật quay lại đây, các ô vẫn còn giá trị đã nhập.
                ODVConnectScreen(onContinue = { backStack.add(AppRoute.SecuritySetup) })
            }
            entry<AppRoute.SecuritySetup> {
                ODVSecuritySetupScreen(
                    // Đã lưu config: bỏ Kết nối và Bảo mật khỏi back stack để Back không quay lại form.
                    onFinished = { backStack.resetTo(AppRoute.Home) },
                    onMissingConnection = { backStack.resetTo(AppRoute.Connect) },
                )
            }
            // Màn Khóa làm ở Lát 2.
            entry<AppRoute.Lock> { ODVPlaceholderScreen() }
            // Màn xem (Lát 5–7) sẽ truyền vào onOpenFile.
            entry<AppRoute.Home> { ODVBrowserScreen() }
        },
    )
}

/** Thay toàn bộ back stack bằng một route. Khác `replaceAll` của MutableList (java) nên đặt tên riêng. */
private fun MutableList<NavKey>.resetTo(route: NavKey) {
    clear()
    add(route)
}
