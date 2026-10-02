package com.lambao.odv.core.network

/** Ngoại lệ này là lỗi đường truyền (mất mạng, không phân giải được tên miền, ngắt kết nối...). Theo nền tảng. */
internal expect fun Throwable.isNetworkFailure(): Boolean
