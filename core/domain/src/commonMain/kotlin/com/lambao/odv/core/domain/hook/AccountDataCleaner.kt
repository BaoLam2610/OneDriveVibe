package com.lambao.odv.core.domain.hook

/**
 * Điểm mở rộng của việc xóa một tài khoản khỏi app (TA-08, ADR-0030): mỗi phần dữ liệu **theo tài khoản** (Room đồng bộ, tiến độ
 * đọc, bộ nhớ đệm) tự đăng ký một bản cài để xóa phần của mình. Cùng kiểu với [ConnectionResetter] nhưng chỉ cho một tài khoản
 * (xóa hết là việc của [ConnectionResetter]).
 *
 * [purge] chạy **sau khi** tài khoản đã bị gỡ khỏi config, nên không còn ai bắt đầu việc mới cho nó; bản cài tự dừng việc đang dở
 * của tài khoản đó (vd. đồng bộ) trước khi xóa. Lỗi của một bản cài không được chặn các bản còn lại: tự nuốt lỗi hoặc ghi log.
 */
interface AccountDataCleaner {

    /** Xóa dữ liệu của tài khoản có drive id [accountId]. */
    suspend fun purge(accountId: String)
}
