package com.lambao.odv.core.data.prefs

import com.lambao.odv.core.data.PreferenceKeys
import com.lambao.odv.core.domain.model.AccountScope
import com.lambao.odv.core.domain.model.HomeListTab
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

    override val lastListTab: Flow<HomeListTab> =
        prefs.observeEnum(PreferenceKeys.LAST_LIST_TAB, HomeListTab.Folders, HomeListTab.entries)

    override val accountScope: Flow<AccountScope?> = prefs.observe { p -> p[PreferenceKeys.ACCOUNT_SCOPE]?.toAccountScope() }

    override suspend fun setAccountScope(scope: AccountScope) {
        prefs.edit { it[PreferenceKeys.ACCOUNT_SCOPE] = scope.toStored() }
    }

    override suspend fun setLastListTab(tab: HomeListTab) {
        prefs.edit { it[PreferenceKeys.LAST_LIST_TAB] = tab.name }
    }

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

private const val SCOPE_ALL = "all"
private const val SCOPE_ONE_PREFIX = "one:"

/** `all` hoặc `one:<drive id>`; chuỗi lạ thì null (coi như chưa chọn). Drive id không chứa dấu cách hay xuống dòng. */
private fun String.toAccountScope(): AccountScope? = when {
    this == SCOPE_ALL -> AccountScope.All
    startsWith(SCOPE_ONE_PREFIX) && length > SCOPE_ONE_PREFIX.length -> AccountScope.One(removePrefix(SCOPE_ONE_PREFIX))
    else -> null
}

private fun AccountScope.toStored(): String = when (this) {
    AccountScope.All -> SCOPE_ALL
    is AccountScope.One -> SCOPE_ONE_PREFIX + accountId
}
