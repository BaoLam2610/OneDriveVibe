package com.lambao.odv.core.data

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.preferencesDataStoreFile
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.data.original.FileOriginalImageRepository
import com.lambao.odv.core.data.prefs.DataStoreBrowserPreferences
import com.lambao.odv.core.data.prefs.DataStorePlayerPreferences
import com.lambao.odv.core.data.prefs.DataStorePreferencesInspector
import com.lambao.odv.core.data.prefs.PreferencesDataSource
import com.lambao.odv.core.data.prefs.createPreferencesDataStore
import com.lambao.odv.core.data.thumbnail.FixedThumbnailQuality
import com.lambao.odv.core.data.thumbnail.GraphThumbnailFetcherFactory
import com.lambao.odv.core.data.thumbnail.ThumbnailCacheResetter
import com.lambao.odv.core.data.thumbnail.ThumbnailKeyer
import com.lambao.odv.core.domain.settings.BrowserPreferences
import com.lambao.odv.core.domain.hook.ConnectionResetter
import com.lambao.odv.core.domain.repository.OriginalImageRepository
import com.lambao.odv.core.domain.settings.PlayerPreferences
import com.lambao.odv.core.domain.hook.PreferencesInspector
import com.lambao.odv.core.domain.hook.ThumbnailCache
import com.lambao.odv.core.domain.hook.ThumbnailQuality
import com.lambao.odv.core.network.graph.GraphApi
import okio.Path.Companion.toOkioPath
import org.koin.dsl.binds
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module

/**
 * Binding Koin của `:core:data` cần Context (bản Android): DataStore lưu tùy chọn hiển thị. Ghép ở MainApplication cùng
 * [dataModule]. Tệp `browser.preferences_pb` nằm trong bộ nhớ riêng của app, không sao lưu (CH-04).
 */
val androidDataModule = module {
    // Mỗi nguồn là một single có tên để cả kho tùy chọn và PreferencesInspector (màn Debug) dùng chung đúng một instance.
    single(named(StorageNames.BROWSER_PREFERENCES)) { preferencesSource(StorageNames.BROWSER_PREFERENCES) }
    single(named(StorageNames.PLAYER_PREFERENCES)) { preferencesSource(StorageNames.PLAYER_PREFERENCES) }
    single<BrowserPreferences> { DataStoreBrowserPreferences(get(named(StorageNames.BROWSER_PREFERENCES))) }
    // Chế độ phát và khung hình của màn xem video (Lát 6b, VD-06, VD-20): tệp DataStore riêng (mỗi tệp chỉ được một instance).
    single<PlayerPreferences> { DataStorePlayerPreferences(get(named(StorageNames.PLAYER_PREFERENCES))) }
    // Chỉ màn Debug gọi (DebugHooks.dumpPreferences); bản release có binding nhưng không ai dùng.
    single<PreferencesInspector> {
        DataStorePreferencesInspector(
            mapOf(
                StorageNames.BROWSER_PREFERENCES to get(named(StorageNames.BROWSER_PREFERENCES)),
                StorageNames.PLAYER_PREFERENCES to get(named(StorageNames.PLAYER_PREFERENCES)),
            ),
        )
    }
    // Trình tải thumbnail (Lát 4). Cache đĩa nằm ở cacheDir nên không được sao lưu (CH-04) và hệ thống có thể dọn khi
    // thiếu chỗ; mất thì chỉ phải tải lại.
    // Tỉ lệ chất lượng mặc định 100%; bản debug ghi đè bằng giá trị chỉnh ở màn Debug (DebugTools, module Koin đứng sau).
    single<ThumbnailQuality> { FixedThumbnailQuality }
    single<ImageLoader> { createImageLoader(androidContext(), get(), get()) }
    // Xóa cache thumbnail khi ngắt kết nối: DisconnectUseCase gom mọi ConnectionResetter bằng getAll(). Cũng là ThumbnailCache
    // để màn Debug xóa tay.
    singleOf(::ThumbnailCacheResetter) binds arrayOf(ConnectionResetter::class, ThumbnailCache::class)
    // Ảnh gốc (Lát 5): kho tệp riêng, tách khỏi cache thumbnail vì thumbnail cố ý chất lượng thấp; cũng là ConnectionResetter
    // để ngắt kết nối xóa sạch (CD-05).
    single {
        FileOriginalImageRepository(
            api = get(),
            dispatchers = get(),
            directory = androidContext().cacheDir.resolve(StorageNames.ORIGINALS_DIR),
            maxBytes = OriginalImageConstants.CACHE_MAX_BYTES,
        )
    } binds arrayOf(OriginalImageRepository::class, ConnectionResetter::class)
}

/**
 * Một nguồn tùy chọn DataStore tên [name]. **Giữ nguyên đường dẫn tệp** (`preferencesDataStoreFile`) để lựa chọn đã lưu trên
 * máy người dùng không mất khi cập nhật app (mỗi tệp chỉ được một instance).
 */
private fun Scope.preferencesSource(name: String): PreferencesDataSource {
    val file = androidContext().preferencesDataStoreFile(name)
    return PreferencesDataSource(createPreferencesDataStore(file.toOkioPath(), get<DispatcherProvider>()))
}

private fun createImageLoader(
    context: Context,
    api: GraphApi,
    quality: ThumbnailQuality,
): ImageLoader =
    ImageLoader.Builder(context)
        .components {
            // GIF động (AN-06): ImageDecoder từ Android 9, bản thấp hơn dùng decoder tự viết của coil-gif.
            if (Build.VERSION.SDK_INT >= 28) add(AnimatedImageDecoder.Factory()) else add(GifDecoder.Factory())
            add(ThumbnailKeyer(quality))
            add(GraphThumbnailFetcherFactory(api, quality))
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve(StorageNames.THUMBNAILS_DIR).toOkioPath())
                .maxSizeBytes(ThumbnailConstants.CACHE_MAX_BYTES)
                .build()
        }
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, ThumbnailConstants.MEMORY_PERCENT).build() }
        .crossfade(true)
        .build()
