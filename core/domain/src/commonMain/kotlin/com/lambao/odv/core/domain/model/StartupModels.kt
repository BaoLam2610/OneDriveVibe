package com.lambao.odv.core.domain.model

/** Tình trạng config đã lưu trên máy (xem `CheckSavedConnectionUseCase`). */
enum class SavedConnection {
    /** Chưa có config: người dùng chưa kết nối. */
    None,

    /** Có config nhưng không giải mã được (khóa Keystore mất, tệp hỏng, hoặc đang khóa). Không phải lúc để xóa config. */
    Unusable,

    /** Có config và giải mã được. */
    Usable,
}

/** Màn đầu tiên sau Splash theo luồng tổng thể (đặc tả mục 2). */
enum class StartDestination { Connect, Home }
