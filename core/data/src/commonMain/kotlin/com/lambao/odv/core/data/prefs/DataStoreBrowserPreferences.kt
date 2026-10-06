package com.lambao.odv.core.data.prefs

import com.lambao.odv.core.data.PreferenceKeys
import com.lambao.odv.core.domain.model.SortDirection
import com.lambao.odv.core.domain.model.SortField
import com.lambao.odv.core.domain.model.SortOrder
import com.lambao.odv.core.domain.model.ViewMode
import com.lambao.odv.core.domain.settings.BrowserPreferences
import kotlinx.coroutines.flow.Flow

/** [BrowserPreferences] trên DataStore Preferences (kế hoạch MVP1 mục 3); đọc/ghi an toàn do [PreferencesDataSource] lo. */
internal class DataStoreBrowserPreferences(
    private val prefs: PreferencesDataSource,
) : BrowserPreferences {

    override val sortOrder: Flow<SortOrder> = prefs.observe { p ->
        SortOrder(
            field = p.getEnum(PreferenceKeys.SORT_FIELD, SortField.Name, SortField.entries),
            direction = p.getEnum(PreferenceKeys.SORT_DIRECTION, SortDirection.Ascending, SortDirection.entries),
        )
    }

    override val viewMode: Flow<ViewMode> = prefs.observeEnum(PreferenceKeys.VIEW_MODE, ViewMode.List, ViewMode.entries)

    override suspend fun setSortOrder(order: SortOrder) {
        prefs.edit {
            it[PreferenceKeys.SORT_FIELD] = order.field.name
            it[PreferenceKeys.SORT_DIRECTION] = order.direction.name
        }
    }

    override suspend fun setViewMode(mode: ViewMode) {
        prefs.edit { it[PreferenceKeys.VIEW_MODE] = mode.name }
    }
}
