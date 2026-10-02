package com.lambao.odv.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lambao.odv.core.designsystem.theme.ODVTheme

/**
 * Khung màn hình chuẩn: [topBar] đứng cố định ở trên, [content] cuộn bên dưới nên thanh không bị cuộn mất.
 * Cả hai nền `bg`. Khung tự lo WindowInsets (edge-to-edge): thanh trên đệm theo thanh trạng thái hoặc tai thỏ, hai bên đệm
 * theo cạnh màn hình. Phần đệm phía dưới (thanh điều hướng, bàn phím) truyền vào [content] qua `contentPadding`;
 * danh sách cuộn nên đặt nó vào `contentPadding` của LazyColumn để nội dung cuộn xuống tới sát đáy rồi mới né thanh hệ thống.
 *
 * [content] phải dùng `contentPadding` này: khung không tiêu thụ inset đáy, nên bỏ qua thì nội dung bị thanh điều hướng hoặc
 * bàn phím che.
 *
 * @param topBar thường là [ODVAppBar] hoặc [ODVSearchBar]; chiều cao của nó tự do, khung không ép.
 * @param content nhận đệm đáy; không cần tự xử lý inset trên và hai bên.
 */
@Composable
fun ODVScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    content: @Composable (contentPadding: PaddingValues) -> Unit,
) {
    val colors = ODVTheme.colors
    val insets = WindowInsets.safeDrawing
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .windowInsetsPadding(insets.only(WindowInsetsSides.Top)),
        ) {
            topBar()
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            content(insets.only(WindowInsetsSides.Bottom).asPaddingValues())
        }
    }
}
