package com.lambao.odv.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lambao.odv.core.domain.model.SortDirection
import com.lambao.odv.core.domain.model.SortField
import com.lambao.odv.core.domain.model.SortOrder
import com.lambao.odv.core.domain.model.ViewMode
import com.lambao.odv.core.domain.repository.BrowserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.enums.enumEntries

private val SORT_FIELD = stringPreferencesKey("sort_field")
private val SORT_DIRECTION = stringPreferencesKey("sort_direction")
private val VIEW_MODE = stringPreferencesKey("view_mode")

/**
 * [BrowserPreferences] trên DataStore Preferences (kế hoạch MVP1 mục 3). Giá trị lạ hoặc đọc lỗi thì dùng mặc định:
 * tùy chọn hiển thị hỏng không được làm hỏng màn hình. Ghi lỗi bị bỏ qua (mất lựa chọn lần này là đủ).
 */
internal class DataStoreBrowserPreferences(
    private val store: DataStore<Preferences>,
) : BrowserPreferences {

    private val data: Flow<Preferences> = store.data.catch { emit(emptyPreferences()) }

    override val sortOrder: Flow<SortOrder> = data
        .map { prefs ->
            SortOrder(
                field = prefs[SORT_FIELD].toEnum(SortField.Name),
                direction = prefs[SORT_DIRECTION].toEnum(SortDirection.Ascending),
            )
        }
        .distinctUntilChanged()

    override val viewMode: Flow<ViewMode> = data
        .map { prefs -> prefs[VIEW_MODE].toEnum(ViewMode.List) }
        .distinctUntilChanged()

    override suspend fun setSortOrder(order: SortOrder) {
        write {
            it[SORT_FIELD] = order.field.name
            it[SORT_DIRECTION] = order.direction.name
        }
    }

    override suspend fun setViewMode(mode: ViewMode) {
        write { it[VIEW_MODE] = mode.name }
    }

    private suspend fun write(block: (MutablePreferences) -> Unit) {
        try {
            store.edit(block)
        } catch (e: IOException) {
            // Chỉ là tùy chọn hiển thị: không ghi được thì giữ lựa chọn trong phiên, không báo lỗi.
            // Đặt tên tham số (không dùng `_`) để không phụ thuộc vào hỗ trợ tham số catch không tên của trình biên dịch.
        }
    }
}

private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    enumEntries<E>().firstOrNull { it.name == this } ?: default
