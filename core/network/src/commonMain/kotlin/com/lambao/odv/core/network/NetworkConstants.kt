package com.lambao.odv.core.network

import kotlin.time.Duration.Companion.minutes

/** Hằng số của Microsoft Graph (ADR-0006): địa chỉ gốc, `$select` và kích thước trang của từng endpoint. */
internal object GraphConstants {
    const val BASE_URL = "https://graph.microsoft.com/v1.0"

    /** Kiểm tra kết nối (KN-07): chỉ cần biết drive tồn tại. Không xin `quota` (KN-12). */
    const val DRIVE_SELECT = "id,driveType"
    const val ITEM_INFO_SELECT = "id,name,image,photo,fileSystemInfo"
    const val DOWNLOAD_CHUNK_BYTES = 64 * 1024
    const val CHILDREN_SELECT = "id,name,size,folder,file,video,package"
    const val CHILDREN_PAGE_SIZE = 200

    /**
     * Số mục mỗi trang delta. Đo thực tế trên drive khoảng 3.200 mục: `200` ra 16 trang (~8 giây vì các trang nối đuôi nhau,
     * mỗi trang ~450 ms), `1000` ra 4 trang (~4,5 giây, mỗi trang 175 đến 210 KB, 1,2 đến 1,4 giây). Graph chấp nhận 1000 nên
     * không bị cắt về 200. Đánh đổi: mỗi transaction Room lớn hơn và tắt app giữa chừng mất tối đa một trang lớn hơn (DB-04).
     * Chốt `500` (2026-10-04): với `1000` người dùng gặp app crash khi gọi delta (nghi trang quá lớn, xem crash SIGSEGV
     * `libsqliteJni` ở nhật ký tiến độ); `500` ổn hơn trên thiết bị thật. Chưa đo lại thời gian quét đầy đủ ở `500`.
     * `children` giữ [CHILDREN_PAGE_SIZE] vì chưa đo.
     */
    const val DELTA_PAGE_SIZE = 500

    /**
     * Đúng các trường `DriveItemDto` đọc khi đồng bộ (bỏ `createdBy`, `lastModifiedBy`, `shared`... để giảm payload và thời
     * gian parse trên drive lớn). `deleted` và `root` phải có mặt: thiếu thì không nhận ra mục bị xóa (DS-06) hay thư mục gốc.
     */
    const val DELTA_SELECT =
        "id,name,size,folder,file,video,package,deleted,root,parentReference,cTag," +
            "lastModifiedDateTime,createdDateTime,fileSystemInfo,photo"
}

/** Hằng số của HTTP client dùng chung: timeout, thử lại, giới hạn ghi lưu lượng. */
internal object HttpConstants {
    const val CONNECT_TIMEOUT_MS = 15_000L
    const val REQUEST_TIMEOUT_MS = 30_000L
    const val SOCKET_TIMEOUT_MS = 30_000L

    /** Client Graph: lỗi mạng, timeout, `429`, `5xx` thử lại tối đa ngần này lần (TK-05, TK-06). */
    const val MAX_RETRIES = 2

    /** Client tải URL đã ký (không bearer): thử lại ít hơn vì bên ngoài còn có vòng thử lại của nơi gọi. */
    const val DOWNLOAD_MAX_RETRIES = 1
    const val DOWNLOAD_RETRY_DELAY_MS = 1_000L

    /** `Retry-After` dài hơn thế này thì cắt về đây để màn hình không treo. */
    const val MAX_RETRY_AFTER_SECONDS = 60L
    const val RETRY_BASE_DELAY_MS = 1_000L

    /** Giới hạn ký tự cho một body trong bản ghi lưu lượng debug. Response một trang 200 mục chỉ cỡ trăm KB; vượt thì cắt và ghi rõ. */
    const val MAX_BODY_CHARS = 1_000_000

    /** Tên qualifier Koin của client không bearer, để không lẫn với client Graph. */
    const val DOWNLOAD_CLIENT = "download"
}

/** Hằng số lấy token Client Credentials (ADR-0005). */
internal object AuthConstants {
    const val TOKEN_ENDPOINT = "https://login.microsoftonline.com"
    const val GRAPH_SCOPE = "https://graph.microsoft.com/.default"

    /** Làm mới token chủ động khi còn dưới ngần này là hết hạn (TK-01). */
    val REFRESH_MARGIN = 5.minutes
}
