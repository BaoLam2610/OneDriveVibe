package com.lambao.odv.core.data.original

import co.touchlab.kermit.Logger
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.hook.ConnectionResetter
import com.lambao.odv.core.domain.model.OriginalImageRef
import com.lambao.odv.core.domain.repository.OriginalImageRepository
import com.lambao.odv.core.domain.model.OriginalImageState
import com.lambao.odv.core.network.graph.GraphApi
import com.lambao.odv.core.data.OriginalImageConstants
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
 * Kho ảnh gốc dựa trên tệp ở [directory] (nằm ở `cacheDir`: không sao lưu, CH-04; hệ thống có thể dọn khi thiếu chỗ,
 * mất thì tải lại). Tách biệt với `DiskCache` thumbnail của Coil vì thumbnail chất lượng thấp còn ảnh gốc cần nguyên bản,
 * và cần tải tiếp theo `Range` (BN-03) mà `DiskCache` không có.
 *
 * Mỗi ảnh có tệp cuối `{khóa}` và tệp dở `{khóa}.part`. Chỉ đổi tên `.part` thành tệp cuối khi số byte khớp dung lượng máy
 * chủ báo, nên tệp cuối luôn đủ (BN-03). Khóa gồm id và `cTag` (BN-02): ảnh đổi nội dung thì khóa đổi và bản cũ bị dọn.
 * Vượt [maxBytes] thì xóa tệp lâu không dùng nhất trước (BN-01). Ngắt kết nối xóa sạch thư mục (CD-05).
 */
internal class FileOriginalImageRepository(
    private val api: GraphApi,
    private val dispatchers: DispatcherProvider,
    private val directory: File,
    private val maxBytes: Long,
) : OriginalImageRepository, ConnectionResetter {

    private val log = Logger.withTag("OriginalImage")
    private val locks = Array(OriginalImageConstants.LOCK_STRIPES) { Mutex() }

    /** Tăng mỗi lần xóa sạch: tải đang bay thấy giá trị đổi thì bỏ ghi, không để ảnh của tài khoản cũ nằm lại (CD-05). */
    @Volatile
    private var generation = 0

    override fun open(ref: OriginalImageRef): Flow<OriginalImageState> = flow {
        val key = fileKey(ref)
        val final = File(directory, key)
        readyPath(final)?.let {
            emit(OriginalImageState.Ready(it))
            return@flow
        }
        locks[(key.hashCode() and Int.MAX_VALUE) % OriginalImageConstants.LOCK_STRIPES].withLock {
            // Một người xem khác (trang kế tiếp tải trước) có thể vừa tải xong cùng ảnh trong lúc chờ khóa.
            readyPath(final)?.let {
                emit(OriginalImageState.Ready(it))
                return@flow
            }
            download(ref, key, final) { emit(it) }
        }
    }.flowOn(dispatchers.io)

    private suspend fun download(
        ref: OriginalImageRef,
        key: String,
        final: File,
        emit: suspend (OriginalImageState) -> Unit,
    ) {
        val startGeneration = generation
        if (!directory.isDirectory && !directory.mkdirs()) return emit(OriginalImageState.Failed(AppError.Unknown()))
        val part = File(directory, key + OriginalImageConstants.PART_SUFFIX)

        // Thử tải tiếp từ phần dở; nếu máy chủ từ chối Range (416: phần dở dài hơn tệp, tệp đổi) thì xóa và tải lại từ đầu một lần.
        var restarted = false
        while (true) {
            var received = part.length()
            var total: Long? = null
            var stream: FileOutputStream? = null
            var lastEmitted = received
            val result = try {
                api.downloadContent(
                    itemId = ref.itemId,
                    offset = received,
                    onStart = { resumed, totalBytes ->
                        total = totalBytes
                        // Máy chủ trả 200 (bỏ qua Range) thì ghi đè từ đầu, tránh nối nội dung vào phần dở.
                        if (!resumed) received = 0
                        stream = FileOutputStream(part, resumed)
                        emit(OriginalImageState.Downloading(received, total))
                    },
                    onBytes = { buffer, length ->
                        stream?.write(buffer, 0, length)
                        received += length
                        if (received - lastEmitted >= OriginalImageConstants.PROGRESS_STEP_BYTES) {
                            lastEmitted = received
                            emit(OriginalImageState.Downloading(received, total))
                        }
                    },
                )
            } finally {
                // Kể cả khi người xem rời ảnh (hủy): đóng tệp và giữ phần dở để lần sau tải tiếp (BN-03).
                runCatching { stream?.close() }
            }

            when (result) {
                is AppResult.Success -> {
                    // Đứt giữa chừng mà không báo lỗi: số byte không khớp thì chưa phải tệp đủ, giữ phần dở.
                    val expected = total
                    if (expected != null && part.length() != expected) {
                        return emit(OriginalImageState.Failed(AppError.Network))
                    }
                    if (generation != startGeneration) {
                        part.delete()
                        return emit(OriginalImageState.Failed(AppError.AppLocked))
                    }
                    if (!part.renameTo(final)) {
                        part.delete()
                        return emit(OriginalImageState.Failed(AppError.Unknown()))
                    }
                    pruneAfterWrite(ref, key)
                    return emit(OriginalImageState.Ready(final.absolutePath))
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
                    return emit(OriginalImageState.Failed(error))
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

    /** Dọn bản cũ của cùng ảnh (cTag khác, BN-02) rồi giữ tổng dung lượng dưới [maxBytes] (BN-01). Không ném ngoại lệ. */
    private fun pruneAfterWrite(ref: OriginalImageRef, keepKey: String) {
        runCatching {
            val prefix = itemPrefix(ref.itemId)
            fun isKept(file: File) = file.name == keepKey || file.name == keepKey + OriginalImageConstants.PART_SUFFIX
            directory.listFiles().orEmpty()
                .filter { it.name.startsWith(prefix) && !isKept(it) }
                .forEach { it.delete() }
            val remaining = directory.listFiles().orEmpty()
            var total = remaining.sumOf { it.length() }
            if (total > maxBytes) {
                for (file in remaining.sortedBy { it.lastModified() }) {
                    if (total <= maxBytes) break
                    if (isKept(file)) continue
                    total -= file.length()
                    file.delete()
                }
            }
        }.onFailure { log.w(it) { "Không dọn được cache ảnh gốc" } }
    }

    override suspend fun reset() {
        generation++
        try {
            withContext(dispatchers.io) { directory.deleteRecursively() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "Không xóa được cache ảnh gốc" }
        }
    }

    /** `{id}_{băm cTag}`: id Graph chỉ gồm chữ, số nên giữ nguyên để nhận ra bản cũ theo tiền tố; ký tự lạ đổi thành `_`. */
    private fun fileKey(ref: OriginalImageRef): String =
        itemPrefix(ref.itemId) + (ref.cTag?.hashCode()?.toUInt()?.toString(16) ?: "0")

    private fun itemPrefix(itemId: String): String = OriginalImageConstants.NON_ALPHANUMERIC.replace(itemId, "_") + "_"
}
