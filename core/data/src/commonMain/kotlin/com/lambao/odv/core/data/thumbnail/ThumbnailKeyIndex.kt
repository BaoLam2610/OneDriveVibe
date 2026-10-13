package com.lambao.odv.core.data.thumbnail

/**
 * Chỉ mục khóa thumbnail theo tài khoản (TA-08, ADR-0030). `DiskCache` của Coil có `remove(key)` nhưng không liệt kê được khóa, nên mỗi lần
 * ghi một thumbnail mới vào đĩa thì ghi nhớ khóa của nó dưới tài khoản chứa tệp; xóa tài khoản thì gỡ từng khóa đó khỏi cache. Khóa đã bị LRU
 * dọn từ trước thì gỡ không có tác dụng, vô hại. Chỉ là phương tiện dọn dẹp: mất chỉ mục thì thumbnail nằm lại tới khi bị LRU dọn, không hỏng gì.
 * Không ném ngoại lệ.
 */
internal interface ThumbnailKeyIndex {

    /** Ghi nhớ [key] thuộc tài khoản [accountId] (drive id). Gọi ngay sau khi ghi thumbnail vào đĩa; chỉ ghi một lần cho mỗi khóa trong một phiên. */
    fun record(accountId: String, key: String)

    /** Các khóa đã ghi nhớ của [accountId]. */
    suspend fun keysOf(accountId: String): List<String>

    /** Bỏ chỉ mục của [accountId] (sau khi đã gỡ các khóa khỏi cache). */
    suspend fun forget(accountId: String)

    /** Bỏ mọi chỉ mục (cache thumbnail bị xóa sạch, ngắt kết nối). */
    suspend fun clear()
}
