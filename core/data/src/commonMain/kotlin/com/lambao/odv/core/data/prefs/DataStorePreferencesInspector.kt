package com.lambao.odv.core.data.prefs

import com.lambao.odv.core.domain.hook.PreferencesInspector

/**
 * [PreferencesInspector] đọc qua đúng các [PreferencesDataSource] app đang dùng (mỗi tệp DataStore chỉ được một instance, nên
 * không mở thêm instance thứ hai). Chỉ phục vụ màn Debug.
 */
internal class DataStorePreferencesInspector(
    private val sources: Map<String, PreferencesDataSource>,
) : PreferencesInspector {
    override suspend fun dump(): Map<String, Map<String, String>> = sources.mapValues { (_, source) -> source.snapshot() }
}
