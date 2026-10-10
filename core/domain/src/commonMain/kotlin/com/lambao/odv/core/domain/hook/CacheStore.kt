package com.lambao.odv.core.domain.hook

import com.lambao.odv.core.domain.model.CacheKind

/**
 * Một kho bộ nhớ đệm trên máy (thumbnail, ảnh gốc, video, PDF) để màn Cài đặt đo, xóa và dọn theo giới hạn (CD, mục Bộ nhớ đệm). Mỗi
 * kho tự đăng ký vào Koin; UseCase gom bằng `getAll()` như [ConnectionResetter], nên thêm kho mới không phải sửa chỗ khác.
 * Không ném ngoại lệ: lỗi chỉ ghi log (hợp đồng như [ConnectionResetter]). Khác [ConnectionResetter] ở chỗ chỉ xóa **tệp cache**, không đụng
 * dữ liệu khác, và không đổi trạng thái kết nối.
 */
interface CacheStore {
    val kind: CacheKind

    /** Dung lượng đang chiếm trên đĩa (byte). Không mở hay quét nặng nếu kho chưa dùng. */
    suspend fun usedBytes(): Long

    /** Xóa toàn bộ tệp cache của kho. Tải đang bay không được ghi lại sau khi xóa. */
    suspend fun clear()

    /** Dọn tệp lâu không dùng nhất trước cho tới khi kho không quá [maxBytes] (CD-07). Kho không dọn chọn lọc được thì xóa hết. */
    suspend fun trimTo(maxBytes: Long)
}

/**
 * Trần dung lượng hiện tại của từng loại, đọc lúc cần (không tiêm giá trị cố định: người dùng đổi được ở Cài đặt, `tech-stack.md` §9
 * "Cài đặt đọc lúc chạy"). Bản cài đặt ở `:core:data`.
 */
interface CacheBudgetProvider {
    fun bytesFor(kind: CacheKind): Long
}
