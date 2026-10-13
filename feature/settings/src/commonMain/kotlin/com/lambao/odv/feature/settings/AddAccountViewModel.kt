package com.lambao.odv.feature.settings

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.domain.model.AddAccountOutcome
import com.lambao.odv.core.domain.usecase.account.AddAccountUseCase
import com.lambao.odv.core.domain.usecase.account.ObserveAccountsUseCase
import com.lambao.odv.core.domain.usecase.security.ObserveProtectionUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel màn Thêm tài khoản (TA-07). Không hỏi PIN (app đã mở khóa) và không hiện hộp thoại K6. Thành công thì
 * [AddAccountUseCase] đã chuyển phạm vi xem sang tài khoản mới; ở đây chỉ báo cho Màn chính hiện Snackbar "Đã thêm …" rồi đóng màn. Không log UPN (CH-06).
 */
class AddAccountViewModel(
    private val addAccount: AddAccountUseCase,
    observeAccounts: ObserveAccountsUseCase,
    private val observeProtection: ObserveProtectionUseCase,
    private val events: AccountEvents,
) : BaseMviViewModel<AddAccountState, AddAccountIntent, AddAccountEffect>(AddAccountState()) {

    init {
        viewModelScope.launch { observeAccounts().collect { list -> setState { copy(accounts = list) } } }
    }

    override fun onIntent(intent: AddAccountIntent) {
        when (intent) {
            // KN-05: bỏ khoảng trắng và xuống dòng ở đầu/cuối giá trị dán vào.
            is AddAccountIntent.UpnChanged -> setState { copy(upn = intent.value.trim(), duplicateFromServer = false) }
            AddAccountIntent.Submit -> submit()
            AddAccountIntent.DismissFailure -> setState { copy(failure = null) }
            AddAccountIntent.FixSecret -> {
                setState { copy(failure = null) }
                // TK-04, CD-04: bảo vệ bật thì xác thực lại bằng PIN (P1) trước khi tới form; tắt thì vào thẳng form.
                viewModelScope.launch { sendEffect(AddAccountEffect.OpenSecretFix(requiresPin = observeProtection().first())) }
            }
        }
    }

    private fun submit() {
        val state = currentState
        // Guard bấm đôi: đang kiểm tra thì bỏ qua.
        if (!state.canSubmit) return
        setState { copy(isChecking = true, failure = null) }
        viewModelScope.launch {
            // NonCancellable: rời màn giữa chừng không được để config đã ghi mà phạm vi xem chưa đổi.
            when (val outcome = withContext(NonCancellable) { addAccount(state.upn) }) {
                is AddAccountOutcome.Added -> {
                    // added = true trước khi bỏ isChecking: không có khoảnh khắc nào ô nhập thấy UPN trùng với tài khoản vừa thêm.
                    setState { copy(added = true, isChecking = false) }
                    events.notifyAdded(outcome.account.displayName)
                    sendEffect(AddAccountEffect.Added)
                }
                AddAccountOutcome.AlreadyAdded -> setState { copy(isChecking = false, duplicateFromServer = true) }
                // U-L3: app vừa khóa lại giữa chừng (CH-03) không phải lỗi thêm tài khoản: màn Khóa sẽ che, sau khi mở khóa giữ nguyên UPN đã nhập
                // và không hiện hộp thoại "Đã xảy ra lỗi" không có mã.
                is AddAccountOutcome.Failed ->
                    if (outcome.error == AppError.AppLocked) {
                        setState { copy(isChecking = false) }
                    } else {
                        setState { copy(isChecking = false, failure = outcome.error.toAddAccountFailure()) }
                    }
            }
        }
    }
}
