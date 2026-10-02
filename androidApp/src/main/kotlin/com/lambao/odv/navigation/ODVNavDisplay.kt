package com.lambao.odv.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.lambao.odv.ui.placeholder.ODVPlaceholderScreen
import com.lambao.odv.ui.splash.ODVSplashScreen

/**
 * Gốc điều hướng (ADR-0003). Back stack là state do app giữ: luồng khóa (Lát 2) thao tác thẳng trên danh sách này.
 * Màn con không nhận back stack; chúng nhận lambda như `onConnected`, `onOpenFile`.
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
            entry<AppRoute.Splash> { ODVSplashScreen() }
            // Lát 0: các route dưới đây chưa có nội dung, thay bằng màn thật ở Lát 1–3.
            entry<AppRoute.Connect> { ODVPlaceholderScreen() }
            entry<AppRoute.SecuritySetup> { ODVPlaceholderScreen() }
            entry<AppRoute.Lock> { ODVPlaceholderScreen() }
            entry<AppRoute.Home> { ODVPlaceholderScreen() }
        },
    )
}
