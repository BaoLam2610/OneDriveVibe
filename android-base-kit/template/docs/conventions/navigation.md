# Điều hướng (Navigation 3)

Quyết định: ADR-0003. Back stack là một danh sách do app sở hữu; `NavDisplay` vẽ phần tử trên cùng.

## 1. Route

```kotlin
// app/src/main/kotlin/{{PACKAGE}}/navigation/AppRoute.kt
@Serializable
sealed interface AppRoute : NavKey {
    @Serializable data object Splash : AppRoute
    @Serializable data object Login : AppRoute
    @Serializable data object Home : AppRoute
    @Serializable data class ProductDetail(val productId: String) : AppRoute
    @Serializable data class OrderDetail(
        val orderId: String,
        val fromNotification: Boolean = false, // trường thêm sau: CÓ giá trị mặc định
    ) : AppRoute
}
```

- Route nằm ở `:app`; feature không biết route của nhau.
- Route chỉ chứa **id và tham số nhỏ** (chuỗi, số, enum), không chứa model đầy đủ hay danh sách. Màn đích tự đọc dữ liệu theo id.
- **Thêm trường vào route đã phát hành thì phải có giá trị mặc định**: back stack được lưu khi process death, route cũ thiếu trường sẽ không khôi phục được.
- Không đổi tên lớp route hay tên trường đã phát hành mà không có kế hoạch (serial name là một phần của dữ liệu đã lưu).

## 2. NavDisplay

```kotlin
@Composable
fun AppNavDisplay() {
    val backStack = rememberNavBackStack(AppRoute.Splash)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(), // rememberSaveable theo từng entry
            rememberViewModelStoreNavEntryDecorator(),      // ViewModel theo từng entry, hủy khi entry rời back stack
        ),
        transitionSpec = { PfxTransitions.screenPush() },
        popTransitionSpec = { PfxTransitions.screenPop() },
        predictivePopTransitionSpec = { PfxTransitions.screenPop() },
        entryProvider = entryProvider {
            entry<AppRoute.Login> {
                PfxLoginScreen(onLoggedIn = { backStack.replaceAll(AppRoute.Home) })
            }
            entry<AppRoute.ProductDetail> { route ->
                PfxProductDetailScreen(productId = route.productId, onBack = { backStack.removeLastOrNull() })
            }
            // ...
        },
    )
}
```

- **Màn nhận lambda** (`onLoggedIn`, `onOpenProduct`, `onBack`), không nhận `backStack` hay đối tượng điều hướng. Thao tác trên back stack chỉ viết trong `AppNavDisplay`.
- Helper thao tác back stack (`replaceAll`, `popUpTo`) đặt cạnh `AppNavDisplay`.
- Tên API Navigation 3 (decorator, `entryProvider`) kiểm lại theo đúng phiên bản khi khởi tạo.

## 3. Hai tầng: app và tab

Khi app có thanh điều hướng đáy:

- **Tầng app** (back stack ở trên): Splash, đăng nhập, khóa, **Màn chính**, các màn chi tiết toàn màn hình. Màn nào đẩy lên tầng này thì **che** thanh điều hướng đáy.
- **Tầng tab** (bên trong Màn chính): mỗi tab giữ trạng thái riêng suốt vòng đời Màn chính.
  - ViewModel mỗi tab sống cùng Màn chính.
  - Trạng thái giao diện (vị trí cuộn, ô đang mở) giữ bằng `SaveableStateHolder` theo khóa tab: `holder.SaveableStateProvider(tab.key) { TabContent(tab) }`.
  - Back ở tab không phải tab đầu thì về tab đầu; ở tab đầu thì thoát.
  - Chạm lại tab đang chọn: cuộn lên đầu (lần hai về gốc nếu tab có cây thư mục).
- Đổi ngữ cảnh lớn (đổi tài khoản, đổi cửa hàng) mà phải bỏ trạng thái mọi tab thì đổi **khóa** của `SaveableStateHolder` (ví dụ thêm `scopeEpoch`) và dọn khóa cũ. Epoch giữ ở ViewModel của Màn chính, không phụ thuộc thời gian của `WhileSubscribed`.

## 4. Cổng (đăng nhập, khóa)

- Màn cần "chặn" toàn app (khóa bằng PIN, bắt đăng nhập lại) là một route ở tầng app, được **đẩy lên trên cùng** khi trạng thái đổi. Không dùng `if` thay cả `NavDisplay`, vì như vậy mất `rememberSaveable` của các màn bên dưới.
- Lúc khởi động nguội mà trạng thái chưa biết (đang đọc kho bí mật), chưa dựng màn nội dung cho tới khi biết. Vẽ Splash trong lúc chờ, không để lộ khung hình của màn nội dung.
- Splash chỉ quyết định đích **sau khi** trạng thái cổng đã rõ (đã mở khóa hoặc chưa đăng nhập).

## 5. Chuyển cảnh

- Hiệu ứng lấy từ `PfxTransitions` (`:core:designsystem`) theo token chuyển động; mặc định fade ngắn.
- Màn cổng (khóa) hiện **ngay**, không animate (không để lộ nội dung bên dưới trong lúc mờ dần).
- Entry nặng dựng lần đầu (Màn chính) có thể tắt hiệu ứng vào để không khựng.
- Tôn trọng "Giảm chuyển động" của hệ thống.

## 6. Kiểm tay mỗi lát

- Bật **Don't keep activities** (Developer options), mở màn sâu, ra nền, vào lại: đúng màn, đúng tham số, đúng vị trí cuộn.
- Back từ từng màn và từng tab.
- Back dự đoán (predictive back) trên Android 14+.
