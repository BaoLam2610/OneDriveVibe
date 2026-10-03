package com.lambao.odv.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.lambao.odv.core.data.prefs.DataStoreBrowserPreferences
import com.lambao.odv.core.domain.repository.BrowserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Binding Koin của `:core:data` cần Context (bản Android): DataStore lưu tùy chọn hiển thị. Ghép ở MainApplication cùng
 * [dataModule]. Tệp `browser.preferences_pb` nằm trong bộ nhớ riêng của app, không sao lưu (CH-04).
 */
val androidDataModule = module {
    single<BrowserPreferences> { DataStoreBrowserPreferences(createBrowserDataStore(androidContext())) }
}

private fun createBrowserDataStore(context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        // Tệp hỏng thì bắt đầu lại từ mặc định; đây chỉ là tùy chọn hiển thị.
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { context.preferencesDataStoreFile("browser") },
    )
