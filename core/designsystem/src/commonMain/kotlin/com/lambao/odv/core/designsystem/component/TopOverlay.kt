package com.lambao.odv.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Nội dung nổi phải luôn nằm trên cùng (nút bọ của công cụ debug, ADR-0012). Dialog và BottomSheet là cửa sổ riêng nên
 * một lớp phủ vẽ trong Activity sẽ bị chúng đè lên. [ODVDialog] và [ODVBottomSheet] vì thế gọi [ODVTopOverlayInWindow] ngay
 * ở TRONG nội dung cửa sổ của mình: lớp phủ tự mở một Popup, mà Popup là cửa sổ con của cửa sổ chứa nó nên nằm trên Dialog/sheet.
 *
 * Tham số `asWindow`: `false` khi vẽ inline trong Activity; `true` khi được gọi từ Dialog/BottomSheet, nơi nội dung phải
 * tự mở một cửa sổ riêng (Popup) để nằm trên cửa sổ vừa mở. Mặc định `null`: bản release không có lớp phủ nào.
 */
val LocalODVTopOverlay = staticCompositionLocalOf<(@Composable (asWindow: Boolean) -> Unit)?> { null }

/** Gọi trong [ODVDialog] và [ODVBottomSheet], ngay sau cửa sổ của chúng. */
@Composable
internal fun ODVTopOverlayInWindow() {
    LocalODVTopOverlay.current?.invoke(true)
}
