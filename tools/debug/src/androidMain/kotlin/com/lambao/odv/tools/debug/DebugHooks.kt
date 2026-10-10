package com.lambao.odv.tools.debug

import kotlinx.coroutines.flow.Flow

/**
 * Điểm móc để app gắn hành động cần dùng tầng ngoài `:tools:debug` (module này chỉ phụ thuộc common, network, designsystem
 * nên không gọi được domain). `DebugTools.install` ở `:androidApp` đặt các móc; để `null` thì tính năng tương ứng ẩn đi.
 */
object DebugHooks {
    /**
     * Xóa dữ liệu local như "Ngắt kết nối" (config, khóa Keystore, PIN, sinh trắc học, token, bộ đếm sai, và dữ liệu các lát sau
     * đã đăng ký `ConnectionResetter`) rồi đưa app về màn Kết nối. Cài đặt debug (che log, chất lượng thumbnail) được giữ.
     */
    @Volatile
    var clearLocalData: (suspend () -> Unit)? = null

    /** Xóa cache thumbnail (đĩa và bộ nhớ) mà không đụng tới kết nối hay dữ liệu khác. Ảnh sẽ tải lại khi cuộn tới. */
    @Volatile
    var clearThumbnailCache: (suspend () -> Unit)? = null

    /** Đọc các kho DataStore (tên kho → khóa → giá trị) để tab Lưu trữ hiển thị. Chỉ đọc, qua đúng instance app đang dùng. */
    @Volatile
    var dumpPreferences: (suspend () -> Map<String, Map<String, String>>)? = null

    /**
     * Luồng của cài đặt "Bảo vệ màn hình" thật (Cài đặt › Bảo mật, CD-12). Tab Khác hiện công tắc bật/tắt cùng cài đặt này, không có
     * trạng thái riêng của Debug (ADR-0020). Lấy luồng lúc dùng vì Koin chỉ có sau `startKoin`.
     */
    @Volatile
    var screenProtection: (() -> Flow<Boolean>)? = null

    /** Ghi cài đặt "Bảo vệ màn hình" thật và áp dụng ngay cho cửa sổ (kể cả màn Debug đang hiện, vì `MainActivity` đang dừng nên chưa dựng lại). */
    @Volatile
    var setScreenProtection: (suspend (Boolean) -> Unit)? = null
}
