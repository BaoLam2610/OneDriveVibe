package com.lambao.odv.core.domain.repository

/**
 * Điểm mở rộng của việc ngắt kết nối (CD-05, KH-03, KH-06): mỗi phần dữ liệu của app tự đăng ký một bản cài để xóa dữ
 * liệu của mình (Room đồng bộ ở Lát 3, cache ở Lát 4, lịch sử xem ở Lát 8, cài đặt ở Lát 9). Nhờ vậy thêm lát mới
 * không phải sửa `DisconnectUseCase`.
 *
 * Lỗi của một bản cài không được chặn các bản còn lại: bản cài tự nuốt lỗi hoặc ghi log, không ném ngoại lệ.
 */
interface ConnectionResetter {
    suspend fun reset()
}
