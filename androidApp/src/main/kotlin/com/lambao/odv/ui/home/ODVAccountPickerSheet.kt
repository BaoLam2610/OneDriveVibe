package com.lambao.odv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lambao.odv.R
import com.lambao.odv.core.designsystem.component.ODVAccountPickerRow
import com.lambao.odv.core.designsystem.component.ODVAddAccountRow
import com.lambao.odv.core.designsystem.component.ODVBottomSheet
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.model.AccountStatus

/**
 * Sheet Chọn tài khoản (AC1 đến AC3, thiet-ke-ui.md mục 4.9, TA-04). Thứ tự: hàng "Tất cả tài khoản" (chỉ khi có từ 2 tài khoản), kẻ, các tài
 * khoản theo thứ tự thêm, kẻ, "Thêm tài khoản". Chạm một hàng là đổi phạm vi và đóng sheet ngay, không có nút Lưu; chạm lại mục đang chọn thì chỉ
 * đóng. Danh sách dài cuộn **bên trong** còn tiêu đề và hàng Thêm cố định. Sheet phủ lên cả thanh điều hướng đáy (mục 4.2).
 */
@Composable
internal fun ODVAccountPickerSheet(
    accounts: List<AccountStatus>,
    scope: AccountScope?,
    onSelect: (AccountScope) -> Unit,
    onAddAccount: () -> Unit,
    onDismiss: () -> Unit,
) {
    val inaccessible = stringResource(R.string.account_inaccessible)
    ODVBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.account_picker_title)) {
        Column(Modifier.heightIn(max = PICKER_LIST_MAX_HEIGHT).verticalScroll(rememberScrollState())) {
            if (accounts.size >= 2) {
                ODVAccountPickerRow(
                    name = stringResource(R.string.account_picker_all),
                    subtitle = pluralStringResource(R.plurals.account_picker_all_count, accounts.size, accounts.size),
                    selected = scope == AccountScope.All,
                    onClick = { onSelect(AccountScope.All) },
                    aggregate = true,
                )
                Divider()
            }
            accounts.forEach { status ->
                ODVAccountPickerRow(
                    name = status.account.displayName,
                    subtitle = status.account.email,
                    selected = (scope as? AccountScope.One)?.accountId == status.account.id,
                    onClick = { onSelect(AccountScope.One(status.account.id)) },
                    error = if (status.inaccessible) inaccessible else null,
                )
            }
        }
        Divider()
        ODVAddAccountRow(title = stringResource(R.string.account_add), onClick = onAddAccount)
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(ODVTheme.colors.line))
}

private val PICKER_LIST_MAX_HEIGHT = 360.dp
