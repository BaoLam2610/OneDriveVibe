@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.SimpleCache
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.domain.hook.CacheBudgetProvider
import com.lambao.odv.core.domain.hook.CacheStore
import com.lambao.odv.core.domain.hook.ConnectionResetter
import com.lambao.odv.core.domain.model.CacheKind
import kotlinx.coroutines.CancellationException
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
 * tải tiếp phần dở không phải tải lại (BN-03), và tự xóa đoạn lâu không dùng nhất khi vượt trần (do người dùng đặt ở Cài đặt, đọc lúc cần qua [CacheBudgetProvider]).
 * Nằm ở `cacheDir` nên không được sao lưu (CH-04) và hệ thống có thể dọn khi thiếu chỗ; mất thì chỉ phải tải lại.
 *
 * `SimpleCache` phải có đúng một bản cho mỗi thư mục trong cả tiến trình, nên lớp này là singleton Koin và mở lười (việc quét
 * thư mục tốn đĩa nên chạy ngoài luồng chính). Cũng là [ConnectionResetter] để ngắt kết nối xóa sạch (CD-05) và [CacheStore] loại video
 * để Cài đặt đo, xóa và dọn theo giới hạn (CD-07).
 */
internal class VideoCache(
    private val context: Context,
    private val dispatchers: DispatcherProvider,
    private val budget: CacheBudgetProvider,
) : ConnectionResetter, CacheStore {

    override val kind: CacheKind = CacheKind.Video

    private val mutex = Mutex()
    private var cache: SimpleCache? = null

    // Bộ dọn của cache đang mở; dựng cùng cache để mỗi lần mở lại bắt đầu với trạng thái đoạn sạch.
    private var evictor: BudgetCacheEvictor? = null

    // Giữ lại để reset() đóng được: nếu không thì `exoplayer_internal.db` vẫn mở và không xóa sạch được (CD-05).
    private var provider: StandaloneDatabaseProvider? = null

    suspend fun get(): Cache = mutex.withLock {
        cache ?: withContext(dispatchers.io) {
            val startedAt = System.currentTimeMillis()
            val db = StandaloneDatabaseProvider(context)
            try {
                val newEvictor = BudgetCacheEvictor { budget.bytesFor(CacheKind.Video) }
                SimpleCache(
                    File(context.cacheDir, PlayerConstants.CACHE_DIR_NAME),
                    newEvictor,
                    db,
                ).also {
                    provider = db
                    evictor = newEvictor
                    playerLog.i { "[Cache] mở xong sau ${System.currentTimeMillis() - startedAt}ms, đang giữ ${it.cacheSpace} byte, ${it.keys.size} video" }
                }
            } catch (e: Exception) {
                // Chỉ ghi tên lớp: message có thể chứa đường dẫn. Để ngoại lệ đi tiếp cho giao diện báo lỗi.
                playerLog.e { "[Cache] mở thất bại: ${e.javaClass.simpleName}" }
                runCatching { db.close() }
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

    /**
     * Xóa sạch (CD-05, KH-03, KH-06): không mở cache chỉ để xóa nó; đóng cache và DB, xóa thư mục video rồi xóa tệp
     * `exoplayer_internal.db` (kèm `-journal`/`-wal`/`-shm`, do `deleteDatabase`). Một bước lỗi vẫn chạy các bước còn lại;
     * log chỉ ghi tên loại ngoại lệ (CH-06). Lần `get()` sau tự mở cache mới.
     */
    override suspend fun reset() {
        withContext(dispatchers.io) {
            mutex.withLock {
                val videos = cache?.keys?.size
                step("đóng cache") { cache?.release() }
                cache = null
                evictor = null
                step("đóng DB") { provider?.close() }
                provider = null
                step("xóa thư mục video") {
                    val dir = File(context.cacheDir, PlayerConstants.CACHE_DIR_NAME)
                    if (dir.exists() && !dir.deleteRecursively()) error("deleteRecursively trả về false")
                }
                step("xóa DB") {
                    // false cũng là "không có tệp để xóa" nên không coi là lỗi.
                    context.deleteDatabase(StandaloneDatabaseProvider.DATABASE_NAME)
                }
                playerLog.i { "[Cache] đã xóa sạch (ngắt kết nối), trước đó ${videos ?: "chưa mở"} video" }
            }
        }
    }

    /** Dung lượng đang giữ. Chưa mở cache thì đo thư mục (tệp đoạn) thay vì mở chỉ để đếm, vì mở tốn đĩa. */
    override suspend fun usedBytes(): Long = withContext(dispatchers.io) {
        cache?.cacheSpace ?: runCatching { File(context.cacheDir, PlayerConstants.CACHE_DIR_NAME).walkTopDown().filter { it.isFile }.sumOf { it.length() } }
            .getOrDefault(0L)
    }

    override suspend fun clear() = reset()

    /** Dọn đoạn lâu không dùng nhất tới khi không quá [maxBytes] (CD-07). Cache chưa mở mà đã nằm trong trần thì khỏi mở. */
    override suspend fun trimTo(maxBytes: Long) {
        if (usedBytes() <= maxBytes) return
        try {
            val store = get()
            withContext(dispatchers.io) { synchronized(store) { evictor?.trimTo(store, maxBytes) } }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            playerLog.w { "[Cache] không dọn được theo giới hạn: ${e.javaClass.simpleName}" }
        }
    }

    private inline fun step(name: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            playerLog.w { "[Cache] không $name được: ${e.javaClass.simpleName}" }
        }
    }
}
