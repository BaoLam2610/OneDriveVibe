package com.lambao.odv.feature.pdfviewer

import com.lambao.odv.core.domain.model.CachedFileState
import com.lambao.odv.core.domain.model.FolderRef
import com.lambao.odv.core.domain.model.PdfFile
import com.lambao.odv.core.domain.model.PdfReadingStyle

/** Nội dung bảng thông tin tệp (PD-08). Mọi trường lấy từ Room nên hiện ngay và dùng được offline; số trang chỉ có khi tệp đã mở được. */
data class PdfViewerInfo(
    val file: PdfFile,
    val folderPath: List<FolderRef> = emptyList(),
    val pageCount: Int = 0,
)

/** Lớp phủ đang mở ngoài bảng thông tin: nhảy tới trang (PD-04) hoặc chọn kiểu đọc (P2 → nút `tune`). */
enum class PdfOverlay { Jump, Style }

/**
 * Trạng thái màn xem PDF (PD-01 → PD-08). Bitmap các trang không nằm ở đây mà do giao diện giữ (cache theo byte): State chỉ biết trang
 * nào đang đọc. [currentPage] đếm từ 0 và ban đầu là trang đã lưu của lần đọc trước (PD-05).
 */
data class PdfViewerState(
    /** Bản ghi PDF từ Room; null cho tới khi đọc xong hoặc khi tệp không còn. */
    val file: PdfFile? = null,
    /** Đã đọc xong Room; phân biệt "chưa nạp" với "không còn tệp". */
    val isFileLoaded: Boolean = false,
    /** Tiến trình tải tệp về cache (PD-01). `Ready` thì giao diện mở tệp bằng `PdfRenderer`. */
    val download: CachedFileState = CachedFileState.Downloading(0L, null),
    val readingStyle: PdfReadingStyle = PdfReadingStyle.Vertical,
    /** Số trang, chỉ có sau khi `PdfRenderer` mở được tệp; 0 là chưa biết. */
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    /** Tệp đã tải đủ nhưng không mở được: có mật khẩu hoặc hỏng (PD-07). */
    val openFailed: Boolean = false,
    /** Thanh công cụ hiện (PD-06). Ẩn thì vào chế độ toàn màn hình. */
    val controlsVisible: Boolean = true,
    /** Số trang (đếm từ 1) cần báo "Đang ở trang X" khi mở lại tệp (PD-05); null là không có gì để báo. */
    val resumeNotice: Int? = null,
    val overlay: PdfOverlay? = null,
    /** Khác null khi bảng thông tin đang mở (PD-08). */
    val info: PdfViewerInfo? = null,
)

sealed interface PdfViewerIntent {
    /** `PdfRenderer` đã mở được tệp với [pageCount] trang. */
    data class Opened(val pageCount: Int) : PdfViewerIntent

    /** Tệp đã tải đủ nhưng `PdfRenderer` không mở được (PD-07). */
    data object OpenFailed : PdfViewerIntent

    /** Trang đang đọc đổi thành [page] (đếm từ 0) do cuộn hoặc lật. */
    data class PageChanged(val page: Int) : PdfViewerIntent

    /** Ghi ngay trang đang đọc (rời màn, xuống nền) thay vì chờ hết thời gian gộp. */
    data object Persist : PdfViewerIntent

    data object ToggleControls : PdfViewerIntent
    data object ShowJump : PdfViewerIntent
    data object ShowStyle : PdfViewerIntent
    data object DismissOverlay : PdfViewerIntent

    /** Nhảy tới trang số [pageNumber] (đếm từ 1, người dùng nhập, PD-04). */
    data class JumpTo(val pageNumber: Int) : PdfViewerIntent

    data class SelectStyle(val style: PdfReadingStyle) : PdfViewerIntent
    data object ShowInfo : PdfViewerIntent
    data object HideInfo : PdfViewerIntent

    /** Tải lại sau lỗi mạng (PD-01). */
    data object Retry : PdfViewerIntent

    /** Thông báo "Đang ở trang X" đã hiện đủ lâu. */
    data object NoticeShown : PdfViewerIntent
}

sealed interface PdfViewerEffect {
    /** Không còn tệp để xem (đã xóa trên OneDrive qua đồng bộ, DS-06): thoát màn. */
    data object Close : PdfViewerEffect

    /** Cuộn hoặc lật tới trang [page] (đếm từ 0) sau khi người dùng nhập số trang (PD-04). */
    data class ScrollTo(val page: Int) : PdfViewerEffect
}
