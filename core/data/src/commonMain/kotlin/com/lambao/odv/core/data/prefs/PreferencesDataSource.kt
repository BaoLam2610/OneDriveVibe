package com.lambao.odv.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path

/**
 * Lớp bọc dùng chung cho mọi kho tùy chọn dựa trên DataStore Preferences (kế hoạch MVP1 mục 3): đọc an toàn và ghi an toàn.
 * Giá trị lạ hoặc đọc lỗi thì dùng mặc định (tùy chọn hiển thị hỏng không được làm hỏng màn hình); ghi lỗi bị bỏ qua (mất
 * lựa chọn lần này là đủ, giữ lựa chọn trong phiên). Mỗi tệp DataStore chỉ được một instance, nên mỗi kho một [PreferencesDataSource].
 */
internal class PreferencesDataSource(
    private val store: DataStore<Preferences>,
) {

    private val data: Flow<Preferences> = store.data.catch { emit(emptyPreferences()) }

    /** Phát lại [transform] của tùy chọn mỗi khi đổi; bỏ các lần phát trùng giá trị. */
    fun <T> observe(transform: (Preferences) -> T): Flow<T> = data.map(transform).distinctUntilChanged()

    /** Một tùy chọn kiểu enum lưu bằng tên; tên lạ hoặc thiếu thì [default]. */
    fun <E : Enum<E>> observeEnum(key: Preferences.Key<String>, default: E, entries: List<E>): Flow<E> =
        observe { it.getEnum(key, default, entries) }

    /** Toàn bộ khóa và giá trị hiện có dạng chữ, sắp theo khóa. Chỉ để màn Debug xem. */
    suspend fun snapshot(): Map<String, String> =
        data.first().asMap().entries.sortedBy { it.key.name }.associate { it.key.name to it.value.toString() }

    /** Xóa mọi tùy chọn của tệp này (ngắt kết nối, CD-05). Tệp còn lại nhưng rỗng. */
    suspend fun clear() = edit { it.clear() }

    suspend fun edit(block: (MutablePreferences) -> Unit) {
        try {
            store.edit(block)
        } catch (e: IOException) {
            // Chỉ là tùy chọn hiển thị: không ghi được thì giữ lựa chọn trong phiên, không báo lỗi.
            // Đặt tên tham số (không dùng `_`) để không phụ thuộc vào hỗ trợ tham số catch không tên của trình biên dịch.
        }
    }
}

/** Đọc một enum lưu bằng tên; tên lạ hoặc thiếu thì [default]. */
internal fun <E : Enum<E>> Preferences.getEnum(key: Preferences.Key<String>, default: E, entries: List<E>): E =
    entries.firstOrNull { it.name == this[key] } ?: default

/**
 * Tạo DataStore tại [path]. Tệp hỏng thì bắt đầu lại từ mặc định (đây chỉ là tùy chọn hiển thị). Đường dẫn do nền tảng đưa vào
 * để **giữ nguyên tên tệp** đã có trên máy người dùng (đổi tên là mất lựa chọn đã lưu).
 */
internal fun createPreferencesDataStore(path: Path, dispatchers: DispatcherProvider): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(SupervisorJob() + dispatchers.io),
        produceFile = { path },
    )
