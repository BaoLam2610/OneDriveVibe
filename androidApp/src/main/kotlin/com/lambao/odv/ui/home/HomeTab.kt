package com.lambao.odv.ui.home

/**
 * Các tab của Màn chính theo thứ tự trên thanh điều hướng đáy (DH-01). Short chỉ có trên thanh khi loại Video đang bật và đồng bộ lần
 * đầu đã xong. Là enum (Serializable) nên `rememberSaveable` giữ được tab đang chọn qua xoay màn hình và khi hệ điều hành thu hồi tiến
 * trình (DH-02).
 */
enum class HomeTab { Folders, Library, Short, Settings }
