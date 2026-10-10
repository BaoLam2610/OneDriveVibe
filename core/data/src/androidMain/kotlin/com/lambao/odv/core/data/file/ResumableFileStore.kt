package com.lambao.odv.core.data.file

import co.touchlab.kermit.Logger
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.data.ResumableFileConstants
import com.lambao.odv.core.domain.hook.ConnectionResetter
import com.lambao.odv.core.domain.model.CachedFileRef
import com.lambao.odv.core.domain.model.CachedFileState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Hàm tải nội dung từ byte [offset] trở đi. Cùng hợp đồng với `GraphApi.downloadContent` nhưng không mang `itemId`, nên
 * [ResumableFileStore] không biết gì về Graph: bên gọi tự bọc thành lời gọi API.
 */
internal fun interface RangeDownloader {
    suspend operator fun invoke(
        offset: Long,
        onStart: suspend (resumed: Boolean, totalBytes: Long?) -> Unit,
        onBytes: suspend (buffer: ByteArray, length: Int) -> Unit,
    ): AppResult<Unit>
}

/**
 * Kho tệp tải về dựa trên [directory] (nằm ở `cacheDir`: không sao lưu, CH-04; hệ thống có thể dọn khi thiếu chỗ, mất thì
 * tải lại), dùng chung cho ảnh gốc và PDF. Tách biệt với `DiskCache` thumbnail của Coil vì cần nguyên bản và cần tải tiếp
 * theo `Range` (BN-03) mà `DiskCache` không có.
 *
 * Mỗi tệp có bản cuối `{khóa}` và bản dở `{khóa}.part`. Chỉ đổi tên `.part` thành bản cuối khi số byte khớp dung lượng máy
 * chủ báo, nên bản cuối luôn đủ (BN-03). Khóa gồm id và `cTag` (BN-02): tệp đổi nội dung thì khóa đổi và bản cũ bị dọn.
 * Vượt [maxBytes] (đọc lúc cần, người dùng đổi được ở Cài đặt) thì xóa tệp lâu không dùng nhất trước (BN-01). Ngắt kết nối xóa sạch thư mục (CD-05).
 *
 * Dùng `java.io.File` ở `androidMain` vì LRU cần `setLastModified` mà okio không có (để MVP2 tính tiếp).
 * Mỗi kho có thư mục và trần dung lượng riêng, nên cache ảnh và cache PDF không đẩy nhau ra.
 */
internal class ResumableFileStore(
    private val directory: File,
    private val maxBytes: () -> Long,
    private val dispatchers: DispatcherProvider,
    logTag: String,
) : ConnectionResetter {

    private val log = Logger.withTag(logTag)
    private val locks = Array(ResumableFileConstants.LOCK_STRIPES) { Mutex() }

    /** Tăng mỗi lần xóa sạch: tải đang bay thấy giá trị đổi thì bỏ ghi, không để tệp của tài khoản cũ nằm lại (CD-05). */
    @Volatile
    private var generation = 0

    /** Có sẵn thì phát `Ready` ngay; chưa thì tải bằng [downloader] kèm tiến trình và tải tiếp phần dở. Không ném lỗi, lỗi phát `Failed`. */
    fun open(ref: CachedFileRef, downloader: RangeDownloader): Flow<CachedFileState> = flow {
        val key = fileKey(ref)
        val final = File(directory, key)
        readyPath(final)?.let {
            emit(CachedFileState.Ready(it))
            return@flow
        }
        locks[(key.hashCode() and Int.MAX_VALUE) % ResumableFileConstants.LOCK_STRIPES].withLock {
            // Một người xem khác (trang kế tiếp tải trước) có thể vừa tải xong cùng tệp trong lúc chờ khóa.
            readyPath(final)?.let {
                emit(CachedFileState.Ready(it))
                return@flow
            }
            download(ref, key, final, downloader) { emit(it) }
        }
    }.flowOn(dispatchers.io)

    private suspend fun download(
        ref: CachedFileRef,
        key: String,
        final: File,
        downloader: RangeDownloader,
        emit: suspend (CachedFileState) -> Unit,
    ) {
        val startGeneration = generation
        if (!directory.isDirectory && !directory.mkdirs()) return emit(CachedFileState.Failed(AppError.Unknown()))
        val part = File(directory, key + ResumableFileConstants.PART_SUFFIX)

        // Thử tải tiếp từ phần dở; nếu máy chủ từ chối Range (416: phần dở dài hơn tệp, tệp đổi) thì xóa và tải lại từ đầu một lần.
        var restarted = false
        while (true) {
            var received = part.length()
            var total: Long? = null
            var stream: FileOutputStream? = null
            var lastEmitted = received
            val result = try {
                downloader(
                    received,
                    onStart = { resumed, totalBytes ->
                        total = totalBytes
                        // Máy chủ trả 200 (bỏ qua Range) thì ghi đè từ đầu, tránh nối nội dung vào phần dở.
                        if (!resumed) received = 0
                        stream = FileOutputStream(part, resumed)
                        emit(CachedFileState.Downloading(received, total))
                    },
                    onBytes = { buffer, length ->
                        stream?.write(buffer, 0, length)
                        received += length
                        if (received - lastEmitted >= ResumableFileConstants.PROGRESS_STEP_BYTES) {
                            lastEmitted = received
                            emit(CachedFileState.Downloading(received, total))
                        }
                    },
                )
            } finally {
                // Kể cả khi người xem rời tệp (hủy): đóng tệp và giữ phần dở để lần sau tải tiếp (BN-03).
                runCatching { stream?.close() }
            }

            when (result) {
                is AppResult.Success -> {
                    // Đứt giữa chừng mà không báo lỗi: số byte không khớp thì chưa phải tệp đủ, giữ phần dở.
                    val expected = total
                    if (expected != null && part.length() != expected) {
                        return emit(CachedFileState.Failed(AppError.Network))
                    }
                    if (generation != startGeneration) {
                        part.delete()
                        return emit(CachedFileState.Failed(AppError.AppLocked))
                    }
                    if (!part.renameTo(final)) {
                        part.delete()
                        return emit(CachedFileState.Failed(AppError.Unknown()))
                    }
                    pruneAfterWrite(ref, key)
                    return emit(CachedFileState.Ready(final.absolutePath))
                }
                is AppResult.Failure -> {
                    val error = result.error
                    val status = (error as? AppError.Http)?.status
                    if (status == 416 && !restarted) {
                        restarted = true
                        part.delete()
                        continue
                    }
                    // Tệp không còn (404/410) thì phần dở vô nghĩa; lỗi khác (mạng, token, 5xx) giữ để tải tiếp.
                    if (status == 404 || status == 410) part.delete()
                    return emit(CachedFileState.Failed(error))
                }
            }
        }
    }

    /** Tệp cuối hợp lệ thì đánh dấu vừa dùng (cho LRU) và trả đường dẫn. */
    private fun readyPath(final: File): String? {
        if (!final.isFile) return null
        runCatching { final.setLastModified(System.currentTimeMillis()) }
        return final.absolutePath
    }

    /** Dọn bản cũ của cùng tệp (cTag khác, BN-02) rồi giữ tổng dung lượng dưới [maxBytes] (BN-01). Không ném ngoại lệ. */
    private fun pruneAfterWrite(ref: CachedFileRef, keepKey: String) {
        runCatching {
            val prefix = itemPrefix(ref.itemId)
            fun isKept(file: File) = file.name == keepKey || file.name == keepKey + ResumableFileConstants.PART_SUFFIX
            directory.listFiles().orEmpty()
                .filter { it.name.startsWith(prefix) && !isKept(it) }
                .forEach { it.delete() }
            evictOldest(maxBytes()) { isKept(it) }
        }.onFailure { log.w(it) { "Không dọn được cache tệp" } }
    }

    /** Dung lượng các tệp đang giữ (kể cả tệp dở `.part`). 0 nếu chưa có thư mục. */
    suspend fun usedBytes(): Long = withContext(dispatchers.io) {
        try {
            directory.listFiles().orEmpty().sumOf { it.length() }
        } catch (e: Exception) {
            log.w(e) { "Không đo được cache tệp" }
            0L
        }
    }

    /** Dọn tệp lâu không dùng nhất trước cho tới khi không quá [limit] (CD-07). Không ném ngoại lệ. */
    suspend fun trimTo(limit: Long) {
        withContext(dispatchers.io) {
            runCatching { evictOldest(limit) { false } }.onFailure { log.w(it) { "Không dọn được cache tệp" } }
        }
    }

    /** Xóa tệp cũ nhất (theo `lastModified`) tới khi tổng không quá [limit]; bỏ qua tệp [keep] ghi. */
    private fun evictOldest(limit: Long, keep: (File) -> Boolean) {
        val remaining = directory.listFiles().orEmpty()
        var total = remaining.sumOf { it.length() }
        if (total <= limit) return
        for (file in remaining.sortedBy { it.lastModified() }) {
            if (total <= limit) break
            if (keep(file)) continue
            total -= file.length()
            file.delete()
        }
    }

    override suspend fun reset() {
        generation++
        try {
            withContext(dispatchers.io) { directory.deleteRecursively() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "Không xóa được cache tệp" }
        }
    }

    /** `{id}_{băm cTag}`: id Graph chỉ gồm chữ, số nên giữ nguyên để nhận ra bản cũ theo tiền tố; ký tự lạ đổi thành `_`. */
    private fun fileKey(ref: CachedFileRef): String =
        itemPrefix(ref.itemId) + (ref.cTag?.hashCode()?.toUInt()?.toString(16) ?: "0")

    private fun itemPrefix(itemId: String): String = ResumableFileConstants.NON_ALPHANUMERIC.replace(itemId, "_") + "_"
}
