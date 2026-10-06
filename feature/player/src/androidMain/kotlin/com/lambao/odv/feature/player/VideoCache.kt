@file:OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.domain.hook.ConnectionResetter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Khóa cache của một video: id và `cTag` (BN-02), **không** phải URL. Link phát chứa `tempauth` đổi mỗi giờ, nếu lấy nó làm khóa
 * thì mỗi lần xem là một lần miss và phần đã xem (VD-11) không bao giờ dùng lại. Đổi tên hay di chuyển tệp giữ nguyên id nên
 * vẫn dùng lại cache; đổi nội dung thì `cTag` đổi và khóa mới tách khỏi bản cũ.
 */
internal fun videoCacheKey(itemId: String, cTag: String?): String = "$itemId:${cTag ?: "0"}"

/**
 * Cache các đoạn video đã xem (BN-01, VD-11) bằng `SimpleCache` của Media3: tự giữ từng đoạn theo khoảng byte nên xem lại hoặc
 * tải tiếp phần dở không phải tải lại (BN-03), và tự xóa đoạn lâu không dùng nhất khi vượt [PlayerConstants.CACHE_MAX_BYTES].
 * Nằm ở `cacheDir` nên không được sao lưu (CH-04) và hệ thống có thể dọn khi thiếu chỗ; mất thì chỉ phải tải lại.
 *
 * `SimpleCache` phải có đúng một bản cho mỗi thư mục trong cả tiến trình, nên lớp này là singleton Koin và mở lười (việc quét
 * thư mục tốn đĩa nên chạy ngoài luồng chính). Cũng là [ConnectionResetter] để ngắt kết nối xóa sạch (CD-05).
 */
internal class VideoCache(
    private val context: Context,
    private val dispatchers: DispatcherProvider,
) : ConnectionResetter {

    private val mutex = Mutex()
    private var cache: SimpleCache? = null

    suspend fun get(): Cache = mutex.withLock {
        cache ?: withContext(dispatchers.io) {
            val startedAt = System.currentTimeMillis()
            try {
                SimpleCache(
                    File(context.cacheDir, PlayerConstants.CACHE_DIR_NAME),
                    LeastRecentlyUsedCacheEvictor(PlayerConstants.CACHE_MAX_BYTES),
                    StandaloneDatabaseProvider(context),
                ).also {
                    playerLog.i { "[Cache] mở xong sau ${System.currentTimeMillis() - startedAt}ms, đang giữ ${it.cacheSpace} byte, ${it.keys.size} video" }
                }
            } catch (e: Exception) {
                // Chỉ ghi tên lớp: message có thể chứa đường dẫn. Để ngoại lệ đi tiếp cho giao diện báo lỗi.
                playerLog.e { "[Cache] mở thất bại: ${e.javaClass.simpleName}" }
                throw e
            }
        }.also { cache = it }
    }

    /** Bỏ các bản cũ (`cTag` khác) của cùng video để chúng không chiếm chỗ tới khi bị LRU dọn (BN-02). Không ném ngoại lệ. */
    suspend fun dropStaleVersions(itemId: String, keepKey: String) {
        withContext(dispatchers.io) {
            runCatching {
                val store = get()
                val prefix = "$itemId:"
                val stale = store.keys.filter { it.startsWith(prefix) && it != keepKey }
                stale.forEach { store.removeResource(it) }
                if (stale.isNotEmpty()) playerLog.i { "[Cache] id=${itemId.shortId()} bỏ ${stale.size} bản cũ (cTag đổi)" }
            }.onFailure { playerLog.w { "[Cache] không dọn được bản cũ: ${it.javaClass.simpleName}" } }
        }
    }

    override suspend fun reset() {
        withContext(dispatchers.io) {
            runCatching {
                val store = get()
                val keys = store.keys.toList()
                keys.forEach { store.removeResource(it) }
                playerLog.i { "[Cache] đã xóa sạch ${keys.size} video (ngắt kết nối)" }
            }.onFailure { playerLog.w { "[Cache] không xóa sạch được: ${it.javaClass.simpleName}" } }
        }
    }
}
