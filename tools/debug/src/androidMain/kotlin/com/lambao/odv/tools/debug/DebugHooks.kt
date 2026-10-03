package com.lambao.odv.tools.debug

/**
 * Điểm móc để app gắn hành động cần dùng tầng ngoài `:tools:debug` (module này chỉ phụ thuộc common, network, designsystem
 * nên không gọi được domain). `DebugTools.install` ở `:androidApp` đặt các móc; để `null` thì tính năng tương ứng ẩn đi.
 */
object DebugHooks {
    /**
     * Xóa dữ liệu local như "Ngắt kết nối" (config, khóa Keystore, PIN, sinh trắc học, token, bộ đếm sai, và dữ liệu các lát sau
     * đã đăng ký `ConnectionResetter`) rồi đưa app về màn Kết nối. Cài đặt debug (FLAG_SECURE, che log) được giữ.
     */
    @Volatile
    var clearLocalData: (suspend () -> Unit)? = null

    /** Xóa cache thumbnail (đĩa và bộ nhớ) mà không đụng tới kết nối hay dữ liệu khác. Ảnh sẽ tải lại khi cuộn tới. */
    @Volatile
    var clearThumbnailCache: (suspend () -> Unit)? = null
}
