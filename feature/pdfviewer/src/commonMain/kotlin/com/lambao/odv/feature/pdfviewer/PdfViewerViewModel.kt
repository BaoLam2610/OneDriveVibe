package com.lambao.odv.feature.pdfviewer

import androidx.lifecycle.viewModelScope
import com.lambao.odv.core.common.mvi.BaseMviViewModel
import com.lambao.odv.core.domain.model.CachedFileRef
import com.lambao.odv.core.domain.model.CachedFileState
import com.lambao.odv.core.domain.model.PdfFile
import com.lambao.odv.core.domain.settings.SettingsPreferences
import com.lambao.odv.core.domain.usecase.folder.GetFolderPathUseCase
import com.lambao.odv.core.domain.usecase.pdf.DiscardCachedPdfUseCase
import com.lambao.odv.core.domain.usecase.pdf.GetPdfFileUseCase
import com.lambao.odv.core.domain.usecase.pdf.GetReadingProgressUseCase
import com.lambao.odv.core.domain.usecase.pdf.OpenPdfUseCase
import com.lambao.odv.core.domain.usecase.pdf.SaveReadingProgressUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Màn xem PDF (PD-01 → PD-08). Thứ tự khởi động: đọc bản ghi và trang đã lưu từ Room, rồi mới tải tệp, để khi tệp sẵn sàng thì trang mở
 * đầu (PD-05) đã biết. Tệp do `PdfRepository` tải và cache; ViewModel chỉ giữ tiến trình. `PdfRenderer` và bitmap các trang thuộc giao
 * diện (Android), nên số trang đến ViewModel qua [PdfViewerIntent.Opened]. Kiểu đọc đọc thẳng `SettingsPreferences` như `SettingsViewModel`
 * (interface cài đặt mà feature được dùng trực tiếp, ADR-0016).
 */
class PdfViewerViewModel(
    private val itemId: String,
    private val getPdfFile: GetPdfFileUseCase,
    private val getReadingProgress: GetReadingProgressUseCase,
    private val saveReadingProgress: SaveReadingProgressUseCase,
    private val openPdf: OpenPdfUseCase,
    private val discardCachedPdf: DiscardCachedPdfUseCase,
    private val getFolderPath: GetFolderPathUseCase,
    private val settings: SettingsPreferences,
) : BaseMviViewModel<PdfViewerState, PdfViewerIntent, PdfViewerEffect>(PdfViewerState()) {

    private var downloadJob: Job? = null
    private var saveJob: Job? = null
    private var infoJob: Job? = null

    /** Lần đọc trước có để lại trang dở (chưa đọc xong); chỉ khi đó mới báo "Đang ở trang X". */
    private var resumed = false
    private var noticeGiven = false

    /** Người dùng đã đổi trang thật kể từ lần ghi cuối; mở hoặc khôi phục trang đầu không đặt cờ này (M1, PD-05). */
    private var pageDirty = false

    /** Việc xóa tệp cache hỏng (PD-07); lần Thử lại chờ nó xong để không tải trúng tệp cũ. */
    private var discardJob: Job? = null

    init {
        viewModelScope.launch { settings.pdfReadingStyle.collect { style -> setState { copy(readingStyle = style) } } }
        loadFile()
    }

    override fun onIntent(intent: PdfViewerIntent) {
        when (intent) {
            is PdfViewerIntent.Opened -> onOpened(intent.pageCount)
            is PdfViewerIntent.OpenFailed -> onOpenFailed(intent.corrupted)
            is PdfViewerIntent.PageChanged -> onPageChanged(intent.page)
            PdfViewerIntent.Persist -> persistNow()
            PdfViewerIntent.ToggleControls -> setState { copy(controlsVisible = !controlsVisible) }
            PdfViewerIntent.ShowJump -> setState { copy(overlay = PdfOverlay.Jump) }
            PdfViewerIntent.ShowStyle -> setState { copy(overlay = PdfOverlay.Style) }
            PdfViewerIntent.DismissOverlay -> setState { copy(overlay = null) }
            is PdfViewerIntent.JumpTo -> jumpTo(intent.pageNumber)
            is PdfViewerIntent.SelectStyle -> {
                setState { copy(overlay = null) }
                viewModelScope.launch { settings.setPdfReadingStyle(intent.style) }
            }
            PdfViewerIntent.ShowInfo -> showInfo()
            PdfViewerIntent.HideInfo -> {
                infoJob?.cancel()
                setState { copy(info = null) }
            }
            PdfViewerIntent.Retry -> currentState.file?.let(::startDownload)
            PdfViewerIntent.NoticeShown -> setState { copy(resumeNotice = null) }
        }
    }

    private fun loadFile() {
        viewModelScope.launch {
            val file = try {
                getPdfFile(itemId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Lỗi đọc Room: coi như không có tệp để mở (viewModelScope không có handler nên không để rơi ra).
                null
            }
            if (file == null) {
                setState { copy(isFileLoaded = true) }
                sendEffect(PdfViewerEffect.Close)
                return@launch
            }
            val progress = try {
                getReadingProgress(itemId, file.item.cTag)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            // Đã đọc xong lần trước (tới trang cuối) thì mở lại từ đầu; còn dở thì vào đúng trang đã lưu (PD-05).
            val start = if (progress == null || progress.finished) 0 else progress.page
            resumed = progress != null && !progress.finished && start > 0
            setState { copy(file = file, isFileLoaded = true, currentPage = start) }
            startDownload(file)
        }
    }

    private fun startDownload(file: PdfFile) {
        downloadJob?.cancel()
        setState { copy(download = CachedFileState.Downloading(0L, null), openFailed = false, openCorrupted = false) }
        downloadJob = viewModelScope.launch {
            // Tệp hỏng vừa bị xóa (M3) thì đợi xóa xong rồi mới tải, nếu không `open` lại thấy bản hỏng và trả Ready ngay.
            discardJob?.join()
            openPdf(CachedFileRef(file.item.id, file.item.cTag)).collect { state -> setState { copy(download = state) } }
        }
    }

    /**
     * `PdfRenderer` không mở được tệp đã tải đủ (PD-07). Có mật khẩu: giữ tệp, chỉ có Quay lại. Hỏng: xóa tệp khỏi cache để "Thử lại" tải
     * lại bản mới thay vì mở lại đúng bản hỏng mãi. (L6: máy chủ không báo Content-Length có thể làm tệp cụt bị coi là đủ, nên tệp hỏng
     * kiểu này cũng đi qua đường này; chưa xử lý riêng.)
     */
    private fun onOpenFailed(corrupted: Boolean) {
        setState { copy(openFailed = true, openCorrupted = corrupted) }
        val file = currentState.file
        if (!corrupted || file == null) return
        discardJob?.cancel()
        discardJob = viewModelScope.launch {
            try {
                discardCachedPdf(CachedFileRef(file.item.id, file.item.cTag))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Không xóa được thì Thử lại sẽ mở lại bản cũ và lỗi lại; không làm sập màn.
            }
        }
    }

    private fun onOpened(pageCount: Int) {
        if (pageCount <= 0) {
            onOpenFailed(corrupted = true)
            return
        }
        // Trang đã lưu có thể vượt số trang thật nếu tệp đổi mà cTag không đổi; kẹp lại để không cuộn ra ngoài.
        val start = currentState.currentPage.coerceIn(0, pageCount - 1)
        val announce = resumed && !noticeGiven
        noticeGiven = true
        setState { copy(pageCount = pageCount, currentPage = start, resumeNotice = if (announce) start + 1 else null) }
    }

    private fun onPageChanged(page: Int) {
        val count = currentState.pageCount
        if (count <= 0 || page !in 0 until count || page == currentState.currentPage) return
        setState { copy(currentPage = page) }
        pageDirty = true
        scheduleSave()
    }

    private fun jumpTo(pageNumber: Int) {
        val count = currentState.pageCount
        if (count <= 0) return
        setState { copy(overlay = null) }
        sendEffect(PdfViewerEffect.ScrollTo((pageNumber - 1).coerceIn(0, count - 1)))
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(PdfViewerConstants.SAVE_DEBOUNCE_MS)
            save()
        }
    }

    private fun persistNow() {
        // Chưa đổi trang thì không ghi (M1, PD-05): mở lại tệp đã `finished` rồi thoát ngay mà ghi trang 1 sẽ hạ `finished` về false.
        if (!pageDirty) return
        saveJob?.cancel()
        if (currentState.pageCount <= 0) return
        viewModelScope.launch { save() }
    }

    private suspend fun save() {
        val state = currentState
        val file = state.file ?: return
        if (state.pageCount <= 0 || !pageDirty) return
        // Hạ cờ trước khi ghi để trang đổi trong lúc ghi vẫn giữ cờ; ghi lỗi thì đặt lại để lần sau thử tiếp.
        pageDirty = false
        try {
            saveReadingProgress(file.item.id, file.item.cTag, state.currentPage, state.pageCount)
        } catch (e: CancellationException) {
            pageDirty = true
            throw e
        } catch (e: Exception) {
            // Không ghi được tiến độ chỉ làm lần sau mở lại ở trang cũ, không đáng làm sập màn đọc.
            pageDirty = true
        }
    }

    private fun showInfo() {
        val file = currentState.file ?: return
        infoJob?.cancel()
        setState { copy(info = PdfViewerInfo(file, pageCount = pageCount)) }
        infoJob = viewModelScope.launch {
            val path = try {
                getFolderPath(file.item.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return@launch
            }
            setState {
                val current = info
                if (current != null && current.file.item.id == file.item.id) copy(info = current.copy(folderPath = path)) else this
            }
        }
    }
}
