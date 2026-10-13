package com.lambao.odv.core.data.thumbnail

import co.touchlab.kermit.Logger
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.File

/**
 * [ThumbnailKeyIndex] bằng tệp văn bản trong `cacheDir` (không sao lưu, CH-04; hệ thống có thể dọn, mất thì chỉ mất khả năng dọn thumbnail của
 * tài khoản bị xóa): mỗi tài khoản một tệp, mỗi dòng một khóa, chỉ nối thêm. Khóa trùng giữa các phiên làm tệp dài hơn một chút nhưng gỡ vẫn đúng.
 * Tên tệp là drive id đã đổi ký tự lạ thành `_`, không lộ email (ADR-0030).
 */
internal class FileThumbnailKeyIndex(
    private val directory: File,
    private val dispatchers: DispatcherProvider,
) : ThumbnailKeyIndex {

    private val log = Logger.withTag("ThumbnailIndex")
    private val lock = Any()

    // Khóa đã ghi trong phiên này, để không nối trùng mỗi lần một ô thumbnail được tải lại.
    private val recorded = HashSet<String>()

    override fun record(accountId: String, key: String) {
        try {
            synchronized(lock) {
                if (!recorded.add(key)) return
                directory.mkdirs()
                fileOf(accountId).appendText(key + "\n")
            }
        } catch (e: Exception) {
            // Ghi chỉ mục lỗi chỉ làm mất khả năng dọn; thumbnail vẫn hiện bình thường.
            log.w { "Không ghi được chỉ mục thumbnail: ${e::class.simpleName}" }
        }
    }

    override suspend fun keysOf(accountId: String): List<String> = withContext(dispatchers.io) {
        try {
            synchronized(lock) { fileOf(accountId).takeIf { it.isFile }?.readLines().orEmpty() }.filter { it.isNotEmpty() }.distinct()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w { "Không đọc được chỉ mục thumbnail: ${e::class.simpleName}" }
            emptyList()
        }
    }

    override suspend fun forget(accountId: String) {
        withContext(dispatchers.io) {
            synchronized(lock) {
                // Các khóa của tài khoản này có thể được ghi lại sau khi xóa (tải đang bay): cho phép ghi lại.
                recorded.removeAll { it.startsWith(THUMB_PREFIX + accountId + "|") }
                fileOf(accountId).delete()
            }
        }
    }

    override suspend fun clear() {
        withContext(dispatchers.io) {
            synchronized(lock) {
                recorded.clear()
                directory.deleteRecursively()
            }
        }
    }

    private fun fileOf(accountId: String) = File(directory, NON_ALPHANUMERIC.replace(accountId, "_") + ".txt")

    private companion object {
        val NON_ALPHANUMERIC = Regex("[^A-Za-z0-9]")

        // Khóa thumbnail là "thumb:<id mờ>:..." và id mờ bắt đầu bằng "<drive id>|" (xem thumbnailCacheKey).
        const val THUMB_PREFIX = "thumb:"
    }
}
