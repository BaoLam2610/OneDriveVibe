package com.lambao.odv.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.odv.core.designsystem.ODVCollectEffects
import com.lambao.odv.core.designsystem.component.ODVAppBar
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVDialog
import com.lambao.odv.core.designsystem.component.ODVDialogTone
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVScaffold
import com.lambao.odv.core.designsystem.component.ODVSecureWindow
import com.lambao.odv.core.designsystem.component.ODVTextField
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * Màn Thêm tài khoản (TA-07, thiet-ke-ui.md mục 4.9, 5.4; AC4 đến AC7). Chỉ có **một trường UPN**: tài khoản mới dùng chung Tenant ID, Client ID,
 * Client Secret đã kết nối. Luôn chặn chụp màn hình (cùng nhóm màn nhập thông tin kết nối, CH-05). [onBack] là nút quay lại; [onAdded] chạy
 * khi đã thêm xong (app đóng màn, Màn chính hiện Snackbar "Đã thêm …"). [onFixSecret] chạy khi bấm "Sửa Client Secret" ở lỗi secret (TK-04):
 * app đóng màn này rồi mở cập nhật Client Secret (qua P1 nếu [requiresPin]).
 */
@Composable
fun ODVAddAccountScreen(
    onBack: () -> Unit,
    onAdded: () -> Unit,
    onFixSecret: (requiresPin: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddAccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ODVSecureWindow()
    // AC6: đang kiểm tra thì không có Hủy; Back hệ thống vẫn dùng được và chỉ rời màn. Việc thêm chạy NonCancellable trong ViewModel nên rời
    // màn giữa chừng cũng không để config đã ghi mà phạm vi xem chưa đổi.
    ODVCollectEffects(viewModel.effects) { effect ->
        when (effect) {
            AddAccountEffect.Added -> onAdded()
            is AddAccountEffect.OpenSecretFix -> onFixSecret(effect.requiresPin)
        }
    }
    ODVAddAccountContent(state = state, onIntent = viewModel::onIntent, onBack = onBack, modifier = modifier)
}

@Composable
private fun ODVAddAccountContent(
    state: AddAccountState,
    onIntent: (AddAccountIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ODVTheme.colors
    val type = ODVTheme.typography
    // Mở màn là gõ được ngay (không bắt chạm vào ô). Chỉ xin tiêu điểm một lần: `rememberSaveable` để xoay màn hình hay khôi phục sau
    // process death không bật lại bàn phím ngoài ý muốn.
    val upnFocus = remember { FocusRequester() }
    var focusRequested by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!focusRequested) {
            focusRequested = true
            upnFocus.requestFocus()
        }
    }
    Box(modifier.fillMaxSize()) {
        ODVScaffold(
            topBar = {
                ODVAppBar(
                    title = stringResource(R.string.account_add_title),
                    navigation = { ODVIconButton(ODVIcon.ArrowLeft, stringResource(R.string.settings_back), onBack) },
                )
            },
        ) { contentPadding ->
            // Cột, không phải Box xếp chồng: vùng nội dung (cuộn được) chiếm phần còn lại và nút nằm dưới nó. Bàn phím mở thì `imePadding` thu vùng
            // nội dung lại và nút nằm ngay trên bàn phím, không đè lên ô nhập (lỗi 2026-10-12).
            // Lưu ý (U-L4): `imePadding` được giữ vì đã chỉnh theo kiểm tay trên máy. contentPadding của ODVScaffold có thể đã gồm cả IME (safeDrawing);
            // consumeWindowInsets(contentPadding) đặt trước imePadding để phần chồng lấn (thanh điều hướng hệ thống, và IME nếu đã có trong contentPadding)
            // không bị cộng đôi: không khai báo là đã dùng thì nút bị đẩy cao thêm đúng bằng thanh đó (lỗi 2026-10-12).
            Column(Modifier.fillMaxSize().padding(contentPadding).consumeWindowInsets(contentPadding).imePadding()) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(stringResource(R.string.account_add_description), style = type.body, color = colors.inkMuted)
                    Column(Modifier.padding(top = 8.dp)) {
                        ODVTextField(
                            value = state.upn,
                            onValueChange = { onIntent(AddAccountIntent.UpnChanged(it)) },
                            label = stringResource(R.string.account_add_label),
                            modifier = Modifier.focusRequester(upnFocus),
                            // AC5: lỗi trùng (hoặc sai định dạng) thay dòng gợi ý ngay dưới trường.
                            errorText = when {
                                state.isDuplicate -> stringResource(R.string.account_add_duplicate)
                                state.hasFormatError -> stringResource(R.string.account_add_format_error)
                                else -> null
                            },
                            helperText = stringResource(R.string.account_add_hint),
                            enabled = !state.isChecking,
                            // KN-04: bàn phím không học hay gợi ý nội dung.
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done,
                            ),
                        )
                    }
                }
                // AC4: nút sát đáy, toàn chiều rộng; AC6: spinner và "Đang kiểm tra…" thay nhãn, khóa nút.
                Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)) {
                    ODVButton(
                        text = stringResource(if (state.isChecking) R.string.account_add_checking else R.string.account_add_submit),
                        onClick = { onIntent(AddAccountIntent.Submit) },
                        modifier = Modifier.fillMaxWidth(),
                        // Giữ nút sáng trong lúc đang kiểm tra và khoảnh khắc màn đóng sau khi thêm xong (không nháy mờ).
                        enabled = state.canSubmit || state.isChecking || state.added,
                        loading = state.isChecking,
                        loadingDescription = stringResource(R.string.account_add_checking_description),
                        fullWidth = true,
                    )
                }
            }
        }
        // AC7: lỗi kèm mã, nút "Đóng"; lỗi do Client Secret có thêm nút "Sửa Client Secret" (TK-04).
        state.failure?.let { failure -> AddAccountFailureDialog(failure, onIntent) }
    }
}

@Composable
private fun AddAccountFailureDialog(failure: AddAccountFailure, onIntent: (AddAccountIntent) -> Unit) {
    ODVDialog(
        onDismissRequest = { onIntent(AddAccountIntent.DismissFailure) },
        title = stringResource(R.string.account_add_error_title),
        icon = ODVIcon.Alert,
        tone = ODVDialogTone.Danger,
        body = stringResource(failure.kind.messageRes()),
        errorCode = failure.code?.let { stringResource(R.string.account_add_error_code, it) },
        alert = true,
    ) {
        if (failure.isSecretProblem) {
            // TK-04: như K5, "Đóng" (ghost) và "Sửa Client Secret" (primary).
            ODVButton(stringResource(R.string.account_add_error_close), { onIntent(AddAccountIntent.DismissFailure) }, style = ODVButtonStyle.Ghost)
            ODVButton(stringResource(R.string.account_add_error_fix_secret), { onIntent(AddAccountIntent.FixSecret) })
        } else {
            ODVButton(stringResource(R.string.account_add_error_close), { onIntent(AddAccountIntent.DismissFailure) }, style = ODVButtonStyle.Ghost)
        }
    }
}

private fun AddAccountFailureKind.messageRes(): Int = when (this) {
    AddAccountFailureKind.SecretInvalid -> R.string.account_add_error_secret_invalid
    AddAccountFailureKind.SecretExpired -> R.string.account_add_error_secret_expired
    AddAccountFailureKind.AppDisabled -> R.string.account_add_error_app_disabled
    AddAccountFailureKind.Forbidden -> R.string.account_add_error_forbidden
    AddAccountFailureKind.DriveNotFound -> R.string.account_add_error_drive_not_found
    AddAccountFailureKind.Network -> R.string.account_add_error_network
    AddAccountFailureKind.SaveFailed -> R.string.account_add_error_save
    AddAccountFailureKind.Other -> R.string.account_add_error_other
}
