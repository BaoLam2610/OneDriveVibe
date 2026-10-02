package com.lambao.odv.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonSize
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVKeypad
import com.lambao.odv.core.designsystem.component.ODVKeypadAction
import com.lambao.odv.core.designsystem.component.ODVPinDots
import com.lambao.odv.core.designsystem.component.ODVPinDotsState
import com.lambao.odv.core.designsystem.component.ODVPinMessage
import com.lambao.odv.core.designsystem.component.odvShake
import com.lambao.odv.core.designsystem.component.rememberODVHaptics
import com.lambao.odv.core.designsystem.component.rememberODVShakeState
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVDuration
import com.lambao.odv.core.designsystem.theme.ODVTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Board "15 Mã PIN và bàn phím số". Chuỗi để trực tiếp vì chỉ dùng khi phát triển. */
internal fun LazyListScope.pinPage() {
    item { SectionTitle("Chấm PIN") }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(0 to "rỗng", 3 to "đang nhập · 3/6", 6 to "đủ 6 số").forEach { (n, name) ->
                Text(name, style = ODVTheme.typography.meta, color = ODVTheme.colors.inkMuted)
                ODVPinDots(filled = n, contentDescription = "Mã PIN, đã nhập $n trên 6 số")
            }
            Text("lỗi · danger", style = ODVTheme.typography.meta, color = ODVTheme.colors.inkMuted)
            ODVPinDots(filled = 6, contentDescription = "Mã PIN, đã nhập 6 trên 6 số", state = ODVPinDotsState.Error)
            ODVPinMessage("Sai mã PIN. Còn 3 lần thử", isError = true)
            ODVPinMessage("Nhập lại mã PIN để xác nhận")
        }
    }

    item { SectionTitle("Nhập thử (tự kiểm tra khi đủ 6 số, mã đúng là 123456)") }
    item {
        var pin by rememberSaveable { mutableStateOf("") }
        var error by rememberSaveable { mutableStateOf(false) }
        var locked by rememberSaveable { mutableStateOf(false) }
        var withBiometric by rememberSaveable { mutableStateOf(true) }
        var attempts by rememberSaveable { mutableStateOf(0) }
        val shake = rememberODVShakeState()
        val haptics = rememberODVHaptics()
        // Đủ 6 số: màn thật tự kiểm tra; sai thì giữ 6 chấm đỏ 250ms (duration.base) rồi xóa hết chấm.
        LaunchedEffect(error) {
            if (error) {
                haptics.reject()
                // Rung chạy song song với đồng hồ 250ms; nếu effect bị hủy giữa chừng thì ODVShakeState tự về 0.
                launch { shake.shake() }
                delay(ODVDuration.base.toLong())
                pin = ""
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ODVPinDots(
                filled = pin.length,
                modifier = Modifier.odvShake(shake),
                contentDescription = "Mã PIN, đã nhập ${pin.length} trên 6 số",
                state = if (error) ODVPinDotsState.Error else ODVPinDotsState.Default,
            )
            ODVPinMessage(
                text = when {
                    locked -> "Tạm khóa nhập mã PIN"
                    // Đổi chữ theo số lần để TalkBack đọc lại ở lần sai tiếp theo.
                    error -> "Sai mã PIN. Lần sai thứ $attempts"
                    else -> "Nhập mã PIN"
                },
                isError = error || locked,
            )
            ODVKeypad(
                onDigit = { digit ->
                    if (pin.length < 6) {
                        error = false
                        pin += digit
                        if (pin.length == 6 && pin != "123456") {
                            attempts += 1
                            error = true
                        }
                    }
                },
                onBackspace = { pin = pin.dropLast(1); error = false },
                backspaceLabel = "Xóa số cuối",
                secondary = if (withBiometric) ODVKeypadAction(ODVIcon.Fingerprint, "Mở bằng sinh trắc học", {}) else null,
                enabled = !locked,
            )
            ODVButton(if (locked) "Mở khóa tạm" else "Giả lập khóa tạm", { locked = !locked }, style = ODVButtonStyle.Tonal, size = ODVButtonSize.Sm)
            ODVButton(if (withBiometric) "Ẩn phím sinh trắc học" else "Hiện phím sinh trắc học", { withBiometric = !withBiometric }, style = ODVButtonStyle.Ghost, size = ODVButtonSize.Sm)
        }
    }
}
