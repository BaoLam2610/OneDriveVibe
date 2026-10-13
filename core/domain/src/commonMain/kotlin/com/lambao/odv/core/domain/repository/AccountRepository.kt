package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.domain.model.Account
import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.model.AddAccountOutcome
import com.lambao.odv.core.domain.model.RemoveAccountOutcome
import kotlinx.coroutines.flow.Flow

/**
 * Danh sách tài khoản và phạm vi xem (đặc tả mục 3.9, ADR-0030). Danh sách nằm trong config đã mã hóa (CH-01) nên rỗng khi chưa có
 * config hoặc app đang khóa (CH-03); phạm vi xem là tùy chọn hiển thị, nhớ qua các lần mở app.
 */
interface AccountRepository {

    /** Tài khoản theo thứ tự thêm (TA-01). Phát lại khi thêm, xóa hoặc cập nhật tên, và khi mở khóa. */
    fun observeAccounts(): Flow<List<Account>>

    /**
     * Phạm vi xem **đã được đưa về giá trị hợp lệ**: chưa chọn thì là tài khoản đầu; tài khoản đã chọn không còn thì là tài khoản đầu;
     * [AccountScope.All] khi chỉ có một tài khoản thì là tài khoản đó (TA-04). Chưa có tài khoản nào thì phát [AccountScope.All]
     * (không khớp tài khoản nào, các truy vấn trả rỗng).
     */
    fun observeScope(): Flow<AccountScope>

    /** Nhớ phạm vi xem (TA-04). Việc bỏ trạng thái các tab (TA-06) do tầng trình bày làm khi thấy [observeScope] đổi. */
    suspend fun setScope(scope: AccountScope)

    /**
     * Lưu [account] đã được xác minh là truy cập được (TA-07). Trùng UPN (không phân biệt hoa thường) hoặc cùng drive id thì trả
     * [AddAccountOutcome.AlreadyAdded] và không đổi gì. Mã hóa lại config giữ nguyên chế độ bảo mật, không hỏi PIN (TA-07).
     */
    suspend fun add(account: Account): AddAccountOutcome

    /**
     * Xóa tài khoản khỏi app (TA-08): UPN khỏi config, dữ liệu đồng bộ, lịch sử xem và cache của tài khoản đó. Không xóa được tài
     * khoản đang là phạm vi xem (trừ khi phạm vi là Tất cả) và tài khoản duy nhất: trả [RemoveAccountOutcome.NotAllowed].
     */
    suspend fun remove(accountId: String): RemoveAccountOutcome

    /**
     * Dọn dữ liệu mồ côi của tài khoản đã bị gỡ khỏi config nhưng chưa dọn xong (vd. app bị tắt giữa lúc [remove]). Chỉ chạy được khi
     * đọc được danh sách tài khoản; chưa có tài khoản nào hoặc app khóa thì không làm gì. Không ném ngoại lệ (trừ hủy coroutine).
     */
    suspend fun purgeOrphanedData()

    /** Cập nhật tên hiển thị và email sau mỗi lần đồng bộ thành công (TA-02). Không đổi gì nếu giá trị không thay đổi. */
    suspend fun updateDetails(account: Account)
}
